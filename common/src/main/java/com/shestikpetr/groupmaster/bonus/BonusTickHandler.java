package com.shestikpetr.groupmaster.bonus;

import com.shestikpetr.groupmaster.group.GroupManager;
import com.shestikpetr.groupmaster.model.PlayerGroupData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Handles tick-based bonus processing.
 * Should be called every server tick from the platform-specific event handler.
 */
public class BonusTickHandler {

    private final GroupManager groupManager;
    private final BonusApplier bonusApplier;
    private int tickCounter = 0;

    public BonusTickHandler(GroupManager groupManager, BonusApplier bonusApplier) {
        this.groupManager = groupManager;
        this.bonusApplier = bonusApplier;
    }

    /**
     * Called every server tick. Processes tick-based bonuses for all online players.
     */
    public void onTick(MinecraftServer server) {
        tickCounter++;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Optional<PlayerGroupData> data = groupManager.getPlayerGroup(player.getUUID());
            data.ifPresent(pgd -> bonusApplier.processTick(player, pgd.getGroupId(), tickCounter));
        }
    }
}
