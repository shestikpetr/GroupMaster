package com.shestikpetr.groupmaster.bonus;

import com.shestikpetr.groupmaster.Constants;
import com.shestikpetr.groupmaster.bonus.action.ActionType;
import com.shestikpetr.groupmaster.bonus.condition.BonusCondition;
import com.shestikpetr.groupmaster.bonus.condition.ConditionParser;
import com.shestikpetr.groupmaster.group.GroupManager;
import com.shestikpetr.groupmaster.model.Bonus;
import com.shestikpetr.groupmaster.model.PlayerGroupData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Optional;

/**
 * Handles event-based bonus triggers (on_attack, on_damaged, on_kill, on_death, on_eat).
 * Called from platform-specific event listeners.
 */
public class EventBonusManager {

    private final GroupManager groupManager;
    private final BonusRegistry registry;
    private final BonusResolver resolver;
    private final StackManager stackManager;

    public EventBonusManager(GroupManager groupManager, BonusRegistry registry,
                             BonusResolver resolver, StackManager stackManager) {
        this.groupManager = groupManager;
        this.registry = registry;
        this.resolver = resolver;
        this.stackManager = stackManager;
    }

    /**
     * Player attacked an entity.
     */
    public void onAttack(ServerPlayer attacker, Entity victim) {
        Optional<PlayerGroupData> data = groupManager.getPlayerGroup(attacker.getUUID());
        data.ifPresent(pgd -> {
            List<Bonus> bonuses = resolver.resolveByTrigger(pgd.getGroupId(), "on_attack");
            for (Bonus bonus : bonuses) {
                if (bonus.isTargetSelf()) {
                    applyIfConditionMet(attacker, bonus);
                } else if (bonus.isTargetVictim() && victim instanceof ServerPlayer victimPlayer) {
                    applyIfConditionMet(attacker, victimPlayer, bonus);
                }
            }
        });
    }

    /**
     * Player took damage from an entity (or environment).
     */
    public void onDamaged(ServerPlayer victim, Entity attacker) {
        Optional<PlayerGroupData> data = groupManager.getPlayerGroup(victim.getUUID());
        data.ifPresent(pgd -> {
            List<Bonus> bonuses = resolver.resolveByTrigger(pgd.getGroupId(), "on_damaged");
            for (Bonus bonus : bonuses) {
                if (bonus.isTargetSelf()) {
                    applyIfConditionMet(victim, bonus);
                } else if (bonus.isTargetVictim() && attacker instanceof ServerPlayer attackerPlayer) {
                    applyIfConditionMet(victim, attackerPlayer, bonus);
                }
            }
        });
    }

    /**
     * Player killed an entity.
     */
    public void onKill(ServerPlayer killer) {
        Optional<PlayerGroupData> data = groupManager.getPlayerGroup(killer.getUUID());
        data.ifPresent(pgd -> {
            List<Bonus> bonuses = resolver.resolveByTrigger(pgd.getGroupId(), "on_kill");
            for (Bonus bonus : bonuses) {
                applyIfConditionMet(killer, bonus);
            }
        });
    }

    /**
     * Player died. Also resets stacks with reset_on=on_death.
     */
    public void onDeath(ServerPlayer player) {
        Optional<PlayerGroupData> data = groupManager.getPlayerGroup(player.getUUID());
        data.ifPresent(pgd -> {
            List<Bonus> bonuses = resolver.resolveByTrigger(pgd.getGroupId(), "on_death");
            for (Bonus bonus : bonuses) {
                applyIfConditionMet(player, bonus);
            }
        });

        // Reset stacks for bonuses with reset_on=on_death
        stackManager.resetOnDeath(player.getUUID().toString());
    }

    /**
     * Player ate food.
     */
    public void onEat(ServerPlayer player) {
        Optional<PlayerGroupData> data = groupManager.getPlayerGroup(player.getUUID());
        data.ifPresent(pgd -> {
            List<Bonus> bonuses = resolver.resolveByTrigger(pgd.getGroupId(), "on_eat");
            for (Bonus bonus : bonuses) {
                applyIfConditionMet(player, bonus);
            }
        });
    }

    private void applyIfConditionMet(ServerPlayer groupMember, Bonus bonus) {
        applyIfConditionMet(groupMember, groupMember, bonus);
    }

    private void applyIfConditionMet(ServerPlayer conditionSubject, ServerPlayer target, Bonus bonus) {
        try {
            BonusCondition condition = ConditionParser.parse(bonus.getCondition());
            if (condition.test(conditionSubject)) {
                if (!stackManager.shouldApply(conditionSubject.getUUID().toString(), bonus)) {
                    return; // stacking limit reached or threshold not yet met
                }
                Optional<ActionType> action = registry.getAction(bonus.getActionType());
                action.ifPresent(a -> a.apply(target, bonus.getActionValue()));
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to apply event bonus {} to {}", bonus, target.getName().getString(), e);
        }
    }
}
