package pl.makoto.essentials.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.util.MessageUtils;

import java.util.List;

/**
 * Suppresses vanilla's "X left the game" broadcast (see {@code removePlayerFromWorld}) and applies
 * permission-filtered color/format codes to sign and written-book text. Sign/book text is colored by
 * rewriting the incoming {@link FilteredText} strings to section codes before the vanilla handler
 * turns them into components.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Redirect(
            method = "removePlayerFromWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
            )
    )
    private void mktessentials$suppressVanillaLeaveMessage(PlayerList instance, Component message, boolean bypassHiddenChat) {
        if (!Settings.isJoinQuitEnabled()) {
            instance.broadcastSystemMessage(message, bypassHiddenChat);
        }
    }

    @ModifyVariable(method = "updateSignText", at = @At("HEAD"), argsOnly = true, require = 0)
    private List<FilteredText> mktessentials$colorSign(List<FilteredText> lines) {
        if (lines == null || !Settings.isSignColorEnabled()) return lines;
        return lines.stream()
                .map(line -> new FilteredText(MessageUtils.legacyColors(this.player, line.raw()), line.mask()))
                .toList();
    }

    @ModifyVariable(method = "signBook", at = @At("HEAD"), argsOnly = true, require = 0)
    private List<FilteredText> mktessentials$colorBook(List<FilteredText> pages) {
        if (pages == null || !Settings.isBookColorEnabled()) return pages;
        return pages.stream()
                .map(page -> new FilteredText(MessageUtils.legacyColors(this.player, page.raw()), page.mask()))
                .toList();
    }
}
