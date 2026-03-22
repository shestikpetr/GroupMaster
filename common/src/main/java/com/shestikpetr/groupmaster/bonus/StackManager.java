package com.shestikpetr.groupmaster.bonus;

import com.shestikpetr.groupmaster.Constants;
import com.shestikpetr.groupmaster.model.Bonus;
import com.shestikpetr.groupmaster.storage.StackRepository;

import java.sql.SQLException;

/**
 * Manages bonus stacking logic. Shared between BonusApplier and EventBonusManager.
 *
 * Stack modes:
 * - "each": action fires on every stack increment, up to max_stacks
 * - "threshold": action fires once when stacks reach max_stacks, then auto-resets
 */
public class StackManager {

    private final StackRepository stackRepo;

    public StackManager(StackRepository stackRepo) {
        this.stackRepo = stackRepo;
    }

    /**
     * Check whether a bonus should be applied, considering stacking rules.
     * Automatically increments stacks as a side effect.
     *
     * @return true if the action should fire
     */
    public boolean shouldApply(String playerUuid, Bonus bonus) {
        if (!bonus.hasStacking()) {
            return true; // no stacking, always apply
        }

        try {
            if ("each".equals(bonus.getStackMode())) {
                int current = stackRepo.getStacks(playerUuid, bonus.getId());
                if (current < bonus.getMaxStacks()) {
                    stackRepo.incrementAndGet(playerUuid, bonus.getId());
                    return true;
                }
                return false; // capped
            }

            if ("threshold".equals(bonus.getStackMode())) {
                int newStacks = stackRepo.incrementAndGet(playerUuid, bonus.getId());
                if (newStacks >= bonus.getMaxStacks()) {
                    // Threshold reached — fire action and auto-reset
                    stackRepo.resetByPlayerAndBonus(playerUuid, bonus.getId());
                    return true;
                }
                return false; // still accumulating
            }
        } catch (SQLException e) {
            Constants.LOG.error("Failed to check stacks for bonus {} player {}", bonus.getId(), playerUuid, e);
        }

        return true; // fallback: apply
    }

    /**
     * Reset stacks for bonuses with reset_on=on_death.
     */
    public void resetOnDeath(String playerUuid) {
        try {
            stackRepo.resetByPlayerAndResetType(playerUuid, "on_death");
        } catch (SQLException e) {
            Constants.LOG.error("Failed to reset on_death stacks for {}", playerUuid, e);
        }
    }

    /**
     * Reset stacks for bonuses with reset_on=on_leave.
     */
    public void resetOnLeave(String playerUuid) {
        try {
            stackRepo.resetByPlayerAndResetType(playerUuid, "on_leave");
        } catch (SQLException e) {
            Constants.LOG.error("Failed to reset on_leave stacks for {}", playerUuid, e);
        }
    }

    public int getStacks(String playerUuid, int bonusId) {
        try {
            return stackRepo.getStacks(playerUuid, bonusId);
        } catch (SQLException e) {
            Constants.LOG.error("Failed to get stacks for bonus {} player {}", bonusId, playerUuid, e);
            return 0;
        }
    }

    public void resetAll(String playerUuid) {
        try {
            stackRepo.resetAllForPlayer(playerUuid);
        } catch (SQLException e) {
            Constants.LOG.error("Failed to reset all stacks for {}", playerUuid, e);
        }
    }
}
