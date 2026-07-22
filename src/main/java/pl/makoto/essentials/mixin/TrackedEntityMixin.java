package pl.makoto.essentials.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.makoto.essentials.util.PhantomIsolation;

/**
 * Enforces "full isolation" phantom shadowban at the entity-tracking layer: whenever the server
 * would start showing an entity to a player, we cancel it if the entity is a player and either side
 * is a phantom. That keeps the phantom's world empty of other players and keeps the phantom invisible
 * to everyone — across movement, dimension changes and joins — without per-tick bookkeeping. Only
 * player entities are affected; mobs/items track normally.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin {

    @Shadow @Final private Entity entity;

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void mktessentials$phantomIsolation(ServerPlayer viewer, CallbackInfo ci) {
        if (entity instanceof ServerPlayer tracked && PhantomIsolation.hidden(viewer, tracked)) {
            ci.cancel();
        }
    }
}
