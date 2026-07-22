package pl.makoto.essentials.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.makoto.essentials.config.Settings;

/**
 * Suppresses vanilla's "X joined the game" broadcast. Vanilla sends it directly inside
 * {@code placeNewPlayer} with no NeoForge event to cancel it, and the listener event
 * ({@code firePlayerLoggedIn}) only fires afterwards — so without this the mod's custom join
 * message would appear as a second line. When custom join/quit messages are disabled we call
 * the original broadcast through unchanged, preserving vanilla behaviour.
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Redirect(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
            )
    )
    private void mktessentials$suppressVanillaJoinMessage(PlayerList instance, Component message, boolean bypassHiddenChat) {
        if (!Settings.isJoinQuitEnabled()) {
            instance.broadcastSystemMessage(message, bypassHiddenChat);
        }
    }
}
