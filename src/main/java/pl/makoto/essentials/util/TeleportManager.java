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
    private static final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public static void requestTeleport(ServerPlayer player, PlayerData.SavedLocation loc, boolean ignoreDelay) {
        UUID uuid = player.getUUID();
        boolean bypass = Permissions.hasPermission(player, "mktessentials.teleport.bypass", 2);
        
        // Check Cooldown
        long now = System.currentTimeMillis();
        if (!ignoreDelay && !bypass && cooldowns.containsKey(uuid)) {
            long remaining = (cooldowns.get(uuid) - now) / 1000;
            if (remaining > 0) {
                player.sendSystemMessage(MessageUtils.prefixed("&cYou must wait " + remaining + " seconds before teleporting again."));
                return;
            }
        }

        int delay = (ignoreDelay || bypass) ? 0 : Permissions.getIntPermission(player, "mktessentials.teleport_delay", Settings.getTeleportDelay());
        
        if (delay <= 0) {
            executeTeleport(player, loc);
        } else {
            player.sendSystemMessage(MessageUtils.prefixed("&7Teleporting in &6" + delay + " &7seconds. Don't move!"));
            
            // Pre-load chunks at destination
            preLoadChunks(player, loc);
            
            pendingTeleports.put(uuid, new PendingTeleport(loc, player.position(), now + (delay * 1000L)));
        }
    }

    private static void preLoadChunks(ServerPlayer player, PlayerData.SavedLocation loc) {
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(loc.dimension));
        ServerLevel level = player.getServer().getLevel(dimKey);
        if (level != null) {
            ChunkPos pos = new ChunkPos((int)loc.x >> 4, (int)loc.z >> 4);
            // Add a ticket to start loading chunks in the area
            level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.POST_TELEPORT, pos, 2, player.getId());
        }
    }

    private static void executeTeleport(ServerPlayer player, PlayerData.SavedLocation loc) {
        TeleportUtils.teleport(player, loc);
        player.sendSystemMessage(MessageUtils.prefixed("&aTeleported successfully!"));
        
        // Set Cooldown
        int cooldown = Permissions.getIntPermission(player, "mktessentials.teleport_cooldown", Settings.getTeleportCooldown());
        if (cooldown > 0) {
            cooldowns.put(player.getUUID(), System.currentTimeMillis() + (cooldown * 1000L));
        }
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
                executeTeleport(player, pending.targetLoc);
                it.remove();
            }
        }
    }

    private static class PendingTeleport {
        final PlayerData.SavedLocation targetLoc;
        final Vec3 startPos;
        final long executeAt;
        long lastMessageSeconds = -1;

        PendingTeleport(PlayerData.SavedLocation targetLoc, Vec3 startPos, long executeAt) {
            this.targetLoc = targetLoc;
            this.startPos = startPos;
            this.executeAt = executeAt;
        }
    }
}
