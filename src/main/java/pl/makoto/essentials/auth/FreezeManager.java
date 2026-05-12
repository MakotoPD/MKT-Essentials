package pl.makoto.essentials.auth;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.CommandEvent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.util.MessageUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = MKTEssentials.MODID)
public final class FreezeManager {

    private static final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, double[]> freezePositions = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> freezeStartTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> reminderTickCounters = new ConcurrentHashMap<>();

    public static final Set<String> ALLOWED_COMMANDS = Set.of("register", "login", "link", "mkt");

    private FreezeManager() {}

    public static void freeze(ServerPlayer player) {
        UUID uuid = player.getUUID();
        frozenPlayers.add(uuid);
        freezePositions.put(uuid, new double[]{player.getX(), player.getY(), player.getZ()});
        freezeStartTimes.put(uuid, System.currentTimeMillis());
        reminderTickCounters.put(uuid, 0);
    }

    public static void unfreeze(ServerPlayer player) {
        UUID uuid = player.getUUID();
        frozenPlayers.remove(uuid);
        freezePositions.remove(uuid);
        freezeStartTimes.remove(uuid);
        reminderTickCounters.remove(uuid);
    }

    public static boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }

    public static void cleanup(UUID uuid) {
        frozenPlayers.remove(uuid);
        freezePositions.remove(uuid);
        freezeStartTimes.remove(uuid);
        reminderTickCounters.remove(uuid);
    }

    // ─── Movement Lock ─────────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (!frozenPlayers.contains(uuid)) return;

        double[] pos = freezePositions.get(uuid);
        if (pos == null) return;

        double dx = player.getX() - pos[0];
        double dy = player.getY() - pos[1];
        double dz = player.getZ() - pos[2];
        if (dx * dx + dy * dy + dz * dz > 0.01) {
            player.teleportTo(pos[0], pos[1], pos[2]);
        }
    }

    // ─── Chat Cancellation ─────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChat(ServerChatEvent event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (frozenPlayers.contains(event.getPlayer().getUUID())) {
            event.setCanceled(true);
        }
    }

    // ─── Command Filtering ─────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCommand(CommandEvent event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (event.getParseResults().getContext().getSource().getPlayer() == null) return;

        ServerPlayer player = event.getParseResults().getContext().getSource().getPlayer();
        if (!frozenPlayers.contains(player.getUUID())) return;

        String command = event.getParseResults().getReader().getString();
        // Strip leading slash if present
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        String rootCmd = cmd.split(" ")[0].toLowerCase();

        if (!ALLOWED_COMMANDS.contains(rootCmd)) {
            event.setCanceled(true);
            player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.frozen-reminder")));
        }
    }

    // ─── Damage Cancellation ───────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            if (frozenPlayers.contains(player.getUUID())) {
                event.setNewDamage(0);
            }
        }
    }

    // ─── Item Drop Cancellation ────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (event.getPlayer() instanceof ServerPlayer player) {
            if (frozenPlayers.contains(player.getUUID())) {
                event.setCanceled(true);
            }
        }
    }

    // ─── Interaction Cancellation ──────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            if (frozenPlayers.contains(player.getUUID())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            if (frozenPlayers.contains(player.getUUID())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            if (frozenPlayers.contains(player.getUUID())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            if (frozenPlayers.contains(player.getUUID())) {
                event.setCanceled(true);
            }
        }
    }

    // ─── Periodic Reminder & Timeout ───────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (frozenPlayers.isEmpty()) return;

        long now = System.currentTimeMillis();
        int timeoutMs = Settings.getLoginTimeoutSeconds() * 1000;

        for (UUID uuid : Set.copyOf(frozenPlayers)) {
            ServerPlayer player = findPlayer(uuid);
            if (player == null) {
                cleanup(uuid);
                continue;
            }

            // Timeout check
            Long startTime = freezeStartTimes.get(uuid);
            if (startTime != null && timeoutMs > 0 && (now - startTime) >= timeoutMs) {
                Component kickMsg = MessageUtils.format(I18n.get("auth.kicked-timeout", "seconds", String.valueOf(Settings.getLoginTimeoutSeconds())));
                player.connection.disconnect(kickMsg);
                cleanup(uuid);
                continue;
            }

            // Reminder every 200 ticks (10 seconds)
            int ticks = reminderTickCounters.getOrDefault(uuid, 0) + 1;
            if (ticks >= 200) {
                player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.frozen-reminder")));
                ticks = 0;
            }
            reminderTickCounters.put(uuid, ticks);
        }
    }

    private static ServerPlayer findPlayer(UUID uuid) {
        var server = MKTEssentials.getServer();
        if (server == null) return null;
        return server.getPlayerList().getPlayer(uuid);
    }
}
