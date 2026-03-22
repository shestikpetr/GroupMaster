package com.shestikpetr.groupmaster;

import com.shestikpetr.groupmaster.command.GroupMasterCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public class Groupmaster implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        ServerLifecycleEvents.SERVER_STARTING.register(GroupMasterServer::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> GroupMasterServer.stop());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            GroupMasterCommand.register(dispatcher);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (GroupMasterServer.getInstance() != null) {
                GroupMasterServer.getInstance().onServerTick(server);
            }
        });

        // Event triggers
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayer sp && GroupMasterServer.getInstance() != null) {
                GroupMasterServer.getInstance().getEventBonusManager().onAttack(sp, entity);
            }
            return InteractionResult.PASS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer sp && GroupMasterServer.getInstance() != null) {
                GroupMasterServer.getInstance().getEventBonusManager().onDamaged(sp, source.getEntity());
            }
            return true; // never cancel damage, just trigger bonuses
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (GroupMasterServer.getInstance() == null) return;

            // Player died
            if (entity instanceof ServerPlayer sp) {
                GroupMasterServer.getInstance().getEventBonusManager().onDeath(sp);
            }

            // Player killed an entity
            if (source.getEntity() instanceof ServerPlayer killer) {
                GroupMasterServer.getInstance().getEventBonusManager().onKill(killer);
            }
        });

        Constants.LOG.info("GroupMaster Fabric initialized");
    }
}
