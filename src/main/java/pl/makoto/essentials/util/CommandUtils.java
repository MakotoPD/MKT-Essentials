package pl.makoto.essentials.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import pl.makoto.essentials.MKTEssentials;

import java.lang.reflect.Field;
import java.util.Map;

public final class CommandUtils {

    private CommandUtils() {}

    // Brigadier's CommandNode keeps three private maps (children/literals/arguments) and
    // exposes no removal API. Overriding a vanilla command that is a *redirect* (e.g. /tp →
    // /teleport, /w → /msg) can't be done by re-registering, because register() merges by
    // name and parsing follows the redirect before our merged children are ever tried. The
    // only way to make our version win is to drop the vanilla node first, which needs
    // reflection into those maps. Guarded so a locked-down module degrades to a logged
    // warning (vanilla command stays) instead of crashing command registration.
    private static final String[] NODE_MAPS = {"children", "literals", "arguments"};

    /**
     * Removes a top-level command literal from the dispatcher so it can be re-registered by
     * this mod. Returns true if removal succeeded (or the command wasn't present).
     */
    public static boolean removeRootCommand(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        RootCommandNode<CommandSourceStack> root = dispatcher.getRoot();
        try {
            for (String fieldName : NODE_MAPS) {
                Field field = CommandNode.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(root);
                if (value instanceof Map<?, ?> map) {
                    map.remove(name);
                }
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            MKTEssentials.LOGGER.warn(
                    "Could not remove vanilla command '{}' to override it — the mod's version may be shadowed by vanilla.",
                    name, e);
            return false;
        }
    }
}
