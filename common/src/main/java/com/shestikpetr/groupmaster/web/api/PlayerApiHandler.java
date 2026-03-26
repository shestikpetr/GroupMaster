package com.shestikpetr.groupmaster.web.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shestikpetr.groupmaster.group.GroupManager;
import com.shestikpetr.groupmaster.model.PlayerGroupData;
import com.shestikpetr.groupmaster.web.JsonHelper;
import com.shestikpetr.groupmaster.web.sse.SseManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class PlayerApiHandler implements HttpHandler {

    private final GroupManager groupManager;
    private final SseManager sseManager;

    public PlayerApiHandler(GroupManager groupManager, SseManager sseManager) {
        this.groupManager = groupManager;
        this.sseManager = sseManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // /api/players
        if (path.equals("/api/players")) {
            switch (method) {
                case "GET" -> handleGetAll(exchange);
                case "POST" -> handleAssign(exchange);
                default -> sendResponse(exchange, 405, JsonHelper.error("Method not allowed"));
            }
            return;
        }

        // /api/players/{uuid}
        if (path.startsWith("/api/players/")) {
            String[] parts = path.split("/");
            if (parts.length < 4) {
                sendResponse(exchange, 400, JsonHelper.error("Invalid path"));
                return;
            }
            String uuidStr = parts[3];
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                sendResponse(exchange, 400, JsonHelper.error("Invalid UUID: " + uuidStr));
                return;
            }

            switch (method) {
                case "GET" -> handleGetOne(exchange, uuid);
                case "PUT" -> handleReassign(exchange, uuid);
                case "DELETE" -> handleRemove(exchange, uuid);
                default -> sendResponse(exchange, 405, JsonHelper.error("Method not allowed"));
            }
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 200, JsonHelper.playersToJson(groupManager.getAllPlayers()));
    }

    private void handleGetOne(HttpExchange exchange, UUID uuid) throws IOException {
        Optional<PlayerGroupData> player = groupManager.getPlayerGroup(uuid);
        if (player.isPresent()) {
            sendResponse(exchange, 200, JsonHelper.playerToJson(player.get()));
        } else {
            sendResponse(exchange, 404, JsonHelper.error("Player not found"));
        }
    }

    private void handleAssign(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            sendResponse(exchange, 400, JsonHelper.error("Invalid JSON"));
            return;
        }

        String uuidStr = getStringOrNull(json, "playerUuid");
        String playerName = getStringOrNull(json, "playerName");
        String groupId = getStringOrNull(json, "groupId");

        if (uuidStr == null || playerName == null || groupId == null) {
            sendResponse(exchange, 400, JsonHelper.error("'playerUuid', 'playerName', and 'groupId' are required"));
            return;
        }

        if (playerName.length() > 16) {
            sendResponse(exchange, 400, JsonHelper.error("Player name too long (max 16 characters)"));
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, JsonHelper.error("Invalid UUID"));
            return;
        }

        if (groupManager.getGroup(groupId).isEmpty()) {
            sendResponse(exchange, 404, JsonHelper.error("Group not found: " + groupId));
            return;
        }

        boolean assigned = groupManager.assignPlayer(uuid, playerName, groupId, "web");
        if (assigned) {
            PlayerGroupData data = groupManager.getPlayerGroup(uuid).orElseThrow();
            sseManager.broadcastPlayerChange("assigned", JsonHelper.playerToJson(data));
            sendResponse(exchange, 200, JsonHelper.playerToJson(data));
        } else {
            sendResponse(exchange, 400, JsonHelper.error("Failed to assign player (group not found)"));
        }
    }

    private void handleReassign(HttpExchange exchange, UUID uuid) throws IOException {
        Optional<PlayerGroupData> existing = groupManager.getPlayerGroup(uuid);
        if (existing.isEmpty()) {
            sendResponse(exchange, 404, JsonHelper.error("Player not found"));
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

        String groupId = getStringOrNull(json, "groupId");
        if (groupId == null) {
            sendResponse(exchange, 400, JsonHelper.error("'groupId' is required"));
            return;
        }

        if (groupManager.getGroup(groupId).isEmpty()) {
            sendResponse(exchange, 404, JsonHelper.error("Group not found: " + groupId));
            return;
        }

        boolean assigned = groupManager.assignPlayer(uuid, existing.get().getPlayerName(), groupId, "web");
        if (assigned) {
            PlayerGroupData data = groupManager.getPlayerGroup(uuid).orElseThrow();
            sseManager.broadcastPlayerChange("reassigned", JsonHelper.playerToJson(data));
            sendResponse(exchange, 200, JsonHelper.playerToJson(data));
        } else {
            sendResponse(exchange, 400, JsonHelper.error("Failed to reassign player"));
        }
    }

    private void handleRemove(HttpExchange exchange, UUID uuid) throws IOException {
        Optional<PlayerGroupData> existing = groupManager.getPlayerGroup(uuid);
        if (existing.isEmpty()) {
            sendResponse(exchange, 404, JsonHelper.error("Player not found"));
            return;
        }

        JsonObject removedJson = JsonHelper.playerToJson(existing.get());
        boolean removed = groupManager.removePlayer(uuid);
        if (removed) {
            sseManager.broadcastPlayerChange("removed", removedJson);
            sendResponse(exchange, 200, JsonHelper.success("Player removed"));
        } else {
            sendResponse(exchange, 500, JsonHelper.error("Failed to remove player"));
        }
    }

    private static String getStringOrNull(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        return json.get(key).getAsString();
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
