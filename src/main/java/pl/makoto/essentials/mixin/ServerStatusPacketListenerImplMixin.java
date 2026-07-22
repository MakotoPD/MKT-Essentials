package pl.makoto.essentials.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.util.FaviconManager;
import pl.makoto.essentials.util.MaintenanceManager;
import pl.makoto.essentials.util.MessageUtils;

import java.util.List;
import java.util.Optional;

/**
 * Overrides the server-list MOTD with the configured {@code motd.lines} when enabled.
 * The MOTD status is built by the vanilla server; this replaces its description just before the
 * {@link net.minecraft.network.protocol.status.ClientboundStatusResponsePacket} is sent.
 * <p>
 * {@code require = 0} keeps a mapping mismatch from bricking startup — if the injection point can't
 * be found the MOTD simply isn't overridden instead of crashing the server.
 */
@Mixin(ServerStatusPacketListenerImpl.class)
public class ServerStatusPacketListenerImplMixin {

    @ModifyArg(
            method = "handleStatusRequest",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/status/ClientboundStatusResponsePacket;<init>(Lnet/minecraft/network/protocol/status/ServerStatus;)V"
            ),
            index = 0,
            require = 0
    )
    private ServerStatus mktessentials$overrideMotd(ServerStatus original) {
        // Maintenance takes priority: automatically show the maintenance MOTD + icon while active.
        if (MaintenanceManager.isActive()) {
            return mktessentials$build(original, Settings.getMaintenanceMotd(), FaviconManager.getMaintenance());
        }

        // Defer to MiniMOTD when it's installed.
        if (pl.makoto.essentials.util.IntegrationsManager.isMotdDeferred()) return original;
        if (!Settings.isMotdEnabled()) return original;
        return mktessentials$build(original, Settings.getMotdLines(), FaviconManager.get());
    }

    private static ServerStatus mktessentials$build(ServerStatus original, List<String> lines,
                                                    Optional<ServerStatus.Favicon> customIcon) {
        Component description = (lines == null || lines.isEmpty())
                ? original.description()
                : MessageUtils.formatBypass(String.join("\n", lines));

        Optional<ServerStatus.Favicon> favicon = customIcon.isPresent() ? customIcon : original.favicon();

        return new ServerStatus(
                description,
                original.players(),
                original.version(),
                favicon,
                original.enforcesSecureChat()
        );
    }
}
