package com.shestikpetr.groupmaster.bonus.action;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends a chat message to the player.
 * Value: {"message":"You feel stronger!"}
 */
public class MessageAction implements ActionType {

    @Override public String getId() { return "message"; }
    @Override public String getDisplayName() { return "Chat Message"; }

    @Override
    public void apply(ServerPlayer player, String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        String msg = json.get("message").getAsString();
        msg = msg.replace("{player}", player.getName().getString());
        player.sendSystemMessage(Component.literal(msg));
    }

    @Override
    public void remove(ServerPlayer player, String value) {
        // Messages can't be "removed"
    }

    @Override
    public String validate(String value) {
        try {
            JsonObject json = JsonParser.parseString(value).getAsJsonObject();
            if (!json.has("message")) return "Missing 'message' field";
            return null;
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
    }

    @Override
    public String extractMergeKey(String value) {
        return "msg";
    }

    @Override
    public String describe(String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        String msg = json.get("message").getAsString();
        return "\"" + (msg.length() > 30 ? msg.substring(0, 30) + "..." : msg) + "\"";
    }
}
