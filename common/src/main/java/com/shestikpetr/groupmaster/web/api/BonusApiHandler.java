package com.shestikpetr.groupmaster.web.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shestikpetr.groupmaster.bonus.BonusRegistry;
import com.shestikpetr.groupmaster.bonus.BonusResolver;
import com.shestikpetr.groupmaster.bonus.action.ActionType;
import com.shestikpetr.groupmaster.bonus.condition.ConditionParser;
import com.shestikpetr.groupmaster.group.GroupManager;
import com.shestikpetr.groupmaster.model.Bonus;
import com.shestikpetr.groupmaster.storage.BonusRepository;
import com.shestikpetr.groupmaster.web.JsonHelper;
import com.shestikpetr.groupmaster.web.sse.SseManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class BonusApiHandler implements HttpHandler {

    private static final Set<String> TRIGGERS = Set.of(
            "on_join", "on_leave", "tick",
            "on_attack", "on_damaged", "on_kill", "on_death", "on_eat"
    );
    private static final Set<String> TARGETS = Set.of("self", "victim");

    private final GroupManager groupManager;
    private final BonusRegistry registry;
    private final BonusRepository bonusRepo;
    private final BonusResolver bonusResolver;
    private final SseManager sseManager;

    public BonusApiHandler(GroupManager groupManager, BonusRegistry registry,
                           BonusRepository bonusRepo, BonusResolver bonusResolver,
                           SseManager sseManager) {
        this.groupManager = groupManager;
        this.registry = registry;
        this.bonusRepo = bonusRepo;
        this.bonusResolver = bonusResolver;
        this.sseManager = sseManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // /api/bonuses/types — list action types
        if (path.equals("/api/bonuses/types")) {
            if (method.equals("GET")) {
                handleGetTypes(exchange);
            } else {
                sendResponse(exchange, 405, JsonHelper.error("Method not allowed"));
            }
            return;
        }

        // /api/bonuses/conditions — list condition types
        if (path.equals("/api/bonuses/conditions")) {
            if (method.equals("GET")) {
                handleGetConditionTypes(exchange);
            } else {
                sendResponse(exchange, 405, JsonHelper.error("Method not allowed"));
            }
            return;
        }

        // /api/bonuses/group/{groupId}/effective — effective bonuses with inheritance
        if (path.matches("/api/bonuses/group/[^/]+/effective")) {
            String groupId = path.split("/")[4];
            if (method.equals("GET")) {
                handleGetEffective(exchange, groupId);
            } else {
                sendResponse(exchange, 405, JsonHelper.error("Method not allowed"));
            }
            return;
        }

        // /api/bonuses/group/{groupId} — direct bonuses for group
        if (path.matches("/api/bonuses/group/[^/]+")) {
            String groupId = path.split("/")[4];
            switch (method) {
                case "GET" -> handleGetByGroup(exchange, groupId);
                case "POST" -> handleCreate(exchange, groupId);
                default -> sendResponse(exchange, 405, JsonHelper.error("Method not allowed"));
            }
            return;
        }

        // /api/bonuses/{id}
        if (path.matches("/api/bonuses/\\d+")) {
            int id = Integer.parseInt(path.split("/")[3]);
            if (method.equals("DELETE")) {
                handleDelete(exchange, id);
            } else {
                sendResponse(exchange, 405, JsonHelper.error("Method not allowed"));
            }
            return;
        }

        // /api/bonuses
        if (path.equals("/api/bonuses")) {
            if (method.equals("GET")) {
                handleGetAll(exchange);
            } else {
                sendResponse(exchange, 405, JsonHelper.error("Method not allowed"));
            }
            return;
        }

        sendResponse(exchange, 404, JsonHelper.error("Not found"));
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        try {
            List<Bonus> bonuses = bonusRepo.findAll();
            sendResponse(exchange, 200, JsonHelper.bonusesToJson(bonuses));
        } catch (SQLException e) {
            sendResponse(exchange, 500, JsonHelper.error("Database error"));
        }
    }

    private void handleGetByGroup(HttpExchange exchange, String groupId) throws IOException {
        if (groupManager.getGroup(groupId).isEmpty()) {
            sendResponse(exchange, 404, JsonHelper.error("Group not found"));
            return;
        }
        List<Bonus> bonuses = bonusResolver.getDirectBonuses(groupId);
        sendResponse(exchange, 200, JsonHelper.bonusesToJson(bonuses));
    }

    private void handleGetEffective(HttpExchange exchange, String groupId) throws IOException {
        if (groupManager.getGroup(groupId).isEmpty()) {
            sendResponse(exchange, 404, JsonHelper.error("Group not found"));
            return;
        }
        List<Bonus> bonuses = bonusResolver.resolve(groupId);
        sendResponse(exchange, 200, JsonHelper.bonusesToJson(bonuses));
    }

    private void handleCreate(HttpExchange exchange, String groupId) throws IOException {
        if (groupManager.getGroup(groupId).isEmpty()) {
            sendResponse(exchange, 404, JsonHelper.error("Group not found"));
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            sendResponse(exchange, 400, JsonHelper.error("Invalid JSON"));
            return;
        }

        String trigger = getStringOr(json, "trigger", "on_join");
        int tickInterval = json.has("tickInterval") ? json.get("tickInterval").getAsInt() : 20;
        String condition = json.has("condition") ? json.get("condition").toString() : "{}";
        String actionType = getStringOrNull(json, "actionType");
        String actionValue = null;
        if (json.has("actionValue")) {
            var av = json.get("actionValue");
            actionValue = av.isJsonPrimitive() ? av.getAsString() : av.toString();
        }
        boolean override = json.has("override") && json.get("override").getAsBoolean();

        if (actionType == null || actionType.isBlank() || actionValue == null) {
            sendResponse(exchange, 400, JsonHelper.error("'actionType' and 'actionValue' are required"));
            return;
        }

        if (actionValue.length() > 4096) {
            sendResponse(exchange, 400, JsonHelper.error("actionValue too long (max 4096 characters)"));
            return;
        }

        if (tickInterval < 1 || tickInterval > 72000) {
            sendResponse(exchange, 400, JsonHelper.error("tickInterval must be between 1 and 72000"));
            return;
        }

        String target = getStringOr(json, "target", "self");

        if (!TRIGGERS.contains(trigger)) {
            sendResponse(exchange, 400, JsonHelper.error("Invalid trigger. Use: " + String.join(", ", TRIGGERS)));
            return;
        }

        if (!TARGETS.contains(target)) {
            sendResponse(exchange, 400, JsonHelper.error("Invalid target. Use: self, victim"));
            return;
        }

        Optional<ActionType> action = registry.getAction(actionType);
        if (action.isEmpty()) {
            sendResponse(exchange, 400, JsonHelper.error("Unknown action type: " + actionType));
            return;
        }

        String error = action.get().validate(actionValue);
        if (error != null) {
            sendResponse(exchange, 400, JsonHelper.error("Invalid action value: " + error));
            return;
        }

        String condError = ConditionParser.validate(condition);
        if (condError != null) {
            sendResponse(exchange, 400, JsonHelper.error("Invalid condition: " + condError));
            return;
        }

        int maxStacks = json.has("maxStacks") ? json.get("maxStacks").getAsInt() : 0;
        if (maxStacks < 0 || maxStacks > 1000) {
            sendResponse(exchange, 400, JsonHelper.error("maxStacks must be between 0 and 1000"));
            return;
        }
        String stackMode = getStringOr(json, "stackMode", "each");
        String resetOn = getStringOr(json, "resetOn", "never");

        if (!Set.of("each", "threshold").contains(stackMode)) {
            sendResponse(exchange, 400, JsonHelper.error("Invalid stackMode. Use: each, threshold"));
            return;
        }
        if (!Set.of("never", "on_death", "on_leave").contains(resetOn)) {
            sendResponse(exchange, 400, JsonHelper.error("Invalid resetOn. Use: never, on_death, on_leave"));
            return;
        }

        String mergeKey = action.get().extractMergeKey(actionValue);
        Bonus bonus = new Bonus(groupId, trigger, tickInterval, condition, actionType, actionValue,
                mergeKey, override, target, maxStacks, stackMode, resetOn);

        try {
            bonusRepo.create(bonus);
            JsonObject bonusJson = JsonHelper.bonusToJson(bonus);
            sseManager.broadcastGroupChange("bonus_added", bonusJson);
            sendResponse(exchange, 201, bonusJson);
        } catch (SQLException e) {
            sendResponse(exchange, 500, JsonHelper.error("Database error"));
        }
    }

    private void handleDelete(HttpExchange exchange, int id) throws IOException {
        try {
            if (bonusRepo.delete(id)) {
                JsonObject msg = JsonHelper.success("Bonus deleted");
                msg.addProperty("id", id);
                sseManager.broadcastGroupChange("bonus_removed", msg);
                sendResponse(exchange, 200, msg);
            } else {
                sendResponse(exchange, 404, JsonHelper.error("Bonus not found"));
            }
        } catch (SQLException e) {
            sendResponse(exchange, 500, JsonHelper.error("Database error"));
        }
    }

    private void handleGetTypes(HttpExchange exchange) throws IOException {
        var arr = new com.google.gson.JsonArray();
        for (ActionType type : registry.getAllActions()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", type.getId());
            obj.addProperty("displayName", type.getDisplayName());
            arr.add(obj);
        }
        sendResponse(exchange, 200, arr);
    }

    private void handleGetConditionTypes(HttpExchange exchange) throws IOException {
        var arr = new com.google.gson.JsonArray();
        for (String type : List.of("always", "is_day", "is_night", "in_sunlight", "in_water",
                "in_rain", "on_fire", "sneaking", "health_below", "health_above",
                "in_dimension", "and", "or", "not")) {
            arr.add(type);
        }
        sendResponse(exchange, 200, arr);
    }

    private static String getStringOrNull(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) return null;
        return json.get(key).getAsString();
    }

    private static String getStringOr(JsonObject json, String key, String defaultValue) {
        String val = getStringOrNull(json, key);
        return val != null ? val : defaultValue;
    }

    private static void sendResponse(HttpExchange exchange, int status, Object body) throws IOException {
        String json = JsonHelper.toJson(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }
}
