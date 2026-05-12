package pl.makoto.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.commands.MessagingCommands;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

import net.neoforged.bus.api.EventPriority;

import net.minecraft.server.TickTask;

import pl.makoto.essentials.data.BanEntry;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = MKTEssentials.MODID)
public class PlayerListener {
    private static final Set<UUID> READY_PLAYERS = new HashSet<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLoginBanCheck(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();

        // Shadowban check — BEFORE ban check so shadowban takes priority
        if (ShadowBanManager.isShadowBanned(uuid)) {
            String method = Settings.getShadowbanMethod();
            switch (method) {
                case "timeout" -> {
                    player.connection.disconnect(Component.literal("io.netty.channel.ConnectTimeoutException: connection timed out"));
                    return;
                }
                case "full" -> {
                    player.connection.disconnect(Component.literal("Disconnected"));
                    return;
                }
                case "internal-error" -> {
                    // Let them join, then kick after 40-60 ticks (2-3 seconds)
                    int delay = 40 + player.getServer().overworld().getRandom().nextInt(21);
                    player.getServer().tell(new TickTask(player.getServer().getTickCount() + delay, () -> {
                        ServerPlayer target = player.getServer().getPlayerList().getPlayer(uuid);
                        if (target != null) {
                            target.connection.disconnect(Component.literal(
                                    "Internal Exception: io.netty.handler.codec.DecoderException: java.lang.IndexOutOfBoundsException: readerIndex(47) + length(1) exceeds writerIndex(47)"));
                        }
                    }));
                }
                case "phantom" -> {
                    // Let them join, add to phantom set — handled in onPlayerJoin and onChat
                    ShadowBanManager.addPhantom(uuid);
                }
            }
        }

        if (BanManager.isBanned(uuid)) {
            BanEntry ban = BanManager.getBan(uuid);
            if (ban == null) return; // Race condition safety

            Component disconnectMessage;
            if (ban.isPermanent()) {
                disconnectMessage = MessageUtils.format(
                    "&c&lYou are banned from this server.\n\n&7Reason: &f" + ban.getReason()
                    + "\n&7Banned by: &f" + ban.getIssuer()
                );
            } else {
                long remaining = ban.getExpiresAt() - System.currentTimeMillis();
                String duration = DurationParser.format(remaining);
                disconnectMessage = MessageUtils.format(
                    "&c&lYou are temporarily banned.\n\n&7Reason: &f" + ban.getReason()
                    + "\n&7Banned by: &f" + ban.getIssuer()
                    + "\n&7Expires in: &f" + duration
                );
            }

            player.connection.disconnect(disconnectMessage);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Delay processing by 1 tick to ensure stability
        player.getServer().tell(new TickTask(player.getServer().getTickCount() + 1, () -> {
            if (player.getServer().getPlayerList().getPlayer(player.getUUID()) == null) return;

            READY_PLAYERS.add(player.getUUID());

            // Restore persisted states from PlayerData
            PlayerData data = DataManager.getPlayerData(player.getUUID());
            if (data.isGodMode()) {
                player.setInvulnerable(true);
            }
            if (data.isFlyEnabled()) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            if (data.isVanished()) {
                AdminManager.restoreVanish(player);
            }

            refreshNickname(player);

            // Hide vanished players from this joining player's tab list
            AdminManager.hideVanishedFromJoiningPlayer(player);

            // If this player is a phantom (shadowban), hide them from everyone's tab list
            if (ShadowBanManager.isPhantom(player.getUUID())) {
                hidePhantomFromTabList(player);
            }
            // Hide phantom players from this joining player's tab list
            hidePhantomPlayersFromJoiningPlayer(player);

            if (Settings.isJoinQuitEnabled()) {
                if (AdminManager.isVanished(player.getUUID())) return;
                // Don't broadcast join message for phantom players
                if (ShadowBanManager.isPhantom(player.getUUID())) return;

                player.getServer().getPlayerList().broadcastSystemMessage(MessageUtils.format(player, Settings.getJoinMessage()), false);
            }
        }));
    }

    @SubscribeEvent
    public static void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        READY_PLAYERS.remove(player.getUUID());

        // Clean up player-specific state to prevent memory leaks
        AFKManager.removePlayer(player.getUUID());
        MessagingCommands.cleanupPlayer(player.getUUID());
        TpaManager.cleanupPlayer(player.getUUID());
        AdminManager.cleanupOnDisconnect(player.getUUID());
        ShadowBanManager.removePhantom(player.getUUID());

        if (Settings.isJoinQuitEnabled()) {
            if (AdminManager.isVanished(player.getUUID())) {
                DataManager.evictPlayer(player.getUUID());
                return;
            }
            // Don't broadcast quit message for phantom players
            if (ShadowBanManager.isPhantom(player.getUUID())) {
                DataManager.evictPlayer(player.getUUID());
                return;
            }

            event.getEntity().getServer().getPlayerList().broadcastSystemMessage(MessageUtils.format(player, Settings.getQuitMessage()), false);
        }

        DataManager.evictPlayer(player.getUUID());
    }

