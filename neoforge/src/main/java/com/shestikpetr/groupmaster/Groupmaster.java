package com.shestikpetr.groupmaster;

import com.shestikpetr.groupmaster.command.GroupMasterCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

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
}
