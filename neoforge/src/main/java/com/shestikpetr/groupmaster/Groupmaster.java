package com.shestikpetr.groupmaster;

import com.shestikpetr.groupmaster.command.GroupMasterCommand;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Constants.MOD_ID)
public class Groupmaster {

    public Groupmaster(IEventBus modEventBus) {
        CommonClass.init();
        NeoForge.EVENT_BUS.register(this);
        Constants.LOG.info("GroupMaster NeoForge initialized");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        GroupMasterServer.start(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        GroupMasterServer.stop();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GroupMasterCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (GroupMasterServer.getInstance() != null) {
            GroupMasterServer.getInstance().onServerTick(event.getServer());
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && GroupMasterServer.getInstance() != null) {
            GroupMasterServer.getInstance().getEventBonusManager().onAttack(sp, event.getTarget());
        }
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp && GroupMasterServer.getInstance() != null) {
            GroupMasterServer.getInstance().getEventBonusManager().onDamaged(sp, event.getSource().getEntity());
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (GroupMasterServer.getInstance() == null) return;

        // Player died
        if (event.getEntity() instanceof ServerPlayer sp) {
            GroupMasterServer.getInstance().getEventBonusManager().onDeath(sp);
        }

        // Player killed an entity
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            GroupMasterServer.getInstance().getEventBonusManager().onKill(killer);
        }
    }
}
