package pl.makoto.essentials.integration;

import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.util.IntegrationsManager;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.event.connection.UdpPacketReceivedEvent;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;

import java.util.UUID;

/**
 * Plasmo Voice mute-sync: a player muted in MKT can't speak in voice chat either. Modeled on
 * FlectonePulse's Plasmo integration — we cancel incoming audio packets from muted players.
 *
 * <p>Unlike Simple Voice Chat (annotation auto-scan), Plasmo addons are registered explicitly via
 * {@link PlasmoVoiceServer#getAddonsLoader()}. {@link #register()} is called only behind an
 * {@code isLoaded("plasmovoice")} guard in {@link IntegrationsManager}, so this class and the
 * {@code su.plo.voice} API it references are never loaded when Plasmo Voice is absent.
 */
@Addon(id = "mktessentials", scope = AddonLoaderScope.SERVER, version = "1.0", authors = {"MakotoPD"})
public final class PlasmoVoicePlugin implements AddonInitializer {

    /** Instantiates and registers the addon with Plasmo's server addon loader. */
    public static void register() {
        try {
            PlasmoVoiceServer.getAddonsLoader().load(new PlasmoVoicePlugin());
        } catch (Throwable t) {
            MKTEssentials.LOGGER.warn("[Integration] Failed to register Plasmo Voice addon: {}", t.toString());
        }
    }

    @EventSubscribe
    public void onPlayerSpeak(UdpPacketReceivedEvent event) {
        if (!IntegrationsManager.isPlasmoVoiceMuteSyncActive()) return;
        if (!(event.getPacket() instanceof PlayerAudioPacket)) return;

        UUID senderUuid = event.getConnection().getPlayer().getInstance().getUuid();
        if (IntegrationsManager.shouldSilenceVoice(senderUuid)) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onAddonInitialize() {
        MKTEssentials.LOGGER.info("[Integration] Plasmo Voice mute-sync addon initialized.");
    }
}
