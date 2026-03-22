package com.shestikpetr.groupmaster.bonus.action;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shestikpetr.groupmaster.GroupMasterServer;
import com.shestikpetr.groupmaster.model.PlayerGroupData;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Transfers the player to a different group.
 * Value: {"group":"paladin_lv2"}
 * Typically used with threshold stacking for progression systems.
 */
public class SetGroupAction implements ActionType {

    @Override public String getId() { return "set_group"; }
    @Override public String getDisplayName() { return "Set Group"; }

    @Override
    public void apply(ServerPlayer player, String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        String newGroupId = json.get("group").getAsString();

        GroupMasterServer gms = GroupMasterServer.getInstance();
        if (gms == null) return;

        if (gms.getGroupManager().getGroup(newGroupId).isEmpty()) return;

        // Defer group change to avoid re-entrancy during bonus processing
        player.getServer().execute(() -> {
            Optional<PlayerGroupData> current = gms.getGroupManager().getPlayerGroup(player.getUUID());
            String oldGroupId = current.map(PlayerGroupData::getGroupId).orElse(null);

            gms.getGroupManager().assignPlayer(player.getUUID(), player.getName().getString(), newGroupId, "bonus");
            gms.getBonusApplier().switchGroup(player, oldGroupId, newGroupId);
        });
    }

    @Override
    public void remove(ServerPlayer player, String value) {
        // Group changes are not reversible
    }

    @Override
    public String validate(String value) {
        try {
            JsonObject json = JsonParser.parseString(value).getAsJsonObject();
            if (!json.has("group")) return "Missing 'group' field";
            return null;
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
    }

    @Override
    public String extractMergeKey(String value) {
        return "set_group";
    }

    @Override
    public String describe(String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        return "Set group: " + json.get("group").getAsString();
    }
}
