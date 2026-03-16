package com.shestikpetr.groupmaster.web.sse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shestikpetr.groupmaster.Constants;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SseManager {

    private static final Gson GSON = new Gson();

    private final List<HttpExchange> clients = new CopyOnWriteArrayList<>();

    public void addClient(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, 0);

        clients.add(exchange);
        Constants.LOG.info("SSE client connected, total: {}", clients.size());

        // Send initial ping
        sendToClient(exchange, "connected", "{}");
    }

    public void broadcast(String event, Object data) {
        String json = GSON.toJson(data);
        for (HttpExchange client : clients) {
            if (!sendToClient(client, event, json)) {
                clients.remove(client);
            }
        }
    }

    public void broadcastGroupChange(String action, Object group) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", action);
        payload.add("data", GSON.toJsonTree(group));
        broadcast("group", payload);
    }

    public void broadcastPlayerChange(String action, Object player) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", action);
        payload.add("data", GSON.toJsonTree(player));
        broadcast("player", payload);
    }

    private boolean sendToClient(HttpExchange exchange, String event, String data) {
        try {
            OutputStream os = exchange.getResponseBody();
            String message = "event: " + event + "\ndata: " + data + "\n\n";
            os.write(message.getBytes(StandardCharsets.UTF_8));
            os.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void shutdown() {
        for (HttpExchange client : clients) {
            try {
                client.getResponseBody().close();
            } catch (IOException ignored) {
            }
        }
        clients.clear();
    }

    public int getClientCount() {
        return clients.size();
    }
}
