package pl.makoto.essentials.util;

import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.config.Settings;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Personal welcome shown to a joining player, inspired by FlectonePulse's greeting module.
 * Supports chat / action bar / title, and — like FlectonePulse — a "skin face" rendered from the
 * player's avatar: lines containing the {@code [#][#][#][#][#][#][#][#]} marker are replaced with
 * rows of colored block pixels of the player's face (fetched asynchronously).
 */
public final class GreetingManager {

    private static final String MARKER = "[#][#][#][#][#][#][#][#]";

    private GreetingManager() {}

    public static void send(ServerPlayer player, boolean firstJoin) {
        if (!Settings.isGreetingEnabled()) return;

        List<String> lines = firstJoin ? Settings.getGreetingFirstJoin() : Settings.getGreetingReturning();
        if (lines == null || lines.isEmpty()) return;

        String type = Settings.getGreetingType();
        String mode = type == null ? "chat" : type.toLowerCase(Locale.ROOT);

        boolean face = mode.equals("chat") && Settings.isGreetingSkin() && lines.stream().anyMatch(l -> l.contains(MARKER));
        if (!face) {
            deliver(player, lines, mode);
            return;
        }

        // Render the face off-thread (network), then deliver on the server thread.
        // Use the account name (works in offline mode too), keeping <uuid> for compatibility.
        UUID uuid = player.getUUID();
        MinecraftServer server = player.getServer();
        String name = player.getScoreboardName();
        // Prefer the player's SkinsRestorer skin when that hook is active; otherwise the avatar API.
        String skinUrl = IntegrationsManager.getSkinTextureUrl(uuid, name);
        String avatarUrl = Settings.getGreetingAvatarUrl()
                .replace("<name>", name)
                .replace("<uuid>", uuid.toString());
        List<String> template = lines;

        CompletableFuture.supplyAsync(() ->
                skinUrl != null ? FaceRenderer.renderSkinTexture(skinUrl) : FaceRenderer.render(avatarUrl)
        ).thenAccept(rows -> server.execute(() -> {
            ServerPlayer online = server.getPlayerList().getPlayer(uuid);
            if (online != null) deliverChat(online, applyFace(template, rows));
        }));
    }

    /** Replaces each face marker with successive rendered rows; leftover markers are removed. */
    private static List<String> applyFace(List<String> lines, List<String> rows) {
        String joined = String.join("\n", lines);
        for (String row : rows) {
            int idx = joined.indexOf(MARKER);
            if (idx < 0) break;
            joined = joined.substring(0, idx) + row + joined.substring(idx + MARKER.length());
        }
        joined = joined.replace(MARKER, "");
        return Arrays.asList(joined.split("\n", -1));
    }

    private static void deliver(ServerPlayer player, List<String> lines, String mode) {
        switch (mode) {
            case "title" -> {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(
                        Settings.getGreetingTitleFadeIn(), Settings.getGreetingTitleStay(), Settings.getGreetingTitleFadeOut()));
                player.connection.send(new ClientboundSetTitleTextPacket(MessageUtils.formatBypass(player, lines.get(0))));
                if (lines.size() > 1) {
                    player.connection.send(new ClientboundSetSubtitleTextPacket(MessageUtils.formatBypass(player, lines.get(1))));
                }
            }
            case "actionbar" ->
                    player.connection.send(new ClientboundSetActionBarTextPacket(MessageUtils.formatBypass(player, lines.get(0))));
            default -> deliverChat(player, lines);
        }
    }

    private static void deliverChat(ServerPlayer player, List<String> lines) {
        for (String line : lines) {
            player.sendSystemMessage(MessageUtils.formatBypass(player, line));
        }
    }
}
