package com.shestikpetr.groupmaster.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.shestikpetr.groupmaster.GroupMasterServer;
import com.shestikpetr.groupmaster.config.ConfigExporter;
import com.shestikpetr.groupmaster.config.ConfigImporter;
import com.shestikpetr.groupmaster.storage.BonusRepository;
import com.shestikpetr.groupmaster.storage.GroupRepository;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public class ConfigCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gm")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("export")
                        .executes(ConfigCommands::exportConfig)
                )
                .then(Commands.literal("reload")
                        .executes(ConfigCommands::reloadConfig)
                )
        );
    }

    private static int exportConfig(CommandContext<CommandSourceStack> ctx) {
        GroupMasterServer server = GroupMasterServer.getInstance();
        Path configDir = server.getConfigDir();

        GroupRepository groupRepo = new GroupRepository(server.getDatabaseManager());
        BonusRepository bonusRepo = new BonusRepository(server.getDatabaseManager());
        ConfigExporter exporter = new ConfigExporter(groupRepo, bonusRepo, configDir);

        try {
            ConfigExporter.ExportResult result = exporter.export();
            ctx.getSource().sendSuccess(() -> Component.literal("Config exported: " +
                    result.groups() + " groups, " + result.bonuses() + " bonuses")
                    .withStyle(ChatFormatting.GREEN), true);
            ctx.getSource().sendSuccess(() -> Component.literal("File: " + result.file())
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Export failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> ctx) {
        GroupMasterServer server = GroupMasterServer.getInstance();
        Path configDir = server.getConfigDir();

        GroupRepository groupRepo = new GroupRepository(server.getDatabaseManager());
        BonusRepository bonusRepo = new BonusRepository(server.getDatabaseManager());
        ConfigImporter importer = new ConfigImporter(groupRepo, bonusRepo, configDir);

        if (!importer.configFileExists()) {
            ctx.getSource().sendFailure(Component.literal("Config file not found: " +
                    configDir.resolve("config.json")));
            ctx.getSource().sendFailure(Component.literal("Use /gm export first to create one")
                    .withStyle(ChatFormatting.GRAY));
            return 0;
        }

        try {
            // Remove active bonuses from online players before reload
            server.reloadState(true);

            ConfigImporter.ImportResult result = importer.importConfig(true);
            if (result.skipped()) {
                ctx.getSource().sendFailure(Component.literal("Reload failed: " + result.message()));
                return 0;
            }

            // Reload caches and re-apply bonuses
            server.reloadState(false);

            ctx.getSource().sendSuccess(() -> Component.literal("Config reloaded: " +
                    result.groups() + " groups, " + result.bonuses() + " bonuses")
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Reload failed: " + e.getMessage()));
            return 0;
        }
    }
}
