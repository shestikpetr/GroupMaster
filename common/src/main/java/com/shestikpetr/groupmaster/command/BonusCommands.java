package com.shestikpetr.groupmaster.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.shestikpetr.groupmaster.GroupMasterServer;
import com.shestikpetr.groupmaster.bonus.BonusRegistry;
import com.shestikpetr.groupmaster.bonus.BonusResolver;
import com.shestikpetr.groupmaster.bonus.action.ActionType;
import com.shestikpetr.groupmaster.bonus.condition.ConditionParser;
import com.shestikpetr.groupmaster.model.Bonus;
import com.shestikpetr.groupmaster.model.Group;
import com.shestikpetr.groupmaster.storage.BonusRepository;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class BonusCommands {

    private static final Set<String> TRIGGERS = Set.of(
            "on_join", "on_leave", "tick",
            "on_attack", "on_damaged", "on_kill", "on_death", "on_eat"
    );
    private static final Set<String> TARGETS = Set.of("self", "victim");

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_GROUPS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    GroupMasterServer.getInstance().getGroupManager().getAllGroups().stream().map(Group::getId),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ACTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    GroupMasterServer.getInstance().getBonusRegistry().getActionIds(),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TRIGGERS = (context, builder) ->
            SharedSuggestionProvider.suggest(TRIGGERS, builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TARGETS = (context, builder) ->
            SharedSuggestionProvider.suggest(TARGETS, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gm")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("bonus")
                        // /gm bonus add <group> <trigger> <actionType> <actionValue>
                        .then(Commands.literal("add")
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests(SUGGEST_GROUPS)
                                        .then(Commands.argument("trigger", StringArgumentType.word())
                                                .suggests(SUGGEST_TRIGGERS)
                                                .then(Commands.argument("actionType", StringArgumentType.word())
                                                        .suggests(SUGGEST_ACTIONS)
                                                        .then(Commands.argument("actionValue", StringArgumentType.greedyString())
                                                                .executes(ctx -> addBonus(ctx, false, "self"))
                                                        )
                                                )
                                        )
                                )
                        )
                        // /gm bonus addoverride <group> <trigger> <actionType> <actionValue>
                        .then(Commands.literal("addoverride")
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests(SUGGEST_GROUPS)
                                        .then(Commands.argument("trigger", StringArgumentType.word())
                                                .suggests(SUGGEST_TRIGGERS)
                                                .then(Commands.argument("actionType", StringArgumentType.word())
                                                        .suggests(SUGGEST_ACTIONS)
                                                        .then(Commands.argument("actionValue", StringArgumentType.greedyString())
                                                                .executes(ctx -> addBonus(ctx, true, "self"))
                                                        )
                                                )
                                        )
                                )
                        )
                        // /gm bonus addtarget <group> <trigger> <target> <actionType> <actionValue>
                        .then(Commands.literal("addtarget")
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests(SUGGEST_GROUPS)
                                        .then(Commands.argument("trigger", StringArgumentType.word())
                                                .suggests(SUGGEST_TRIGGERS)
                                                .then(Commands.argument("target", StringArgumentType.word())
                                                        .suggests(SUGGEST_TARGETS)
                                                        .then(Commands.argument("actionType", StringArgumentType.word())
                                                                .suggests(SUGGEST_ACTIONS)
                                                                .then(Commands.argument("actionValue", StringArgumentType.greedyString())
                                                                        .executes(ctx -> {
                                                                            String target = StringArgumentType.getString(ctx, "target");
                                                                            return addBonus(ctx, false, target);
                                                                        })
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        // /gm bonus condition <bonusId> <conditionJson>
                        .then(Commands.literal("condition")
                                .then(Commands.argument("bonusId", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("condition", StringArgumentType.greedyString())
                                                .executes(BonusCommands::setCondition)
                                        )
                                )
                        )
                        // /gm bonus interval <bonusId> <ticks>
                        .then(Commands.literal("interval")
                                .then(Commands.argument("bonusId", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                                                .executes(BonusCommands::setInterval)
                                        )
                                )
                        )
                        // /gm bonus stacks <bonusId> <maxStacks> <stackMode> <resetOn>
                        .then(Commands.literal("stacks")
                                .then(Commands.argument("bonusId", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("maxStacks", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("stackMode", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("each", "threshold"), builder))
                                                        .then(Commands.argument("resetOn", StringArgumentType.word())
                                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("never", "on_death", "on_leave"), builder))
                                                                .executes(BonusCommands::setStacks)
                                                        )
                                                )
                                        )
                                )
                        )
                        // /gm bonus remove <id>
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                        .executes(BonusCommands::removeBonus)
                                )
                        )
                        // /gm bonus list <group>
                        .then(Commands.literal("list")
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests(SUGGEST_GROUPS)
                                        .executes(BonusCommands::listBonuses)
                                )
                        )
                        // /gm bonus effective <group>
                        .then(Commands.literal("effective")
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests(SUGGEST_GROUPS)
                                        .executes(BonusCommands::effectiveBonuses)
                                )
                        )
                        // /gm bonus types
                        .then(Commands.literal("types")
                                .executes(BonusCommands::listTypes)
                        )
                )
        );
    }

    private static int addBonus(CommandContext<CommandSourceStack> ctx, boolean override, String target) {
        String groupId = StringArgumentType.getString(ctx, "group");
        String trigger = StringArgumentType.getString(ctx, "trigger");
        String actionType = StringArgumentType.getString(ctx, "actionType");
        String actionValue = StringArgumentType.getString(ctx, "actionValue");

        GroupMasterServer server = GroupMasterServer.getInstance();

        if (server.getGroupManager().getGroup(groupId).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Group not found: " + groupId));
            return 0;
        }

        if (!TRIGGERS.contains(trigger)) {
            ctx.getSource().sendFailure(Component.literal("Invalid trigger. Use: " + String.join(", ", TRIGGERS)));
            return 0;
        }

        if (!TARGETS.contains(target)) {
            ctx.getSource().sendFailure(Component.literal("Invalid target. Use: self, victim"));
            return 0;
        }

        Optional<ActionType> action = server.getBonusRegistry().getAction(actionType);
        if (action.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Unknown action type: " + actionType + ". Use /gm bonus types"));
            return 0;
        }

        String error = action.get().validate(actionValue);
        if (error != null) {
            ctx.getSource().sendFailure(Component.literal("Invalid value: " + error));
            return 0;
        }

        String mergeKey = action.get().extractMergeKey(actionValue);
        Bonus bonus = new Bonus(groupId, trigger, 20, "{}", actionType, actionValue, mergeKey, override, target);

        try {
            server.getBonusRepository().create(bonus);
            String desc = action.get().describe(actionValue);
            ctx.getSource().sendSuccess(() -> Component.literal("Bonus #" + bonus.getId() + " added to ")
                    .append(Component.literal(groupId).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" [" + trigger + "] "))
                    .append(Component.literal(desc).withStyle(ChatFormatting.AQUA))
                    .append(override ? Component.literal(" [OVERRIDE]").withStyle(ChatFormatting.RED) : Component.empty()),
                    true);
            if (trigger.equals("tick")) {
                ctx.getSource().sendSuccess(() -> Component.literal("Tip: set condition with /gm bonus condition " + bonus.getId() + " {\"type\":\"in_sunlight\"}")
                        .withStyle(ChatFormatting.GRAY), false);
            }
            return 1;
        } catch (SQLException e) {
            ctx.getSource().sendFailure(Component.literal("Database error: " + e.getMessage()));
            return 0;
        }
    }

    private static int setCondition(CommandContext<CommandSourceStack> ctx) {
        int bonusId = IntegerArgumentType.getInteger(ctx, "bonusId");
        String conditionJson = StringArgumentType.getString(ctx, "condition");

        String error = ConditionParser.validate(conditionJson);
        if (error != null) {
            ctx.getSource().sendFailure(Component.literal("Invalid condition: " + error));
            return 0;
        }

        BonusRepository repo = GroupMasterServer.getInstance().getBonusRepository();
        try {
            // We need to update just the condition — use raw SQL for simplicity
            var conn = GroupMasterServer.getInstance().getDatabaseManager().getConnection();
            try (var stmt = conn.prepareStatement("UPDATE bonuses SET condition = ? WHERE id = ?")) {
                stmt.setString(1, conditionJson);
                stmt.setInt(2, bonusId);
                if (stmt.executeUpdate() > 0) {
                    String desc = ConditionParser.describe(conditionJson);
                    ctx.getSource().sendSuccess(() -> Component.literal("Condition set for bonus #" + bonusId + ": ")
                            .append(Component.literal(desc).withStyle(ChatFormatting.AQUA)), true);
                    return 1;
                } else {
                    ctx.getSource().sendFailure(Component.literal("Bonus #" + bonusId + " not found"));
                    return 0;
                }
            }
        } catch (SQLException e) {
            ctx.getSource().sendFailure(Component.literal("Database error: " + e.getMessage()));
            return 0;
        }
    }

    private static int setInterval(CommandContext<CommandSourceStack> ctx) {
        int bonusId = IntegerArgumentType.getInteger(ctx, "bonusId");
        int ticks = IntegerArgumentType.getInteger(ctx, "ticks");

        if (ticks < 1 || ticks > 72000) {
            ctx.getSource().sendFailure(Component.literal("Tick interval must be between 1 and 72000"));
            return 0;
        }

        try {
            var conn = GroupMasterServer.getInstance().getDatabaseManager().getConnection();
            try (var stmt = conn.prepareStatement("UPDATE bonuses SET tick_interval = ? WHERE id = ?")) {
                stmt.setInt(1, ticks);
                stmt.setInt(2, bonusId);
                if (stmt.executeUpdate() > 0) {
                    ctx.getSource().sendSuccess(() -> Component.literal("Interval set for bonus #" + bonusId + ": " + ticks + " ticks (" + (ticks / 20.0) + "s)")
                            .withStyle(ChatFormatting.GREEN), true);
                    return 1;
                } else {
                    ctx.getSource().sendFailure(Component.literal("Bonus #" + bonusId + " not found"));
                    return 0;
                }
            }
        } catch (SQLException e) {
            ctx.getSource().sendFailure(Component.literal("Database error: " + e.getMessage()));
            return 0;
        }
    }

    private static int setStacks(CommandContext<CommandSourceStack> ctx) {
        int bonusId = IntegerArgumentType.getInteger(ctx, "bonusId");
        int maxStacks = IntegerArgumentType.getInteger(ctx, "maxStacks");
        String stackMode = StringArgumentType.getString(ctx, "stackMode");
        String resetOn = StringArgumentType.getString(ctx, "resetOn");

        if (!Set.of("each", "threshold").contains(stackMode)) {
            ctx.getSource().sendFailure(Component.literal("Invalid stack mode. Use: each, threshold"));
            return 0;
        }
        if (!Set.of("never", "on_death", "on_leave").contains(resetOn)) {
            ctx.getSource().sendFailure(Component.literal("Invalid reset_on. Use: never, on_death, on_leave"));
            return 0;
        }

        try {
            var conn = GroupMasterServer.getInstance().getDatabaseManager().getConnection();
            try (var stmt = conn.prepareStatement("UPDATE bonuses SET max_stacks = ?, stack_mode = ?, reset_on = ? WHERE id = ?")) {
                stmt.setInt(1, maxStacks);
                stmt.setString(2, stackMode);
                stmt.setString(3, resetOn);
                stmt.setInt(4, bonusId);
                if (stmt.executeUpdate() > 0) {
                    ctx.getSource().sendSuccess(() -> Component.literal("Stacking set for bonus #" + bonusId + ": " +
                            maxStacks + "x " + stackMode + ", reset: " + resetOn)
                            .withStyle(ChatFormatting.GREEN), true);
                    return 1;
                } else {
                    ctx.getSource().sendFailure(Component.literal("Bonus #" + bonusId + " not found"));
                    return 0;
                }
            }
        } catch (SQLException e) {
            ctx.getSource().sendFailure(Component.literal("Database error: " + e.getMessage()));
            return 0;
        }
    }

    private static int removeBonus(CommandContext<CommandSourceStack> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        try {
            if (GroupMasterServer.getInstance().getBonusRepository().delete(id)) {
                ctx.getSource().sendSuccess(() -> Component.literal("Bonus #" + id + " removed").withStyle(ChatFormatting.GREEN), true);
                return 1;
            } else {
                ctx.getSource().sendFailure(Component.literal("Bonus #" + id + " not found"));
                return 0;
            }
        } catch (SQLException e) {
            ctx.getSource().sendFailure(Component.literal("Database error: " + e.getMessage()));
            return 0;
        }
    }

    private static int listBonuses(CommandContext<CommandSourceStack> ctx) {
        String groupId = StringArgumentType.getString(ctx, "group");
        GroupMasterServer server = GroupMasterServer.getInstance();

        if (server.getGroupManager().getGroup(groupId).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Group not found: " + groupId));
            return 0;
        }

        List<Bonus> bonuses = server.getBonusResolver().getDirectBonuses(groupId);
        if (bonuses.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No bonuses for " + groupId).withStyle(ChatFormatting.GRAY), false);
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("=== Bonuses for " + groupId + " ===").withStyle(ChatFormatting.GOLD), false);
        for (Bonus b : bonuses) {
            printBonus(ctx, server, b);
        }
        return 1;
    }

    private static int effectiveBonuses(CommandContext<CommandSourceStack> ctx) {
        String groupId = StringArgumentType.getString(ctx, "group");
        GroupMasterServer server = GroupMasterServer.getInstance();

        if (server.getGroupManager().getGroup(groupId).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Group not found: " + groupId));
            return 0;
        }

        List<Bonus> bonuses = server.getBonusResolver().resolve(groupId);
        if (bonuses.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No effective bonuses for " + groupId).withStyle(ChatFormatting.GRAY), false);
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("=== Effective bonuses for " + groupId + " ===").withStyle(ChatFormatting.GOLD), false);
        for (Bonus b : bonuses) {
            printBonus(ctx, server, b);
        }
        return 1;
    }

    private static void printBonus(CommandContext<CommandSourceStack> ctx, GroupMasterServer server, Bonus b) {
        String desc = server.getBonusRegistry().getAction(b.getActionType())
                .map(a -> a.describe(b.getActionValue()))
                .orElse(b.getActionValue());
        String condDesc = ConditionParser.describe(b.getCondition());

        ctx.getSource().sendSuccess(() -> Component.literal("#" + b.getId() + " ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("[" + b.getTrigger()).withStyle(ChatFormatting.DARK_AQUA))
                .append(b.isTickBased() ? Component.literal("/" + b.getTickInterval() + "t") : Component.empty())
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("[" + b.getActionType() + "] ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(desc).withStyle(ChatFormatting.WHITE))
                .append(!condDesc.equals("always") ? Component.literal(" IF " + condDesc).withStyle(ChatFormatting.LIGHT_PURPLE) : Component.empty())
                .append(!"self".equals(b.getTarget()) ? Component.literal(" →" + b.getTarget()).withStyle(ChatFormatting.GOLD) : Component.empty())
                .append(b.hasStacking() ? Component.literal(" [" + b.getMaxStacks() + "x " + b.getStackMode() +
                        (!"never".equals(b.getResetOn()) ? " reset:" + b.getResetOn() : "") + "]")
                        .withStyle(ChatFormatting.DARK_GREEN) : Component.empty())
                .append(b.isOverride() ? Component.literal(" [OVR]").withStyle(ChatFormatting.RED) : Component.empty())
                .append(Component.literal(" (from " + b.getGroupId() + ")").withStyle(ChatFormatting.DARK_GRAY)),
                false);
    }

    private static int listTypes(CommandContext<CommandSourceStack> ctx) {
        BonusRegistry registry = GroupMasterServer.getInstance().getBonusRegistry();

        ctx.getSource().sendSuccess(() -> Component.literal("=== Action Types ===").withStyle(ChatFormatting.GOLD), false);
        for (ActionType type : registry.getAllActions()) {
            ctx.getSource().sendSuccess(() -> Component.literal(type.getId())
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" - " + type.getDisplayName()).withStyle(ChatFormatting.WHITE)), false);
        }

        ctx.getSource().sendSuccess(() -> Component.literal("=== Triggers ===").withStyle(ChatFormatting.GOLD), false);
        ctx.getSource().sendSuccess(() -> Component.literal("on_join").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - applied when player joins group / logs in").withStyle(ChatFormatting.WHITE)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("on_leave").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - applied when player leaves group").withStyle(ChatFormatting.WHITE)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("tick").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - checked every N ticks (use /gm bonus interval)").withStyle(ChatFormatting.WHITE)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("on_attack").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - when player attacks an entity").withStyle(ChatFormatting.WHITE)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("on_damaged").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - when player takes damage").withStyle(ChatFormatting.WHITE)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("on_kill").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - when player kills an entity").withStyle(ChatFormatting.WHITE)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("on_death").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - when player dies").withStyle(ChatFormatting.WHITE)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("on_eat").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - when player eats food").withStyle(ChatFormatting.WHITE)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("=== Targets ===").withStyle(ChatFormatting.GOLD), false);
        ctx.getSource().sendSuccess(() -> Component.literal("self").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - action applies to the group member (default)").withStyle(ChatFormatting.WHITE)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("victim").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - action applies to the other player (PvP only)").withStyle(ChatFormatting.WHITE)), false);

        return 1;
    }
}
