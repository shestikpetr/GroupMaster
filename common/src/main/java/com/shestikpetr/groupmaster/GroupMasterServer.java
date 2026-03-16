package com.shestikpetr.groupmaster;

import com.shestikpetr.groupmaster.group.GroupManager;
import com.shestikpetr.groupmaster.storage.DatabaseManager;
import com.shestikpetr.groupmaster.storage.GroupRepository;
import com.shestikpetr.groupmaster.storage.PlayerRepository;
import com.shestikpetr.groupmaster.web.WebConfig;
import com.shestikpetr.groupmaster.web.WebServer;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public class GroupMasterServer {

    private static GroupMasterServer instance;

    private final MinecraftServer server;
    private DatabaseManager databaseManager;
    private GroupManager groupManager;
    private WebServer webServer;

    private GroupMasterServer(MinecraftServer server) {
        this.server = server;
    }

    public static void start(MinecraftServer server) {
        instance = new GroupMasterServer(server);
        instance.init();
    }

    public static void stop() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }

    public static GroupMasterServer getInstance() {
        return instance;
    }

    private void init() {
        Path serverDir = server.getServerDirectory();

        databaseManager = new DatabaseManager(serverDir);
        databaseManager.init();

        GroupRepository groupRepo = new GroupRepository(databaseManager);
        PlayerRepository playerRepo = new PlayerRepository(databaseManager);

        groupManager = new GroupManager(groupRepo, playerRepo);
        groupManager.loadAll();

        WebConfig webConfig = WebConfig.load(serverDir);
        webServer = new WebServer(webConfig, groupManager);
        webServer.start();

        Constants.LOG.info("GroupMaster server initialized");
    }

    private void shutdown() {
        if (webServer != null) {
            webServer.stop();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        Constants.LOG.info("GroupMaster server shut down");
    }

    public MinecraftServer getServer() {
        return server;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
