package pl.makoto.essentials.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.MessageUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = MKTEssentials.MODID)
public class TeleportManager {
    /**
     * Teleport category. Each type carries its own cooldown, resolved independently so that, e.g.,
     * using /tpa does not block /rtp. The {@code key} is used to build per-type config and
     * LuckPerms nodes (e.g. {@code mktessentials.teleport_cooldown.tpa}).
     */
    public enum Type {
        DEFAULT("default"),
        HOME("home"),
        TPA("tpa"),
        RTP("rtp"),
        WARP("warp"),
        BACK("back"),
        SPAWN("spawn");

        public final String key;
        Type(String key) { this.key = key; }
    }

    private static final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();
    // Cooldowns are tracked per (player, type) so each teleport category expires on its own clock.
    private static final Map<UUID, Map<Type, Long>> cooldowns = new ConcurrentHashMap<>();

    public static void requestTeleport(ServerPlayer player, PlayerData.SavedLocation loc, boolean ignoreDelay) {
        requestTeleport(player, loc, ignoreDelay, Type.DEFAULT);
    }

    public static void requestTeleport(ServerPlayer player, PlayerData.SavedLocation loc, boolean ignoreDelay, Type type) {
        UUID uuid = player.getUUID();
        boolean bypass = Permissions.hasPermission(player, "mktessentials.teleport.bypass", 2);

        // Check Cooldown for this teleport type
        long now = System.currentTimeMillis();
        Map<Type, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns != null) {
            Long cooldownEnd = playerCooldowns.get(type);
            if (cooldownEnd != null) {
                long remaining = (cooldownEnd - now) / 1000;
                if (remaining <= 0) {
                    playerCooldowns.remove(type);
                } else if (!ignoreDelay && !bypass) {
                    player.sendSystemMessage(MessageUtils.prefixed("&cYou must wait " + remaining + " seconds before teleporting again."));
                    return;
                }
            }
        }

        int delay = (ignoreDelay || bypass) ? 0 : Permissions.getIntPermission(player, "mktessentials.teleport_delay", Settings.getTeleportDelay());

        if (delay <= 0) {
            executeTeleport(player, loc, type);
        } else {
            player.sendSystemMessage(MessageUtils.prefixed("&7Teleporting in &6" + delay + " &7seconds. Don't move!"));

            // Pre-load chunks at destination
            preLoadChunks(player, loc);

            pendingTeleports.put(uuid, new PendingTeleport(loc, player.position(), now + (delay * 1000L), type));
        }
    }

    private static void preLoadChunks(ServerPlayer player, PlayerData.SavedLocation loc) {
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(loc.dimension));
        ServerLevel level = player.getServer().getLevel(dimKey);
        if (level != null) {
            // floor, not (int) cast — casting truncates toward zero and picks the wrong chunk
            // for negative coordinates
            ChunkPos pos = new ChunkPos(net.minecraft.util.Mth.floor(loc.x) >> 4, net.minecraft.util.Mth.floor(loc.z) >> 4);
            // Add a ticket to start loading chunks in the area
            level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.POST_TELEPORT, pos, 2, player.getId());
        }
    }

    private static void executeTeleport(ServerPlayer player, PlayerData.SavedLocation loc, Type type) {
        TeleportUtils.teleport(player, loc);
        player.sendSystemMessage(MessageUtils.prefixed("&aTeleported successfully!"));

        // Set Cooldown for this teleport type
        int cooldown = resolveCooldown(player, type);
        if (cooldown > 0) {
            cooldowns.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
                    .put(type, System.currentTimeMillis() + (cooldown * 1000L));
        }
    }

    /**
     * Resolves the cooldown (seconds) for a teleport type. Resolution order:
     * <ol>
     *   <li>LuckPerms meta {@code mktessentials.teleport_cooldown.<type>}</li>
     *   <li>LuckPerms meta {@code mktessentials.teleport_cooldown} (legacy, applies to all types)</li>
     *   <li>config {@code teleportation.cooldown-<type>}, falling back to {@code teleportation.cooldown}</li>
     * </ol>
     */
    private static int resolveCooldown(ServerPlayer player, Type type) {
        int typed = Permissions.getIntPermission(player, "mktessentials.teleport_cooldown." + type.key, Integer.MIN_VALUE);
        if (typed != Integer.MIN_VALUE) return typed;
        return Permissions.getIntPermission(player, "mktessentials.teleport_cooldown", Settings.getTeleportCooldown(type.key));
    }

    public static void cleanupPlayer(UUID uuid) {
        pendingTeleports.remove(uuid);
        cooldowns.remove(uuid);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, PendingTeleport>> it = pendingTeleports.entrySet().iterator();
        
        while (it.hasNext()) {
            Map.Entry<UUID, PendingTeleport> entry = it.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            
            if (player == null) {
                it.remove();
                continue;
            }

            PendingTeleport pending = entry.getValue();
            
            // Check if moved
            if (player.position().distanceToSqr(pending.startPos) > 0.1) {
                player.sendSystemMessage(MessageUtils.prefixed("&cTeleportation cancelled due to movement."));
                it.remove();
                continue;
            }

            // Check if time is up
            long remaining = (pending.executeAt - now) / 1000;
            if (remaining >= 0 && remaining != pending.lastMessageSeconds) {
                pending.lastMessageSeconds = remaining;
                player.sendSystemMessage(MessageUtils.format("&7Teleporting in &6" + (remaining + 1) + "&7..."), true);
            }

            if (now >= pending.executeAt) {
                executeTeleport(player, pending.targetLoc, pending.type);
                it.remove();
            }
        }
    }

    private static class PendingTeleport {
        final PlayerData.SavedLocation targetLoc;
        final Vec3 startPos;
        final long executeAt;
        final Type type;
        long lastMessageSeconds = -1;

        PendingTeleport(PlayerData.SavedLocation targetLoc, Vec3 startPos, long executeAt, Type type) {
            this.targetLoc = targetLoc;
            this.startPos = startPos;
            this.executeAt = executeAt;
            this.type = type;
        }
    }
}
