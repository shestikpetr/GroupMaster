package com.shestikpetr.groupmaster.bonus.action;

import net.minecraft.server.level.ServerPlayer;

/**
 * Defines how a bonus action is applied to and removed from a player.
 * Actions are data-driven: parameters come from a JSON string.
 */
public interface ActionType {

    String getId();

    String getDisplayName();

    /**
     * Apply the action to the player.
     */
    void apply(ServerPlayer player, String value);

    /**
     * Remove/undo the action from the player (if applicable).
     */
    void remove(ServerPlayer player, String value);

    /**
     * Validate the JSON value. Returns null if valid, error message otherwise.
     */
    String validate(String value);

    /**
     * Extract merge key from the value for override resolution.
     */
    String extractMergeKey(String value);

    /**
     * Human-readable description.
     */
    String describe(String value);
}
