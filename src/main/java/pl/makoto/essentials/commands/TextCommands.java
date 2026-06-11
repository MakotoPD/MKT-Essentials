package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.util.List;

/**
 * Config-defined text commands (messages.yml → text-commands).
 * Each entry defines a list of aliases (e.g. "rules", "zasady") and the lines to send.
 * Registered LAST so existing commands always take precedence; conflicting aliases
 * are skipped with a warning.
 */
public class TextCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (Settings.TextCommand cmd : Settings.getTextCommands()) {
            for (String alias : cmd.aliases()) {
                String name = alias.toLowerCase().trim();
                if (name.isEmpty()) continue;
                if (dispatcher.getRoot().getChild(name) != null) {
                    MKTEssentials.LOGGER.warn("Text command alias '/{}' conflicts with an existing command — skipped.", name);
                    continue;
                }
                final List<String> lines = cmd.messages();
                dispatcher.register(Commands.literal(name)
                        .requires(source -> Permissions.hasPermission(source, "mktessentials.command.text." + cmd.name(), 0))
                        .executes(context -> {
                            for (String line : lines) {
                                context.getSource().sendSuccess(() -> MessageUtils.formatBypass(line), false);
                            }
                            return 1;
                        }));
            }
        }
    }
}
