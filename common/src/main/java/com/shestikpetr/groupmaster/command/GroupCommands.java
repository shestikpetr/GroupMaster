package com.shestikpetr.groupmaster.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import net.minecraft.network.chat.Component;

import java.util.List;

public class GroupCommands {

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
                .then(Commands.literal("group")
                        .then(Commands.literal("create")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("displayName", StringArgumentType.string())
                                                .executes(ctx -> createGroup(ctx, null, 0))
                                                .then(Commands.argument("parent", StringArgumentType.word())
                                                        .suggests(SUGGEST_GROUPS)
                                                        .executes(ctx -> createGroup(ctx, StringArgumentType.getString(ctx, "parent"), 0))
                                                        .then(Commands.argument("priority", IntegerArgumentType.integer())
                                                                .executes(ctx -> createGroup(ctx,
                                                                        StringArgumentType.getString(ctx, "parent"),
                                                                        IntegerArgumentType.getInteger(ctx, "priority")))
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("delete")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(SUGGEST_GROUPS)
                                        .executes(GroupCommands::deleteGroup)
                                )
                        )
                        .then(Commands.literal("list")
                                .executes(GroupCommands::listGroups)
                        )
                        .then(Commands.literal("info")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(SUGGEST_GROUPS)
                                        .executes(GroupCommands::groupInfo)
                                )
                        )
                        .then(Commands.literal("setparent")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(SUGGEST_GROUPS)
                                        .then(Commands.literal("none")
                                                .executes(GroupCommands::removeParent)
                                        )
                                        .then(Commands.argument("parent", StringArgumentType.word())
                                                .suggests(SUGGEST_GROUPS)
                                                .executes(GroupCommands::setParent)
                                        )
                                )
                        )
                )
        );
    }

    private static int createGroup(CommandContext<CommandSourceStack> ctx, String parentId, int priority) {
        String id = StringArgumentType.getString(ctx, "id");
        String displayName = StringArgumentType.getString(ctx, "displayName");
        GroupManager gm = GroupMasterServer.getInstance().getGroupManager();

        if (gm.createGroup(id, displayName, parentId, priority)) {
            ctx.getSource().sendSuccess(() -> Component.literal("Group ")
                    .append(Component.literal(displayName).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" (" + id + ") created"))
                    .append(parentId != null ? Component.literal(" under ").append(Component.literal(parentId).withStyle(ChatFormatting.YELLOW)) : Component.empty()),
                    true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("Failed to create group. ID may already exist or parent not found."));
            return 0;
        }
    }

    private static int deleteGroup(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        GroupManager gm = GroupMasterServer.getInstance().getGroupManager();

        if (gm.deleteGroup(id)) {
            ctx.getSource().sendSuccess(() -> Component.literal("Group ")
                    .append(Component.literal(id).withStyle(ChatFormatting.RED))
                    .append(Component.literal(" deleted. Children re-parented.")), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("Group not found: " + id));
            return 0;
        }
    }

    private static int listGroups(CommandContext<CommandSourceStack> ctx) {
        GroupManager gm = GroupMasterServer.getInstance().getGroupManager();
        List<Group> roots = gm.getRootGroups();

        if (roots.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No groups defined.").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("=== Groups ===").withStyle(ChatFormatting.GOLD), false);
        for (Group root : roots) {
            printGroupTree(ctx, gm, root, 0);
        }
        return 1;
    }

    private static void printGroupTree(CommandContext<CommandSourceStack> ctx, GroupManager gm, Group group, int depth) {
        String indent = "  ".repeat(depth);
        String prefix = depth > 0 ? indent + "|- " : "";

        int playerCount = gm.getPlayersInGroup(group.getId()).size();
        ctx.getSource().sendSuccess(() -> Component.literal(prefix)
                .append(Component.literal(group.getDisplayName()).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" (" + group.getId() + ")").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" [" + playerCount + " players]").withStyle(ChatFormatting.AQUA)),
                false);

        for (Group child : gm.getChildren(group.getId())) {
            printGroupTree(ctx, gm, child, depth + 1);
        }
    }

    private static int groupInfo(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        GroupManager gm = GroupMasterServer.getInstance().getGroupManager();

        return gm.getGroup(id).map(group -> {
            ctx.getSource().sendSuccess(() -> Component.literal("=== " + group.getDisplayName() + " ===").withStyle(ChatFormatting.GOLD), false);
            ctx.getSource().sendSuccess(() -> Component.literal("ID: ").append(Component.literal(group.getId()).withStyle(ChatFormatting.WHITE)), false);
            ctx.getSource().sendSuccess(() -> Component.literal("Parent: ").append(
                    Component.literal(group.getParentId() != null ? group.getParentId() : "none").withStyle(ChatFormatting.YELLOW)), false);
            ctx.getSource().sendSuccess(() -> Component.literal("Priority: ").append(
                    Component.literal(String.valueOf(group.getPriority())).withStyle(ChatFormatting.WHITE)), false);

            List<Group> chain = gm.getHierarchyChain(group.getId());
            String chainStr = String.join(" -> ", chain.stream().map(Group::getDisplayName).toList());
            ctx.getSource().sendSuccess(() -> Component.literal("Hierarchy: ").append(
                    Component.literal(chainStr).withStyle(ChatFormatting.AQUA)), false);

            var players = gm.getPlayersInGroup(id);
            ctx.getSource().sendSuccess(() -> Component.literal("Players (" + players.size() + "): ").append(
                    Component.literal(players.isEmpty() ? "none" :
                            String.join(", ", players.stream().map(p -> p.getPlayerName()).toList()))
                            .withStyle(ChatFormatting.WHITE)), false);

            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("Group not found: " + id));
            return 0;
        });
    }

    private static int setParent(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String parentId = StringArgumentType.getString(ctx, "parent");
        GroupManager gm = GroupMasterServer.getInstance().getGroupManager();

        return gm.getGroup(id).map(group -> {
            if (gm.updateGroup(id, group.getDisplayName(), parentId, group.getPriority())) {
                ctx.getSource().sendSuccess(() -> Component.literal("Set parent of ")
                        .append(Component.literal(id).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" to "))
                        .append(Component.literal(parentId).withStyle(ChatFormatting.YELLOW)), true);
                return 1;
            } else {
                ctx.getSource().sendFailure(Component.literal("Failed. Parent not found or would create a cycle."));
                return 0;
            }
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("Group not found: " + id));
            return 0;
        });
    }

    private static int removeParent(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        GroupManager gm = GroupMasterServer.getInstance().getGroupManager();

        return gm.getGroup(id).map(group -> {
            if (gm.updateGroup(id, group.getDisplayName(), null, group.getPriority())) {
                ctx.getSource().sendSuccess(() -> Component.literal("Removed parent from ")
                        .append(Component.literal(id).withStyle(ChatFormatting.GREEN)), true);
                return 1;
            } else {
                ctx.getSource().sendFailure(Component.literal("Failed to update group."));
                return 0;
            }
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("Group not found: " + id));
            return 0;
        });
    }
}
