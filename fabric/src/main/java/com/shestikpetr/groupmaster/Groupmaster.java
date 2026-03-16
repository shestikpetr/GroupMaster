package com.shestikpetr.groupmaster;

import com.shestikpetr.groupmaster.command.GroupMasterCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

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

        Constants.LOG.info("GroupMaster Fabric initialized");
    }
}
