package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import pl.makoto.essentials.Config;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.TeleportUtils;
import pl.makoto.essentials.util.TeleportManager;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.util.Random;
import java.util.List;

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
            source.sendFailure(MessageUtils.prefixed("&cNo back location found!"));
            return 0;
        }

        TeleportManager.requestTeleport(player, loc, false);
        return 1;
    }

    private static int rtp(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ServerLevel level = (ServerLevel) player.level();
        int min = Config.RTP_MIN_DISTANCE.get();
        int max = Config.RTP_MAX_DISTANCE.get();
        
        double centerX = Config.RTP_RELATIVE.get() ? player.getX() : Config.RTP_CENTER_X.get();
        double centerZ = Config.RTP_RELATIVE.get() ? player.getZ() : Config.RTP_CENTER_Z.get();

        List<? extends String> blacklist = Config.RTP_BIOME_BLACKLIST.get();
        BlockPos finalPos = null;

        for (int i = 0; i < 50; i++) {
            double x = centerX + (RANDOM.nextBoolean() ? 1 : -1) * (min + RANDOM.nextInt(max - min));
            double z = centerZ + (RANDOM.nextBoolean() ? 1 : -1) * (min + RANDOM.nextInt(max - min));
            
            level.getChunkSource().getChunk((int)x >> 4, (int)z >> 4, ChunkStatus.FULL, true);
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));
            
            if (pos.getY() <= level.getMinBuildHeight()) {
                pos = new BlockPos((int)x, level.getLogicalHeight(), (int)z);
            }

            // Check biome at a safe height (surface)
            String biomeId = level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("unknown");
            
            String finalBiomeId = biomeId;
            boolean isBlacklisted = blacklist.stream().anyMatch(s -> s.trim().equalsIgnoreCase(finalBiomeId));
            
            if (!isBlacklisted) {
                finalPos = pos;
                break;
            } else {
                MKTEssentials.LOGGER.info("RTP: Skipping blacklisted biome: {}", biomeId);
            }
        }

        if (finalPos == null) {
            source.sendFailure(MessageUtils.prefixed("&cCould not find a safe location after 50 attempts. Please try again."));
            return 0;
        }

        TeleportManager.requestTeleport(player, new PlayerData.SavedLocation(
                level.dimension().location().toString(),
                new Vec3(finalPos.getX() + 0.5, finalPos.getY() + 1, finalPos.getZ() + 0.5),
                player.getYRot(), player.getXRot()
        ), false);
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
