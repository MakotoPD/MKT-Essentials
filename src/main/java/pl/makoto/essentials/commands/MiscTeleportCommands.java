package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.TeleportUtils;
import pl.makoto.essentials.util.TeleportManager;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.config.I18n;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MiscTeleportCommands {
    private static final Random RANDOM = new Random();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.spawn", 0))
                .executes(context -> spawn(context.getSource())));

        dispatcher.register(Commands.literal("back")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.back", 0))
                .executes(context -> back(context.getSource())));

        dispatcher.register(Commands.literal("rtp")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.rtp", 0))
                .executes(context -> rtp(context.getSource())));

        dispatcher.register(Commands.literal("top")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.top", 0))
                .executes(context -> top(context.getSource())));
    }

    private static int spawn(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ServerLevel level = player.getServer().overworld();
        BlockPos spawnPos = level.getSharedSpawnPos();
        
        TeleportManager.requestTeleport(player, new PlayerData.SavedLocation(
                level.dimension().location().toString(),
                new Vec3(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5),
                0, 0
        ), false);
        return 1;
    }

    private static int back(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerData data = DataManager.getPlayerData(player.getUUID());
        PlayerData.SavedLocation loc = data.popBackLocation();
        if (loc == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("back.no-location")));
            return 0;
        }

        TeleportManager.requestTeleport(player, loc, false);
        return 1;
    }

    private static int rtp(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ServerLevel level = (ServerLevel) player.level();
        MinecraftServer server = player.getServer();
        UUID playerUuid = player.getUUID();
        String dimensionId = level.dimension().location().toString();

        int min = Settings.getRtpMinDistance();
        int max = Settings.getRtpMaxDistance();

        double centerX = Settings.isRtpRelative() ? player.getX() : Settings.getRtpCenterX();
        double centerZ = Settings.isRtpRelative() ? player.getZ() : Settings.getRtpCenterZ();

        List<? extends String> blacklist = Settings.getRtpBiomeBlacklist();

        // Send "Searching..." action-bar message before starting async work
        player.sendSystemMessage(MessageUtils.prefixed(I18n.get("rtp.searching")), true);

        // Offload candidate generation to async, then validate on main thread
        CompletableFuture.supplyAsync(() -> {
            // Async: generate random X/Z candidates (no chunk loading here)
            double[][] candidates = new double[50][2];
            for (int i = 0; i < 50; i++) {
                candidates[i][0] = centerX + (RANDOM.nextBoolean() ? 1 : -1) * (min + RANDOM.nextInt(max - min));
                candidates[i][1] = centerZ + (RANDOM.nextBoolean() ? 1 : -1) * (min + RANDOM.nextInt(max - min));
            }
            return candidates;
        }).thenAccept(candidates -> {
            // Schedule validation back to main thread (chunk loading + biome check must be on main thread)
            server.tell(new TickTask(server.getTickCount() + 1, () -> {
                ServerPlayer p = server.getPlayerList().getPlayer(playerUuid);
                if (p == null) return; // Player disconnected during search

                ServerLevel targetLevel = (ServerLevel) p.level();

                for (double[] candidate : candidates) {
                    double x = candidate[0];
                    double z = candidate[1];

                    // Load chunk on main thread (thread-safe here)
                    targetLevel.getChunkSource().getChunk((int) x >> 4, (int) z >> 4, ChunkStatus.FULL, true);

                    // Get surface position via heightmap
                    BlockPos pos = targetLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));

                    // Skip if no valid surface found (void, below min build height)
                    if (pos.getY() <= targetLevel.getMinBuildHeight()) {
                        continue;
                    }

                    // Check biome at the ACTUAL surface position (not at y=64)
                    String biomeId = targetLevel.getBiome(pos).unwrapKey()
                            .map(key -> key.location().toString()).orElse("unknown");

                    boolean isBlacklisted = blacklist.stream().anyMatch(s -> s.trim().equalsIgnoreCase(biomeId));
                    if (isBlacklisted) {
                        continue;
                    }

                    // Valid location found — teleport
                    TeleportManager.requestTeleport(p, new PlayerData.SavedLocation(
                            dimensionId,
                            new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5),
                            p.getYRot(), p.getXRot()
                    ), false);
                    return;
                }

                // No valid location found after all candidates
                p.sendSystemMessage(MessageUtils.prefixed(I18n.get("rtp.failed")));
            }));
        });

        return 1;
    }

    private static int top(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, player.blockPosition());
        
        TeleportManager.requestTeleport(player, new PlayerData.SavedLocation(
                level.dimension().location().toString(),
                new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5),
                player.getYRot(), player.getXRot()
        ), false);
        return 1;
    }
}
