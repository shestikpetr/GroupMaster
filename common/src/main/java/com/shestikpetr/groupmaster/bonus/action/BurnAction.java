package com.shestikpetr.groupmaster.bonus.action;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sets the player on fire.
 * Value: {"duration":3}
 * duration in seconds
 */
public class BurnAction implements ActionType {

    @Override public String getId() { return "burn"; }
    @Override public String getDisplayName() { return "Burn"; }

    @Override
    public void apply(ServerPlayer player, String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        int duration = json.has("duration") ? json.get("duration").getAsInt() : 3;
        player.igniteForSeconds(duration);
    }

    @Override
    public void remove(ServerPlayer player, String value) {
        player.clearFire();
    }

    @Override
    public String validate(String value) {
        try {
            JsonParser.parseString(value).getAsJsonObject();
            return null;
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
    }

    @Override
    public String extractMergeKey(String value) {
        return "burn";
    }

    @Override
    public String describe(String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        int dur = json.has("duration") ? json.get("duration").getAsInt() : 3;
        return "burn for " + dur + "s";
    }
}
