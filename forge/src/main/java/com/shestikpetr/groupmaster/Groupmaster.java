package com.shestikpetr.groupmaster;

import com.shestikpetr.groupmaster.command.GroupMasterCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
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
}
