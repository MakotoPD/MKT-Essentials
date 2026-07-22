package pl.makoto.essentials.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.makoto.essentials.util.NicknameService;

/**
 * Makes nicknames show in the tab list AND above the head. The per-player entry of
 * {@code ClientboundPlayerInfoUpdatePacket} is built from the {@link ServerPlayer}; here we swap:
 * <ul>
 *   <li>{@code getGameProfile()} → a nick-named profile (keeps skin) → above-head name;</li>
 *   <li>{@code getTabListDisplayName()} → the colored full name → tab list.</li>
 * </ul>
 * Non-nicked players get the original values. {@code require = 0} so a mapping mismatch can't brick
 * startup — nicknames just wouldn't be rewritten.
 */
@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry")
public class PlayerInfoEntryMixin {

    @Redirect(
            method = "<init>(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;getGameProfile()Lcom/mojang/authlib/GameProfile;"
            ),
            require = 0
    )
    private static GameProfile mktessentials$nickProfile(ServerPlayer player) {
        return NicknameService.profile(player);
    }

    @Redirect(
            method = "<init>(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;getTabListDisplayName()Lnet/minecraft/network/chat/Component;"
            ),
            require = 0
    )
    private static Component mktessentials$nickDisplayName(ServerPlayer player) {
        return NicknameService.displayName(player);
    }
}
