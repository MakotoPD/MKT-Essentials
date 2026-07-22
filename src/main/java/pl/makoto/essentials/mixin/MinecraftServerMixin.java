package pl.makoto.essentials.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.util.FaviconManager;
import pl.makoto.essentials.util.MaintenanceManager;
import pl.makoto.essentials.util.MessageUtils;

import java.util.List;
import java.util.Optional;

/**
 * Overrides the server-list status (MOTD + icon) at the point it is fetched for a ping:
 * {@code MinecraftServer.getStatus()}. This is more reliable than modifying the status packet,
 * since the status listener is created from this value for every ping connection. Maintenance mode
 * takes priority; otherwise the configured {@code motd} is used when enabled.
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "getStatus", at = @At("RETURN"), cancellable = true, require = 0)
    private void mktessentials$overrideStatus(CallbackInfoReturnable<ServerStatus> cir) {
        ServerStatus original = cir.getReturnValue();
        if (original == null) return;

        boolean maintenance = MaintenanceManager.isActive();
        // Defer to MiniMOTD when it's installed (unless we're forcing the maintenance MOTD).
        if (!maintenance && pl.makoto.essentials.util.IntegrationsManager.isMotdDeferred()) return;
        if (!maintenance && !Settings.isMotdEnabled()) return;

        List<String> lines = maintenance ? Settings.getMaintenanceMotd() : Settings.getMotdLines();
        Component description = (lines == null || lines.isEmpty())
                ? original.description()
                : MessageUtils.formatBypass(String.join("\n", lines));

        Optional<ServerStatus.Favicon> customIcon = maintenance ? FaviconManager.getMaintenance() : FaviconManager.get();
        Optional<ServerStatus.Favicon> favicon = customIcon.isPresent() ? customIcon : original.favicon();

        cir.setReturnValue(new ServerStatus(
                description,
                original.players(),
                original.version(),
                favicon,
                original.enforcesSecureChat()
        ));
    }
}
