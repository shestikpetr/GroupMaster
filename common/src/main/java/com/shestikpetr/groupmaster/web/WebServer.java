package com.shestikpetr.groupmaster.web;

import com.shestikpetr.groupmaster.Constants;
import com.shestikpetr.groupmaster.GroupMasterServer;
import com.shestikpetr.groupmaster.group.GroupManager;
import com.shestikpetr.groupmaster.web.api.BonusApiHandler;
import com.shestikpetr.groupmaster.web.api.GroupApiHandler;
import com.shestikpetr.groupmaster.web.api.PlayerApiHandler;
import com.shestikpetr.groupmaster.web.sse.SseManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class WebServer {

    private final WebConfig config;
    private final GroupManager groupManager;
    private final SseManager sseManager;
    private HttpServer server;

    public WebServer(WebConfig config, GroupManager groupManager) {
        this.config = config;
        this.groupManager = groupManager;
        this.sseManager = new SseManager();
    }

    public void start() {
        if (!config.isEnabled()) {
            Constants.LOG.info("Web server is disabled in config");
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(config.getPort()), 0);
            server.setExecutor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "GroupMaster-Web");
                t.setDaemon(true);
                return t;
            }));

            GroupApiHandler groupHandler = new GroupApiHandler(groupManager, sseManager);
            PlayerApiHandler playerHandler = new PlayerApiHandler(groupManager, sseManager);

            GroupMasterServer gms = GroupMasterServer.getInstance();
            BonusApiHandler bonusHandler = new BonusApiHandler(
                    groupManager, gms.getBonusRegistry(), gms.getBonusRepository(),
                    gms.getBonusResolver(), sseManager);

            server.createContext("/api/groups", exchange -> {
                if (handleCors(exchange)) return;
                if (!checkAuth(exchange)) return;
                groupHandler.handle(exchange);
            });

            server.createContext("/api/players", exchange -> {
                if (handleCors(exchange)) return;
                if (!checkAuth(exchange)) return;
                playerHandler.handle(exchange);
            });

            server.createContext("/api/bonuses", exchange -> {
                if (handleCors(exchange)) return;
                if (!checkAuth(exchange)) return;
                bonusHandler.handle(exchange);
            });

            server.createContext("/api/events", exchange -> {
                if (handleCors(exchange)) return;
                if (!checkAuth(exchange)) return;
                sseManager.addClient(exchange);
            });

            server.createContext("/", this::handleStatic);

            server.start();
            Constants.LOG.info("Web server started on port {}", config.getPort());
            Constants.LOG.info("Web panel token: {}", config.getToken());
        } catch (IOException e) {
            Constants.LOG.error("Failed to start web server", e);
        }
    }

    public void stop() {
        if (server != null) {
            sseManager.shutdown();
            server.stop(0);
            Constants.LOG.info("Web server stopped");
        }
    }

    public SseManager getSseManager() {
        return sseManager;
    }

    private boolean checkAuth(HttpExchange exchange) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.equals("Bearer " + config.getToken())) {
            // Also check query parameter for SSE (browsers can't set headers for EventSource)
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("token=" + config.getToken())) {
                return true;
            }
            byte[] resp = "{\"error\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.getResponseBody().close();
            return false;
        }
        return true;
    }

    private boolean handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.equals("/index.html")) {
            serveResource(exchange, "/web/index.html", "text/html");
        } else {
            byte[] resp = "Not found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.getResponseBody().close();
        }
    }

    private void serveResource(HttpExchange exchange, String resourcePath, String contentType) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                byte[] resp = "Resource not found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, resp.length);
                exchange.getResponseBody().write(resp);
                exchange.getResponseBody().close();
                return;
            }
            byte[] data = is.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.getResponseBody().close();
        }
    }
}
