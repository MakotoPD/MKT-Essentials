package pl.makoto.essentials.util;

import net.neoforged.fml.ModList;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;

import java.util.UUID;

/**
 * Optional hooks into other mods, mirroring FlectonePulse's integration modules. Every hook is
 * gated on the target mod actually being installed (checked once, on server start) AND on its
 * per-mod toggle in {@code integration.yml}. When a mod is absent every method degrades to a safe
 * no-op / default, so MKT stays fully self-sufficient.
 *
 * <ul>
 *   <li><b>TAB</b> — if present, MKT's native tablist/nametag/ping back off ({@link #isTabDeferred()}).</li>
 *   <li><b>MiniMOTD</b> — if present, MKT's MOTD override backs off ({@link #isMotdDeferred()}).</li>
 *   <li><b>SkinsRestorer</b> — supplies a player's skin texture for the greeting face
 *       ({@link #getSkinTextureUrl}), via reflection so no compile-time dependency is needed.</li>
 *   <li><b>PlasmoVoice / Simple Voice Chat</b> — detected so a player muted in MKT can also be
 *       muted in voice chat ({@link #shouldMuteVoice}). Enforcement is wired by the voice mod's
 *       own event pipeline when present.</li>
 * </ul>
 */
public final class IntegrationsManager {

    private IntegrationsManager() {}

    private static boolean tab;
    private static boolean minimotd;
    private static boolean skinsRestorer;
    private static boolean plasmoVoice;
    private static boolean simpleVoice;

    /** Detects installed mods. Call once the mod list is available (server starting). */
    public static void init() {
        ModList mods = ModList.get();
        tab = mods.isLoaded("tab");
        minimotd = mods.isLoaded("minimotd") || mods.isLoaded("minimotd-neoforge") || mods.isLoaded("minimotd-fabric");
        skinsRestorer = mods.isLoaded("skinsrestorer");
        simpleVoice = mods.isLoaded("voicechat");
        plasmoVoice = mods.isLoaded("plasmovoice")
                && hasClass("su.plo.voice.api.server.event.audio.source.ServerSourceCreatedEvent");

        logHook("TAB", tab, Settings.isIntegrationTab());
        logHook("MiniMOTD", minimotd, Settings.isIntegrationMinimotd());
        logHook("SkinsRestorer", skinsRestorer, Settings.isIntegrationSkinsRestorer());
        logHook("PlasmoVoice", plasmoVoice, Settings.isIntegrationPlasmoVoice());
        logHook("SimpleVoiceChat", simpleVoice, Settings.isIntegrationSimpleVoice());

        // Simple Voice Chat auto-discovers our @ForgeVoicechatPlugin; Plasmo needs explicit
        // registration. Guarded so the su.plo.voice classes only load when the mod is present.
        if (plasmoVoice && Settings.isIntegrationPlasmoVoice()) {
            pl.makoto.essentials.integration.PlasmoVoicePlugin.register();
        }
    }

    private static void logHook(String name, boolean present, boolean enabled) {
        if (!present) return;
        MKTEssentials.LOGGER.info("[Integration] {} detected — hook {}.", name, enabled ? "enabled" : "disabled in config");
    }

    // --- Presence (mod installed) --------------------------------------------------------------
    public static boolean isTabPresent() { return tab; }
    public static boolean isMiniMotdPresent() { return minimotd; }
    public static boolean isSkinsRestorerPresent() { return skinsRestorer; }
    public static boolean isPlasmoVoicePresent() { return plasmoVoice; }
    public static boolean isSimpleVoicePresent() { return simpleVoice; }

    // --- Active hooks (installed AND enabled in config) -----------------------------------------

    /** True when TAB should manage the player list/nametags instead of MKT's native ones. */
    public static boolean isTabDeferred() {
        return tab && Settings.isIntegrationTab();
    }

    /** True when MiniMOTD should own the server-list MOTD instead of MKT's override. */
    public static boolean isMotdDeferred() {
        return minimotd && Settings.isIntegrationMinimotd();
    }

    /** Whether Simple Voice Chat mic packets from muted players should be dropped. */
    public static boolean isSimpleVoiceMuteSyncActive() {
        return simpleVoice && Settings.isIntegrationSimpleVoice();
    }

    /** Whether Plasmo Voice audio packets from muted players should be dropped. */
    public static boolean isPlasmoVoiceMuteSyncActive() {
        return plasmoVoice && Settings.isIntegrationPlasmoVoice();
    }

    /** True when the player is currently muted in MKT (temporary not expired, or permanent). */
    private static boolean isMutedInMkt(UUID playerId) {
        if (playerId == null) return false;
        long expiration = pl.makoto.essentials.data.DataManager.getPlayerData(playerId).getMuteExpiration();
        return expiration == -1 || expiration > System.currentTimeMillis();
    }

    /**
     * Whether a player's voice should be silently dropped: either they're muted in MKT, or they're a
     * "phantom" shadowban player (they can join and hear/speak into the void, but nobody hears them).
     * The silencing is invisible to the player — no error, the mic packet is simply discarded.
     */
    public static boolean shouldSilenceVoice(UUID playerId) {
        return isMutedInMkt(playerId) || ShadowBanManager.isPhantom(playerId);
    }

    /**
     * The full-skin texture URL for the player's SkinsRestorer skin, or {@code null} when the hook
     * is inactive or the player has no custom skin. Resolved reflectively against the SkinsRestorer
     * API ({@code net.skinsrestorer.api.*}) so MKT never hard-links the mod.
     */
    public static String getSkinTextureUrl(UUID uuid, String name) {
        if (!skinsRestorer || !Settings.isIntegrationSkinsRestorer() || uuid == null) return null;
        try {
            Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
            Object api = providerClass.getMethod("get").invoke(null);

            Object playerStorage = api.getClass().getMethod("getPlayerStorage").invoke(api);
            Object optional = playerStorage.getClass()
                    .getMethod("getSkinForPlayer", UUID.class, String.class)
                    .invoke(playerStorage, uuid, name == null ? "" : name);
            Object skinProperty = ((java.util.Optional<?>) optional).orElse(null);
            if (skinProperty == null) return null;

            String value = (String) skinProperty.getClass().getMethod("getValue").invoke(skinProperty);
            return textureUrlFromProperty(value);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Decodes the base64 texture property and extracts the {@code SKIN.url}. */
    private static String textureUrlFromProperty(String base64Value) {
        try {
            String json = new String(java.util.Base64.getDecoder().decode(base64Value),
                    java.nio.charset.StandardCharsets.UTF_8);
            int urlIdx = json.indexOf("\"url\"");
            if (urlIdx < 0) return null;
            int start = json.indexOf('"', json.indexOf(':', urlIdx) + 1) + 1;
            int end = json.indexOf('"', start);
            if (start <= 0 || end < 0) return null;
            return json.substring(start, end).replace("\\/", "/");
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name, false, IntegrationsManager.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
