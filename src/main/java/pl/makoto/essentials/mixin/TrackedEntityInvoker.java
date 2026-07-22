package pl.makoto.essentials.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Invokers for {@code ChunkMap.TrackedEntity} so we can force a nicked player's entity to be
 * removed and re-spawned for observers — the only way an already-rendered entity picks up the new
 * profile name (and therefore the nickname above the head).
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public interface TrackedEntityInvoker {

    @Invoker("updatePlayer")
    void mktessentials$updatePlayer(ServerPlayer player);

    @Invoker("removePlayer")
    void mktessentials$removePlayer(ServerPlayer player);
}
