package com.shestikpetr.groupmaster.bonus.condition;

import net.minecraft.server.level.ServerPlayer;

/**
 * A condition that determines whether a bonus should be applied.
 * Conditions are composable via AND, OR, NOT.
 */
@FunctionalInterface
public interface BonusCondition {

    boolean test(ServerPlayer player);
}