    public static String getFullDisplayNameForTab(ServerPlayer player) {
        return getFullDisplayName(player, true);
    }

    public static String getPrefixForTab(ServerPlayer player) {
        String afk = AFKManager.isAFK(player.getUUID()) ? "&7[AFK] " : "";
        return afk + LuckPermsHook.getPrefix(player);
    }

    public static String getSuffixForTab(ServerPlayer player) {
        return LuckPermsHook.getSuffix(player);
    }

    public static String getFullDisplayName(ServerPlayer player, boolean forceIncludeLuckPerms) {
        boolean includeLuckPerms = forceIncludeLuckPerms && READY_PLAYERS.contains(player.getUUID());
        
        PlayerData data = DataManager.getPlayerData(player.getUUID());
        String name = data.getNickname() != null && !data.getNickname().isBlank() ? data.getNickname() : player.getScoreboardName();
        String dot = "";
        if (data.isRecording()) dot = "&c\u25cf &r";
        else if (data.isStreaming()) dot = "&d\u25cf &r";

        String prefix = includeLuckPerms ? LuckPermsHook.getPrefix(player) : "";
        String suffix = includeLuckPerms ? LuckPermsHook.getSuffix(player) : "";

        String afkPrefix = AFKManager.isAFK(player.getUUID()) ? "&7[AFK] " : "";
        return afkPrefix + dot + prefix + name + suffix;
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();

        // Phantom shadowban: cancel the message but echo it back to the sender
        if (ShadowBanManager.isPhantom(player.getUUID())) {
            event.setCanceled(true);
            // Build the message as if it was sent normally, so the player thinks it went through
            String group = LuckPermsHook.getPrimaryGroup(player);
            String chatFormat = Settings.getChatFormatForGroup(group);
            Component prefix = MessageUtils.format(player, chatFormat.replace("{message}", ""));
            String messageText = event.getMessage().getString();
            Component formattedMessage = MessageUtils.formatWithPermissions(player, messageText);
            Component finalMsg = prefix.copy().append(formattedMessage);
            player.sendSystemMessage(finalMsg);
            return;
        }

        // Mute check: prevent muted players from chatting
        PlayerData playerData = DataManager.getPlayerData(player.getUUID());
        long muteExpiration = playerData.getMuteExpiration();

        if (muteExpiration == -1) {
            // Permanently muted
            event.setCanceled(true);
            player.sendSystemMessage(MessageUtils.prefixed(I18n.get("moderation.permanently-muted")));
            return;
        } else if (muteExpiration > 0) {
            long now = System.currentTimeMillis();
            if (muteExpiration > now) {
                // Timed mute still active
                event.setCanceled(true);
                String remaining = formatRemainingTime(muteExpiration - now);
                player.sendSystemMessage(MessageUtils.prefixed(I18n.get("moderation.already-muted", "remaining", remaining)));
                return;
            } else {
                // Timed mute has expired, clear it
                playerData.setMuteExpiration(0);
                DataManager.savePlayerData(player.getUUID());
            }
        }
        // muteExpiration == 0: not muted, continue normally

        // Cancel original message to remove <PlayerName> brackets
        event.setCanceled(true);

        // Get per-group chat format (falls back to default if no group format defined)
        String group = LuckPermsHook.getPrimaryGroup(player);
        String chatFormat = Settings.getChatFormatForGroup(group);

        // Format the chat prefix (player name, rank, etc.) using placeholders
        Component prefix = MessageUtils.format(player, chatFormat.replace("{message}", ""));

        // Build hover text with rank, ping, and UUID
        MutableComponent hoverText = Component.empty();
        String rank = LuckPermsHook.getPrimaryGroup(player);
        if (rank != null) {
            hoverText.append(MessageUtils.format("&7Rank: &f" + rank + "\n"));
        }
        hoverText.append(MessageUtils.format("&7Ping: &f" + player.connection.latency() + "ms\n"));
        hoverText.append(MessageUtils.format("&7UUID: &f" + player.getUUID().toString()));

        // Attach HoverEvent to the prefix component
        Component prefixWithHover = prefix.copy().withStyle(style ->
            style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
        );

        // Format the player's message content with permission-based MiniMessage/legacy code filtering
        String messageText = event.getMessage().getString();
        Component formattedMessage = MessageUtils.formatWithPermissions(player, messageText);

        // Combine prefix (with hover) and formatted message
        Component finalMsg = prefixWithHover.copy().append(formattedMessage);
        
        // Broadcast to all players
        player.getServer().getPlayerList().broadcastSystemMessage(finalMsg, false);
        
        // Log to console manually since we cancelled the event
        MKTEssentials.LOGGER.info("[Chat] " + finalMsg.getString());
    }

