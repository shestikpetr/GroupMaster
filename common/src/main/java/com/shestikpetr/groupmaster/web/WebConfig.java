package com.shestikpetr.groupmaster.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shestikpetr.groupmaster.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class WebConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private int port = 8080;
    private String token = "";
    private boolean enabled = true;

    public int getPort() {
        return port;
    }

    public String getToken() {
        return token;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static WebConfig load(Path serverDir) {
        Path configPath = serverDir.resolve("config").resolve("groupmaster").resolve("web.json");
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                WebConfig config = GSON.fromJson(json, WebConfig.class);
                if (config.token.isEmpty()) {
                    config.token = generateToken();
                    config.save(configPath);
                }
                return config;
            } catch (IOException e) {
                Constants.LOG.error("Failed to read web config", e);
            }
        }

        WebConfig config = new WebConfig();
        config.token = generateToken();
        config.save(configPath);
        return config;
    }

    private void save(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(this));
            Constants.LOG.info("Web config saved to {}", configPath);
        } catch (IOException e) {
            Constants.LOG.error("Failed to save web config", e);
        }
    }

    private static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
