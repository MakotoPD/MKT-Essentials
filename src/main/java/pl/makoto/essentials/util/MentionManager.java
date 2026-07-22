package pl.makoto.essentials.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.data.DataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat @mentions, inspired by FlectonePulse's mention module.
 * <p>
 * Detects {@code @name} tokens in a chat message, highlights them in a configurable color and
 * collects the mentioned online players so a ping sound can be played to them. Highlighting is
 * injected AFTER permission filtering (via {@link MessageUtils#formatWithPermissions}) so it is
 * server-controlled and cannot be abused/stripped by the sender's own formatting permissions.
 */
public final class MentionManager {

    // @ followed by 1-16 name characters (Minecraft name charset).
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_]{1,16})");

    /** Formatted chat message plus the online players that were mentioned in it. */
    public record Result(MutableComponent message, List<ServerPlayer> mentioned) {}

    private MentionManager() {}

    public static Result process(ServerPlayer sender, String rawMessage) {
        List<ServerPlayer> mentioned = new ArrayList<>();
        MinecraftServer server = sender.getServer();
        if (server == null) {
            return new Result(MessageUtils.formatWithPermissions(sender, rawMessage), mentioned);
        }

        boolean mentionOn = Settings.isMentionEnabled();
        // Replacement (links/spoiler/emoji) is applied inside formatWithPermissions, so the
        // post-filter only does mention highlighting.
        MutableComponent message = MessageUtils.formatWithPermissions(sender, rawMessage,
                filtered -> mentionOn ? highlight(filtered, server, sender, mentioned) : filtered);
        return new Result(message, mentioned);
    }

    /**
     * Plays the configured mention ping to a player. No-op if the sound id is empty/invalid.
     */
    public static void playPing(ServerPlayer target) {
        // Respect the player's /chatsetting mentions toggle
        if (!DataManager.getPlayerData(target.getUUID()).getChatSetting("mentions", true)) return;
        String id = Settings.getMentionSound();
        if (id == null || id.isBlank()) return;
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return;
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(location).orElse(null);
        if (sound == null) return;
        target.playNotifySound(sound, SoundSource.MASTER, 1.0f, 1.0f);
    }

    private static String highlight(String filtered, MinecraftServer server, ServerPlayer sender, List<ServerPlayer> mentioned) {
        Matcher matcher = MENTION_PATTERN.matcher(filtered);
        String openTag = "<color:" + Settings.getMentionColor() + ">";
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            ServerPlayer target = resolve(server, matcher.group(1));
            boolean isSelf = target != null && target.getUUID().equals(sender.getUUID());
            if (target == null || (isSelf && !Settings.isMentionSelf())) {
                // Not a real mention — keep the token untouched.
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            if (!mentioned.contains(target)) mentioned.add(target);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(openTag + matcher.group() + "</color>"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Resolves a mention token to an online player by real account name or visible nickname
     * (case-insensitive). Returns null when no online player matches.
     */
    private static ServerPlayer resolve(MinecraftServer server, String name) {
        String needle = name.toLowerCase(Locale.ROOT);
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (online.getScoreboardName().toLowerCase(Locale.ROOT).equals(needle)) {
                return online;
            }
            String nick = DataManager.getPlayerData(online.getUUID()).getNickname();
            if (nick != null && !nick.isBlank()) {
                String visibleNick = MiniMessageParser.parse(nick).getString();
                if (visibleNick.toLowerCase(Locale.ROOT).equals(needle)) {
                    return online;
                }
            }
        }
        return null;
    }
}
