package com.shestikpetr.groupmaster.bonus.action;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;

/**
 * Executes a server command targeting the player.
 * Value: {"command":"give {player} minecraft:diamond 1"}
 * {player} is replaced with the player's name.
 */
public class CommandAction implements ActionType {

    @Override public String getId() { return "command"; }
    @Override public String getDisplayName() { return "Server Command"; }

    @Override
    public void apply(ServerPlayer player, String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        String cmd = json.get("command").getAsString();
        cmd = cmd.replace("{player}", player.getName().getString());
        player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack().withSuppressedOutput(),
                cmd
        );
    }

    @Override
    public void remove(ServerPlayer player, String value) {
        // Commands are fire-and-forget, no undo
    }

    @Override
    public String validate(String value) {
        try {
            JsonObject json = JsonParser.parseString(value).getAsJsonObject();
            if (!json.has("command")) return "Missing 'command' field";
            return null;
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
    }

    @Override
    public String extractMergeKey(String value) {
        // Each command is unique by its content
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        return json.get("command").getAsString();
    }

    @Override
    public String describe(String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        String cmd = json.get("command").getAsString();
        return "/" + (cmd.length() > 40 ? cmd.substring(0, 40) + "..." : cmd);
    }
}
