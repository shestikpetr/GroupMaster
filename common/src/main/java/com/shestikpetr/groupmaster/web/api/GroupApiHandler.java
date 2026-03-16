package com.shestikpetr.groupmaster.web.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shestikpetr.groupmaster.group.GroupManager;
import com.shestikpetr.groupmaster.model.Group;
import com.shestikpetr.groupmaster.model.PlayerGroupData;
import com.shestikpetr.groupmaster.web.JsonHelper;
import com.shestikpetr.groupmaster.web.sse.SseManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class GroupApiHandler implements HttpHandler {

    private final GroupManager groupManager;
    private final SseManager sseManager;

    public GroupApiHandler(GroupManager groupManager, SseManager sseManager) {
        this.groupManager = groupManager;
        this.sseManager = sseManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // /api/groups
        if (path.equals("/api/groups")) {
            switch (method) {
                case "GET" -> handleGetAll(exchange);
                case "POST" -> handleCreate(exchange);
                default -> sendResponse(exchange, 405, JsonHelper.error("Method not allowed"));
            }
            return;
        }

        // /api/groups/{id}
        if (path.startsWith("/api/groups/")) {
            String[] parts = path.split("/");
            if (parts.length < 4) {
                sendResponse(exchange, 400, JsonHelper.error("Invalid path"));
                return;
            }
            String groupId = parts[3];

            // /api/groups/{id}/players
            if (parts.length == 5 && parts[4].equals("players")) {
                if (method.equals("GET")) {
                    handleGetGroupPlayers(exchange, groupId);
                } else {
                    sendResponse(exchange, 405, JsonHelper.error("Method not allowed"));
                }
                return;
            }

            // /api/groups/{id}/tree
            if (parts.length == 5 && parts[4].equals("tree")) {
                if (method.equals("GET")) {
                    handleGetTree(exchange, groupId);
                } else {
                    sendResponse(exchange, 405, JsonHelper.error("Method not allowed"));
                }
                return;
            }

            switch (method) {
                case "GET" -> handleGetOne(exchange, groupId);
                case "PUT" -> handleUpdate(exchange, groupId);
                case "DELETE" -> handleDelete(exchange, groupId);
                default -> sendResponse(exchange, 405, JsonHelper.error("Method not allowed"));
            }
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 200, JsonHelper.groupsToJson(groupManager.getAllGroups()));
    }

    private void handleGetOne(HttpExchange exchange, String id) throws IOException {
        Optional<Group> group = groupManager.getGroup(id);
        if (group.isPresent()) {
            sendResponse(exchange, 200, JsonHelper.groupToJson(group.get()));
        } else {
            sendResponse(exchange, 404, JsonHelper.error("Group not found: " + id));
        }
    }

    private void handleGetTree(HttpExchange exchange, String id) throws IOException {
        Optional<Group> group = groupManager.getGroup(id);
        if (group.isEmpty()) {
            sendResponse(exchange, 404, JsonHelper.error("Group not found: " + id));
            return;
        }
        List<Group> children = groupManager.getChildren(id);
        sendResponse(exchange, 200, JsonHelper.groupTreeToJson(group.get(), children));
    }

    private void handleGetGroupPlayers(HttpExchange exchange, String groupId) throws IOException {
        Optional<Group> group = groupManager.getGroup(groupId);
        if (group.isEmpty()) {
            sendResponse(exchange, 404, JsonHelper.error("Group not found: " + groupId));
            return;
        }
        List<PlayerGroupData> players = groupManager.getPlayersInGroup(groupId);
        sendResponse(exchange, 200, JsonHelper.playersToJson(players));
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            sendResponse(exchange, 400, JsonHelper.error("Invalid JSON"));
            return;
        }

        String id = getStringOrNull(json, "id");
        String displayName = getStringOrNull(json, "displayName");
        String parentId = getStringOrNull(json, "parentId");
        int priority = json.has("priority") ? json.get("priority").getAsInt() : 0;

        if (id == null || displayName == null) {
            sendResponse(exchange, 400, JsonHelper.error("'id' and 'displayName' are required"));
            return;
        }

        boolean created = groupManager.createGroup(id, displayName, parentId, priority);
        if (created) {
            Group group = groupManager.getGroup(id).orElseThrow();
            sseManager.broadcastGroupChange("created", JsonHelper.groupToJson(group));
            sendResponse(exchange, 201, JsonHelper.groupToJson(group));
        } else {
            sendResponse(exchange, 409, JsonHelper.error("Failed to create group (already exists or invalid parent)"));
        }
    }

    private void handleUpdate(HttpExchange exchange, String id) throws IOException {
        Optional<Group> existing = groupManager.getGroup(id);
        if (existing.isEmpty()) {
            sendResponse(exchange, 404, JsonHelper.error("Group not found: " + id));
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

        Group group = existing.get();
        String displayName = json.has("displayName") ? json.get("displayName").getAsString() : group.getDisplayName();
        String parentId = json.has("parentId") ? getStringOrNull(json, "parentId") : group.getParentId();
        int priority = json.has("priority") ? json.get("priority").getAsInt() : group.getPriority();

        boolean updated = groupManager.updateGroup(id, displayName, parentId, priority);
        if (updated) {
            Group updatedGroup = groupManager.getGroup(id).orElseThrow();
            sseManager.broadcastGroupChange("updated", JsonHelper.groupToJson(updatedGroup));
            sendResponse(exchange, 200, JsonHelper.groupToJson(updatedGroup));
        } else {
            sendResponse(exchange, 400, JsonHelper.error("Failed to update group (cycle or invalid parent)"));
        }
    }

    private void handleDelete(HttpExchange exchange, String id) throws IOException {
        Optional<Group> existing = groupManager.getGroup(id);
        if (existing.isEmpty()) {
            sendResponse(exchange, 404, JsonHelper.error("Group not found: " + id));
            return;
        }

        JsonObject deletedJson = JsonHelper.groupToJson(existing.get());
        boolean deleted = groupManager.deleteGroup(id);
        if (deleted) {
            sseManager.broadcastGroupChange("deleted", deletedJson);
            sendResponse(exchange, 200, JsonHelper.success("Group deleted: " + id));
        } else {
            sendResponse(exchange, 500, JsonHelper.error("Failed to delete group"));
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
