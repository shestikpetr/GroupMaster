package com.shestikpetr.groupmaster;

import com.shestikpetr.groupmaster.command.GroupMasterCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class Groupmaster {

    public Groupmaster() {
        CommonClass.init();
        MinecraftForge.EVENT_BUS.register(this);
        Constants.LOG.info("GroupMaster Forge initialized");
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
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && GroupMasterServer.getInstance() != null) {
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
    public void onLivingDamage(LivingDamageEvent event) {
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
