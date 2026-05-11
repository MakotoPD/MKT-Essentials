package pl.makoto.essentials.util;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;

public final class MKTPlaceholders {
    private static boolean registered;

    private MKTPlaceholders() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        register("full_name", (player, argument) -> {
            if (player == null) return "";
            boolean includeLuckPerms = !"safe".equalsIgnoreCase(argument);
            return PlayerListener.getFullDisplayName(player, includeLuckPerms);
        });
        register("tab_full_name", (player, argument) -> player != null ? PlayerListener.getFullDisplayNameForTab(player) : "");
        register("prefix", (player, argument) -> player != null ? PlayerListener.getPrefixForTab(player) : "");
        register("suffix", (player, argument) -> player != null ? PlayerListener.getSuffixForTab(player) : "");
        register("name", (player, argument) -> player != null ? getName(player) : "");
        register("real_name", (player, argument) -> player != null ? player.getScoreboardName() : "");
        register("nick", (player, argument) -> {
            if (player == null) return "";
            String nickname = DataManager.getPlayerData(player.getUUID()).getNickname();
            return nickname != null ? nickname : "";
        });
        register("uuid", (player, argument) -> player != null ? player.getUUID().toString() : "");
        register("dot", (player, argument) -> {
            if (player == null) return "";
            PlayerData data = DataManager.getPlayerData(player.getUUID());
            if (data.isRecording()) return "&c\u25cf &r";
            if (data.isStreaming()) return "&d\u25cf &r";
            return "";
        });
        register("recording", (player, argument) -> {
            if (player == null) return "false";
            return String.valueOf(DataManager.getPlayerData(player.getUUID()).isRecording());
        });
        register("streaming", (player, argument) -> {
            if (player == null) return "false";
            return String.valueOf(DataManager.getPlayerData(player.getUUID()).isStreaming());
        });

        MKTEssentials.LOGGER.info("Registered MKT Essentials Text Placeholder API placeholders.");
    }

    private static void register(String path, Handler handler) {
        Placeholders.registerServer(ResourceLocation.fromNamespaceAndPath(MKTEssentials.MODID, path), (context, argument) -> {
            ServerPlayer player = context != null ? context.serverPlayer() : null;
            return PlaceholderResult.value(handler.resolve(player, argument));
        });
    }

    private static String getName(ServerPlayer player) {
        PlayerData data = DataManager.getPlayerData(player.getUUID());
        String nickname = data.getNickname();
        return nickname != null && !nickname.isBlank() ? nickname : player.getScoreboardName();
    }

    @FunctionalInterface
    private interface Handler {
        String resolve(ServerPlayer player, String argument);
    }
}
