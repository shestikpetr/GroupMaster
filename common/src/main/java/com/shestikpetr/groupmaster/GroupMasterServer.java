package com.shestikpetr.groupmaster;

import com.shestikpetr.groupmaster.bonus.BonusApplier;
import com.shestikpetr.groupmaster.bonus.BonusRegistry;
import com.shestikpetr.groupmaster.bonus.BonusResolver;
import com.shestikpetr.groupmaster.bonus.BonusTickHandler;
import com.shestikpetr.groupmaster.bonus.EventBonusManager;
import com.shestikpetr.groupmaster.bonus.StackManager;
import com.shestikpetr.groupmaster.config.ConfigImporter;
import com.shestikpetr.groupmaster.group.GroupManager;
import com.shestikpetr.groupmaster.model.PlayerGroupData;
import com.shestikpetr.groupmaster.storage.BonusRepository;
import com.shestikpetr.groupmaster.storage.DatabaseManager;
import com.shestikpetr.groupmaster.storage.GroupRepository;
import com.shestikpetr.groupmaster.storage.PlayerRepository;
import com.shestikpetr.groupmaster.storage.StackRepository;
import com.shestikpetr.groupmaster.web.WebConfig;
import com.shestikpetr.groupmaster.web.WebServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.Optional;

public class GroupMasterServer {

    private static GroupMasterServer instance;

    private final MinecraftServer server;
    private DatabaseManager databaseManager;
    private GroupManager groupManager;
    private BonusRegistry bonusRegistry;
    private BonusRepository bonusRepository;
    private BonusResolver bonusResolver;
    private BonusApplier bonusApplier;
    private BonusTickHandler bonusTickHandler;
    private EventBonusManager eventBonusManager;
    private StackManager stackManager;
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

        bonusRegistry = new BonusRegistry();
        bonusRepository = new BonusRepository(databaseManager);
        bonusResolver = new BonusResolver(bonusRepository, groupManager::getHierarchyChain);

        // Auto-import config if database is empty
        if (groupManager.getAllGroups().isEmpty()) {
            ConfigImporter importer = new ConfigImporter(groupRepo, bonusRepository, getConfigDir());
            if (importer.configFileExists()) {
                try {
                    ConfigImporter.ImportResult result = importer.importConfig(false);
                    if (!result.skipped()) {
                        groupManager.loadAll();
                        Constants.LOG.info("Auto-imported config: {} groups, {} bonuses",
                                result.groups(), result.bonuses());
                    }
                } catch (Exception e) {
                    Constants.LOG.error("Failed to auto-import config", e);
                }
            }
        }

        StackRepository stackRepo = new StackRepository(databaseManager);
        stackManager = new StackManager(stackRepo);

        bonusApplier = new BonusApplier(bonusRegistry, bonusResolver, stackManager);
        bonusTickHandler = new BonusTickHandler(groupManager, bonusApplier);
        eventBonusManager = new EventBonusManager(groupManager, bonusRegistry, bonusResolver, stackManager);

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

    public BonusRegistry getBonusRegistry() {
        return bonusRegistry;
    }

    public BonusRepository getBonusRepository() {
        return bonusRepository;
    }

    public BonusResolver getBonusResolver() {
        return bonusResolver;
    }

    public BonusApplier getBonusApplier() {
        return bonusApplier;
    }

    public BonusTickHandler getBonusTickHandler() {
        return bonusTickHandler;
    }

    public EventBonusManager getEventBonusManager() {
        return eventBonusManager;
    }

    public StackManager getStackManager() {
        return stackManager;
    }

    /**
     * Called every server tick by platform-specific event handlers.
     */
    public void onServerTick(MinecraftServer server) {
        if (bonusTickHandler != null) {
            bonusTickHandler.onTick(server);
        }
    }

    public Path getConfigDir() {
        return server.getServerDirectory().resolve("config").resolve("groupmaster");
    }

    /**
     * Reload state after config import.
     * @param removeOnly true = only remove active bonuses (before import), false = reload caches and re-apply
     */
    public void reloadState(boolean removeOnly) {
        if (removeOnly) {
            // Remove active bonuses from all online players
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Optional<PlayerGroupData> data = groupManager.getPlayerGroup(player.getUUID());
                data.ifPresent(pgd -> bonusApplier.removeJoinBonuses(player, pgd.getGroupId()));
            }
            return;
        }

        // Reload group cache from DB
        groupManager.loadAll();

        // Re-apply join bonuses for all online players
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Optional<PlayerGroupData> data = groupManager.getPlayerGroup(player.getUUID());
            data.ifPresent(pgd -> bonusApplier.applyJoinBonuses(player, pgd.getGroupId()));
        }

        Constants.LOG.info("GroupMaster state reloaded");
    }
}
