package com.shestikpetr.groupmaster.web.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.shestikpetr.groupmaster.storage.StackRepository;
import com.shestikpetr.groupmaster.web.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

public class StackApiHandler implements HttpHandler {

    private final StackRepository stackRepo;

    public StackApiHandler(StackRepository stackRepo) {
        this.stackRepo = stackRepo;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (!method.equals("GET")) {
            sendResponse(exchange, 405, JsonHelper.error("Method not allowed"));
            return;
        }

        // GET /api/stacks/player/{uuid}
        if (path.matches("/api/stacks/player/.+")) {
            String uuid = path.split("/")[4];
            handleGetByPlayer(exchange, uuid);
            return;
        }

        // GET /api/stacks
        if (path.equals("/api/stacks")) {
            handleGetAll(exchange);
            return;
        }

        sendResponse(exchange, 404, JsonHelper.error("Not found"));
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        try {
            List<StackRepository.StackEntry> entries = stackRepo.findAll();
            sendResponse(exchange, 200, entriesToJson(entries));
        } catch (SQLException e) {
            sendResponse(exchange, 500, JsonHelper.error("Database error"));
        }
    }

    private void handleGetByPlayer(HttpExchange exchange, String playerUuid) throws IOException {
        try {
            List<StackRepository.StackEntry> entries = stackRepo.findByPlayer(playerUuid);
            sendResponse(exchange, 200, entriesToJson(entries));
        } catch (SQLException e) {
            sendResponse(exchange, 500, JsonHelper.error("Database error"));
        }
    }

    private JsonArray entriesToJson(List<StackRepository.StackEntry> entries) {
        JsonArray arr = new JsonArray();
        for (StackRepository.StackEntry e : entries) {
            JsonObject obj = new JsonObject();
            obj.addProperty("playerUuid", e.playerUuid());
            obj.addProperty("bonusId", e.bonusId());
            obj.addProperty("stacks", e.stacks());
            arr.add(obj);
        }
        return arr;
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
