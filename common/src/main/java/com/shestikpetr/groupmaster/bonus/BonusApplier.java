package com.shestikpetr.groupmaster.bonus;

import com.shestikpetr.groupmaster.Constants;
import com.shestikpetr.groupmaster.bonus.action.ActionType;
import com.shestikpetr.groupmaster.bonus.condition.BonusCondition;
import com.shestikpetr.groupmaster.bonus.condition.ConditionParser;
import com.shestikpetr.groupmaster.model.Bonus;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

/**
 * Applies and removes bonuses from players.
 * Handles on_join, on_leave triggers, condition checking, and stacking.
 */
public class BonusApplier {

    private final BonusRegistry registry;
    private final BonusResolver resolver;
    private final StackManager stackManager;

    public BonusApplier(BonusRegistry registry, BonusResolver resolver, StackManager stackManager) {
        this.registry = registry;
        this.resolver = resolver;
        this.stackManager = stackManager;
    }

    /**
     * Apply all on_join bonuses for a group to a player.
     */
    public void applyJoinBonuses(ServerPlayer player, String groupId) {
        List<Bonus> bonuses = resolver.resolveByTrigger(groupId, "on_join");
        for (Bonus bonus : bonuses) {
            applyIfConditionMet(player, bonus);
        }
    }

    /**
     * Remove all on_join bonuses for a group from a player.
     */
    public void removeJoinBonuses(ServerPlayer player, String groupId) {
        List<Bonus> bonuses = resolver.resolveByTrigger(groupId, "on_join");
        for (Bonus bonus : bonuses) {
            removeBonus(player, bonus);
        }
    }

    /**
     * Apply all on_leave bonuses (fire-and-forget actions like messages).
     */
    public void applyLeaveBonuses(ServerPlayer player, String groupId) {
        List<Bonus> bonuses = resolver.resolveByTrigger(groupId, "on_leave");
        for (Bonus bonus : bonuses) {
            applyIfConditionMet(player, bonus);
        }
    }

    /**
     * Process tick bonuses for a player in a group.
     */
    public void processTick(ServerPlayer player, String groupId, int currentTick) {
        List<Bonus> bonuses = resolver.resolveByTrigger(groupId, "tick");
        for (Bonus bonus : bonuses) {
            int interval = bonus.getTickInterval() > 0 ? bonus.getTickInterval() : 20;
            if (currentTick % interval == 0) {
                applyIfConditionMet(player, bonus);
            }
        }
    }

    /**
     * Switch player from one group to another.
     */
    public void switchGroup(ServerPlayer player, String oldGroupId, String newGroupId) {
        if (oldGroupId != null) {
            applyLeaveBonuses(player, oldGroupId);
            removeJoinBonuses(player, oldGroupId);
            stackManager.resetOnLeave(player.getUUID().toString());
        }
        applyJoinBonuses(player, newGroupId);
    }

    private void applyIfConditionMet(ServerPlayer player, Bonus bonus) {
        try {
            BonusCondition condition = ConditionParser.parse(bonus.getCondition());
            if (condition.test(player)) {
                if (!stackManager.shouldApply(player.getUUID().toString(), bonus)) {
                    return; // stacking limit reached
                }
                Optional<ActionType> action = registry.getAction(bonus.getActionType());
                action.ifPresent(a -> a.apply(player, bonus.getActionValue()));
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to apply bonus {} to {}", bonus, player.getName().getString(), e);
        }
    }

    private void removeBonus(ServerPlayer player, Bonus bonus) {
        try {
            Optional<ActionType> action = registry.getAction(bonus.getActionType());
            action.ifPresent(a -> a.remove(player, bonus.getActionValue()));
        } catch (Exception e) {
            Constants.LOG.error("Failed to remove bonus {} from {}", bonus, player.getName().getString(), e);
        }
    }
}
