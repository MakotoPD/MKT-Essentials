package pl.makoto.essentials.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import pl.makoto.essentials.Config;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;

public class TeleportUtils {
    public static void teleport(ServerPlayer player, PlayerData.SavedLocation loc) {
        // Save current location to back stack before teleporting
        PlayerData data = DataManager.getPlayerData(player.getUUID());
        data.pushBackLocation(new PlayerData.SavedLocation(
                player.level().dimension().location().toString(),
                player.position(),
                player.getYRot(),
                player.getXRot()
        ));
        DataManager.savePlayerData(player.getUUID());

        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(loc.dimension));
        ServerLevel targetLevel = player.getServer().getLevel(dimKey);
        
        if (targetLevel != null) {
            // Effects at start position
            if (Config.TELEPORT_EFFECTS.get()) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                ((ServerLevel)player.level()).sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 32, 0.5, 0.5, 0.5, 0.1);
            }

            player.teleportTo(targetLevel, loc.x, loc.y, loc.z, loc.yaw, loc.pitch);
            
            // Effects at end position
            if (Config.TELEPORT_EFFECTS.get()) {
                targetLevel.playSound(null, loc.x, loc.y, loc.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                targetLevel.sendParticles(ParticleTypes.PORTAL, loc.x, loc.y + 1, loc.z, 32, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
}
