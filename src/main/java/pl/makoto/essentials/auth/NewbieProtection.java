package pl.makoto.essentials.auth;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.util.MessageUtils;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = MKTEssentials.MODID)
public final class NewbieProtection {

    private static final Map<UUID, Long> protectionEndTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> pausedAt = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    private NewbieProtection() {}

    public static void activate(ServerPlayer player, int durationMinutes) {
        if (durationMinutes <= 0) return;
        UUID uuid = player.getUUID();
        long endTime = System.currentTimeMillis() + (durationMinutes * 60_000L);
        protectionEndTimes.put(uuid, endTime);
        player.sendSystemMessage(MessageUtils.prefixed(
                I18n.get("auth.newbie-protection", "minutes", String.valueOf(durationMinutes))));
    }

    public static void deactivate(ServerPlayer player) {
        UUID uuid = player.getUUID();
        protectionEndTimes.remove(uuid);
        pausedAt.remove(uuid);
    }

    public static boolean isProtected(UUID uuid) {
        Long endTime = protectionEndTimes.get(uuid);
        if (endTime == null) return false;
        // If paused, still considered protected (timer frozen)
        if (pausedAt.containsKey(uuid)) return true;
        return System.currentTimeMillis() < endTime;
    }

    public static void pauseTimer(UUID uuid) {
        if (protectionEndTimes.containsKey(uuid) && !pausedAt.containsKey(uuid)) {
            pausedAt.put(uuid, System.currentTimeMillis());
        }
    }

    public static void resumeTimer(UUID uuid) {
        Long pauseStart = pausedAt.remove(uuid);
        if (pauseStart != null) {
            Long endTime = protectionEndTimes.get(uuid);
            if (endTime != null) {
                long pausedDuration = System.currentTimeMillis() - pauseStart;
                protectionEndTimes.put(uuid, endTime + pausedDuration);
            }
        }
    }

    public static void cleanup(UUID uuid) {
        protectionEndTimes.remove(uuid);
        pausedAt.remove(uuid);
    }

    // ─── Damage Cancellation (after FreezeManager) ─────────────────────────────

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            if (isProtected(player.getUUID())) {
                event.setNewDamage(0);
            }
        }
    }

    // ─── PvP Detection — attacker loses protection ─────────────────────────────

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (!(event.getEntity() instanceof ServerPlayer attacker)) return;
        if (!(event.getTarget() instanceof ServerPlayer)) return;

        if (isProtected(attacker.getUUID())) {
            deactivate(attacker);
        }
    }

    // ─── Action Bar & Expiry Check ─────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (protectionEndTimes.isEmpty()) return;

        tickCounter++;
        long now = System.currentTimeMillis();

        for (UUID uuid : Set.copyOf(protectionEndTimes.keySet())) {
            Long endTime = protectionEndTimes.get(uuid);
            if (endTime == null) continue;

            // Skip paused players for expiry check
            if (pausedAt.containsKey(uuid)) continue;

            ServerPlayer player = findPlayer(uuid);
            if (player == null) continue;

            // Expiry check
            if (now >= endTime) {
                deactivate(player);
                continue;
            }

            // Action bar every 20 ticks (1 second)
            if (tickCounter % 20 == 0) {
                long remainingMs = endTime - now;
                long remainingMinutes = (remainingMs / 60_000) + 1;
                Component actionBar = MessageUtils.format(
                        I18n.get("auth.newbie-protection", "minutes", String.valueOf(remainingMinutes)));
                player.sendSystemMessage(actionBar, true);
            }
        }
    }

    private static ServerPlayer findPlayer(UUID uuid) {
        var server = MKTEssentials.getServer();
        if (server == null) return null;
        return server.getPlayerList().getPlayer(uuid);
    }
}
