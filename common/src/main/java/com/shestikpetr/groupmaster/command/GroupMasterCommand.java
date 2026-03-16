package com.shestikpetr.groupmaster.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class GroupMasterCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        GroupCommands.register(dispatcher);
        PlayerCommands.register(dispatcher);
        BonusCommands.register(dispatcher);
    }
}
