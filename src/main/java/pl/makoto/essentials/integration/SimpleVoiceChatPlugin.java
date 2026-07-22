package pl.makoto.essentials.integration;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.util.IntegrationsManager;

/**
 * Simple Voice Chat mute-sync: a player muted in MKT can't speak in voice chat either. Modeled on
 * FlectonePulse's Simple Voice integration — we cancel the microphone packet of muted players.
 *
 * <p>Registration is automatic: on NeoForge, Simple Voice Chat scans mods for classes annotated
 * with {@link ForgeVoicechatPlugin} that implement {@link VoicechatPlugin} and instantiates them.
 * Because that scan only runs when Simple Voice Chat is installed, this class (and the voicechat API
 * it references) is never loaded when the mod is absent — a safe no-op. The {@code voicechat-api}
 * dependency is {@code compileOnly} for the same reason.
 */
@ForgeVoicechatPlugin
public class SimpleVoiceChatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return "mktessentials";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        if (!IntegrationsManager.isSimpleVoiceMuteSyncActive()) return;
        if (event.isCancelled() || event.getSenderConnection() == null) return;

        Player sender = event.getSenderConnection().getPlayer();
        if (sender == null) return;

        if (IntegrationsManager.shouldSilenceVoice(sender.getUuid())) {
            event.cancel();
        }
    }
}