    @SubscribeEvent
    public static void onTabListFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        event.setDisplayName(MessageUtils.format(player, "%mktessentials:full_name%"));
    }

    public static void refreshNickname(ServerPlayer player) {
        // Update TAB first. TAB caches forced tab-list display names, so the vanilla packet below
        // must see the freshly refreshed TAB value instead of the previous one.
        TABHook.refreshPlayer(player);

        // Trigger TabList format update (Minecraft)
        player.getServer().getPlayerList().broadcastAll(
            new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player)
        );

        // Handle Vanish Tab visibility via AdminManager
        if (AdminManager.isVanished(player.getUUID())) {
            AdminManager.hideFromTabList(player);
        }
    }

    private static String formatRemainingTime(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    /**
     * Hides a phantom player from the tab list for all other players.
     * The phantom player can still see everyone else.
     */
    private static void hidePhantomFromTabList(ServerPlayer phantomPlayer) {
        net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket removePacket =
                new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(java.util.List.of(phantomPlayer.getUUID()));

        for (ServerPlayer viewer : phantomPlayer.getServer().getPlayerList().getPlayers()) {
            if (viewer.getUUID().equals(phantomPlayer.getUUID())) continue; // Don't hide from self
            viewer.connection.send(removePacket);
        }
    }

    /**
     * Hides all currently phantom players from a joining player's tab list.
     * (The phantom players themselves can see the joining player.)
     */
    private static void hidePhantomPlayersFromJoiningPlayer(ServerPlayer joiningPlayer) {
        // Don't hide phantoms from themselves
        if (ShadowBanManager.isPhantom(joiningPlayer.getUUID())) return;

        for (ServerPlayer online : joiningPlayer.getServer().getPlayerList().getPlayers()) {
            if (ShadowBanManager.isPhantom(online.getUUID()) && !online.getUUID().equals(joiningPlayer.getUUID())) {
                net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket removePacket =
                        new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(java.util.List.of(online.getUUID()));
                joiningPlayer.connection.send(removePacket);
            }
        }
    }
}
