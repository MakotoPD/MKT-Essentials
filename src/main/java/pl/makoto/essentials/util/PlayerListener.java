package pl.makoto.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import pl.makoto.essentials.Config;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

import net.neoforged.bus.api.EventPriority;

import net.minecraft.server.TickTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = MKTEssentials.MODID)
public class PlayerListener {
    private static final Set<UUID> READY_PLAYERS = new HashSet<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Delay processing by 1 tick to ensure stability
        player.getServer().tell(new TickTask(player.getServer().getTickCount() + 1, () -> {
            if (player.getServer().getPlayerList().getPlayer(player.getUUID()) == null) return;

            READY_PLAYERS.add(player.getUUID());
            refreshNickname(player);

            if (Config.JOIN_QUIT_MESSAGES.get()) {
                if (AdminManager.isVanished(player.getUUID())) return;

                player.getServer().getPlayerList().broadcastSystemMessage(MessageUtils.format(player, Config.JOIN_MESSAGE.get()), false);
            }
        }));
    }

    @SubscribeEvent
    public static void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        READY_PLAYERS.remove(player.getUUID());

        if (Config.JOIN_QUIT_MESSAGES.get()) {
            if (AdminManager.isVanished(player.getUUID())) return;

            event.getEntity().getServer().getPlayerList().broadcastSystemMessage(MessageUtils.format(player, Config.QUIT_MESSAGE.get()), false);
        }
    }

    public static String getFullDisplayNameForTab(ServerPlayer player) {
        return getFullDisplayName(player, true);
    }

    public static String getPrefixForTab(ServerPlayer player) {
        PlayerData data = DataManager.getPlayerData(player.getUUID());
        String dot = data.isRecording() ? "&c\u25cf &r" : (data.isStreaming() ? "&d\u25cf &r" : "");
        return dot + LuckPermsHook.getPrefix(player);
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

        return dot + prefix + name + suffix;
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        
        // Cancel original message to remove <PlayerName> brackets
        event.setCanceled(true);

        Component finalMsg = MessageUtils.format(player, Config.CHAT_FORMAT.get().replace("{message}", "")).append(event.getMessage());
        
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

        // Handle Vanish Tab visibility
        boolean vanished = AdminManager.isVanished(player.getUUID());
        if (vanished) {
            // Remove from tab for everyone except admins
            ClientboundPlayerInfoUpdatePacket removePacket = new ClientboundPlayerInfoUpdatePacket(
                java.util.EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED), java.util.List.of(player)
            );
            // This is a bit complex in NeoForge 1.21.1, usually setInvisible(true) helps, 
            // but for full tab removal we need custom packet handling.
            // For now, we'll ensure the name is updated and invisibility is set.
        }
    }
}
