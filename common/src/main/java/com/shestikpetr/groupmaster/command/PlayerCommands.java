package com.shestikpetr.groupmaster.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.shestikpetr.groupmaster.GroupMasterServer;
import com.shestikpetr.groupmaster.group.GroupManager;
import com.shestikpetr.groupmaster.model.Group;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PlayerCommands {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_GROUPS = (context, builder) -> {
        GroupManager gm = GroupMasterServer.getInstance().getGroupManager();
        return SharedSuggestionProvider.suggest(
                gm.getAllGroups().stream().map(Group::getId),
                builder
        );
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gm")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("player")
                        .then(Commands.literal("assign")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("group", StringArgumentType.word())
                                                .suggests(SUGGEST_GROUPS)
                                                .executes(PlayerCommands::assignPlayer)
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(PlayerCommands::removePlayer)
                                )
                        )
                        .then(Commands.literal("info")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(PlayerCommands::playerInfo)
                                )
                        )
                )
        );
    }

    private static int assignPlayer(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String groupId = StringArgumentType.getString(ctx, "group");
        GroupManager gm = GroupMasterServer.getInstance().getGroupManager();

        String assignedBy = ctx.getSource().getEntity() != null
                ? ctx.getSource().getEntity().getUUID().toString()
                : "console";

        if (gm.assignPlayer(player.getUUID(), player.getGameProfile().getName(), groupId, assignedBy)) {
            String groupName = gm.getGroup(groupId).map(Group::getDisplayName).orElse(groupId);
            ctx.getSource().sendSuccess(() -> Component.literal("Assigned ")
                    .append(Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" to group "))
                    .append(Component.literal(groupName).withStyle(ChatFormatting.GREEN)), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("Failed. Group may not exist: " + groupId));
            return 0;
        }
    }

    private static int removePlayer(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        GroupManager gm = GroupMasterServer.getInstance().getGroupManager();

        if (gm.removePlayer(player.getUUID())) {
            ctx.getSource().sendSuccess(() -> Component.literal("Removed ")
                    .append(Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" from their group")), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("Player is not in any group"));
            return 0;
        }
    }

    private static int playerInfo(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        GroupManager gm = GroupMasterServer.getInstance().getGroupManager();

        var data = gm.getPlayerGroup(player.getUUID());
        if (data.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal(player.getGameProfile().getName())
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(" is not in any group").withStyle(ChatFormatting.GRAY)), false);
            return 0;
        }

        var pd = data.get();
        var group = gm.getGroup(pd.getGroupId());
        String groupName = group.map(Group::getDisplayName).orElse(pd.getGroupId());

        var chain = gm.getHierarchyChain(pd.getGroupId());
        String chainStr = String.join(" -> ", chain.stream().map(Group::getDisplayName).toList());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String assignedDate = sdf.format(new Date(pd.getAssignedAt()));

        ctx.getSource().sendSuccess(() -> Component.literal("=== " + player.getGameProfile().getName() + " ===").withStyle(ChatFormatting.GOLD), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Group: ").append(
                Component.literal(groupName).withStyle(ChatFormatting.GREEN)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Hierarchy: ").append(
                Component.literal(chainStr).withStyle(ChatFormatting.AQUA)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Assigned: ").append(
                Component.literal(assignedDate).withStyle(ChatFormatting.GRAY)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("By: ").append(
                Component.literal(pd.getAssignedBy()).withStyle(ChatFormatting.GRAY)), false);

        return 1;
    }
}
