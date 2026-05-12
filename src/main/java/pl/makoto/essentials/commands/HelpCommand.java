package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * @deprecated Help functionality has been merged into {@link MKTCommand}.
 * This class is retained as a no-op to avoid breaking any external references.
 */
@Deprecated
public class HelpCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // No-op: help is now handled by MKTCommand
    }
}
