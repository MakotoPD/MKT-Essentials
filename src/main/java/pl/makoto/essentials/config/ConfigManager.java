package pl.makoto.essentials.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import pl.makoto.essentials.MKTEssentials;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ConfigManager {
    private static Path configDir;
    private static Path langDir;
    private static boolean initialized = false;

    public static boolean isInitialized() { return initialized; }

    public static void init() {
        configDir = Path.of("config", "mktessentials");
        langDir = configDir.resolve("lang");
        ensureDirectories();
        writeDefaultIfMissing(configDir.resolve("settings.yml"), DefaultTemplates.SETTINGS_YML);
        writeDefaultIfMissing(configDir.resolve("commands.yml"), DefaultTemplates.COMMANDS_YML);
        writeDefaultIfMissing(configDir.resolve("messages.yml"), DefaultTemplates.MESSAGES_YML);
        // dopisz brakujące klucze do istniejącego settings.yml (po aktualizacji moda),
        // zachowując wartości i własne sekcje użytkownika
        mergeMissingKeys(configDir.resolve("settings.yml"), DefaultTemplates.SETTINGS_YML);
        writeDefaultIfMissing(langDir.resolve("en_us.yml"), DefaultTemplates.LANG_EN_US);
        writeDefaultIfMissing(langDir.resolve("pl_pl.yml"), DefaultTemplates.LANG_PL_PL);
        loadAll();
        initialized = true;
    }

    public static boolean reload() {
        try {
            loadAll();
            return true;
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to reload configuration", e);
            return false;
        }
    }

    private static void loadAll() {
        Map<String, Object> settings = parseYaml(configDir.resolve("settings.yml"));
        Map<String, Object> commands = parseYaml(configDir.resolve("commands.yml"));
        Map<String, Object> messages = parseYaml(configDir.resolve("messages.yml"));

        Settings.loadSettings(settings != null ? settings : Map.of());
        Settings.loadCommands(commands != null ? commands : Map.of());
        Settings.loadMessages(messages != null ? messages : Map.of());
        Settings.loadAuth(settings != null ? settings : Map.of());

        I18n.init(Settings.getLanguage());
    }

    private static void ensureDirectories() {
        try {
            Files.createDirectories(configDir);
            Files.createDirectories(langDir);
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to create config directories", e);
        }
    }

    private static void writeDefaultIfMissing(Path file, String content) {
        if (!Files.exists(file)) {
            try {
                Files.writeString(file, content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                MKTEssentials.LOGGER.error("Failed to write default config: {}", file, e);
            }
        }
    }

    /**
     * Dopisuje do istniejącego pliku klucze obecne w szablonie, a brakujące w pliku
     * użytkownika (rekurencyjnie). Istniejące wartości i własne sekcje użytkownika
     * zostają nietknięte. Zapisuje tylko gdy coś dodano.
     * UWAGA: zapis przez snakeyaml nie zachowuje komentarzy — przy aktualizacji
     * dodającej klucze plik traci komentarze (klucze i wartości pozostają).
     */
    private static void mergeMissingKeys(Path file, String templateContent) {
        if (!Files.exists(file)) return;
        Map<String, Object> user = parseYaml(file);
        if (user == null) return; // nie nadpisujemy uszkodzonego pliku
        Object tpl;
        try {
            tpl = new Yaml().load(templateContent);
        } catch (Exception e) {
            return;
        }
        if (!(tpl instanceof Map<?, ?> templateMap)) return;

        Map<String, Object> merged = new LinkedHashMap<>(user);
        @SuppressWarnings("unchecked")
        boolean changed = deepMergeMissing(merged, (Map<String, Object>) templateMap);
        if (!changed) return;

        try {
            DumperOptions opts = new DumperOptions();
            opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            opts.setPrettyFlow(true);
            opts.setIndent(2);
            String out = new Yaml(opts).dump(merged);
            Files.writeString(file, out, StandardCharsets.UTF_8);
            MKTEssentials.LOGGER.info("Uzupełniono brakujące klucze w {} (wartości zachowane).", file.getFileName());
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Nie udało się zapisać scalonego configu: {}", file, e);
        }
    }

    /** Dodaje brakujące klucze z defaults do user (rekurencyjnie po mapach). Zwraca true gdy coś dodano. */
    @SuppressWarnings("unchecked")
    private static boolean deepMergeMissing(Map<String, Object> user, Map<String, Object> defaults) {
        boolean changed = false;
        for (Map.Entry<String, Object> e : defaults.entrySet()) {
            String key = e.getKey();
            Object def = e.getValue();
            if (!user.containsKey(key)) {
                user.put(key, def);
                changed = true;
            } else if (user.get(key) instanceof Map<?, ?> uSub && def instanceof Map<?, ?> dSub) {
                // scalaj tylko gdy oba są mapami; w razie potrzeby zamień na mutowalną
                Map<String, Object> mutable = new LinkedHashMap<>((Map<String, Object>) uSub);
                if (deepMergeMissing(mutable, (Map<String, Object>) dSub)) {
                    user.put(key, mutable);
                    changed = true;
                }
            }
            // w przeciwnym razie: użytkownik ma wartość (skalar/lista) → zostawiamy bez zmian
        }
        return changed;
    }

    static Map<String, Object> parseYaml(Path file) {
        if (!Files.exists(file)) return null;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Yaml yaml = new Yaml();
            Object result = yaml.load(reader);
            if (result instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedMap = (Map<String, Object>) map;
                return typedMap;
            }
            return Map.of();
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to parse YAML file: {}", file, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T getNestedValue(Map<String, Object> map, String path, T defaultValue) {
        if (map == null) return defaultValue;
        String[] parts = path.split("\\.");
        Object current = map;
        for (String part : parts) {
            if (current instanceof Map<?, ?> m) {
                current = m.get(part);
            } else {
                return defaultValue;
            }
        }
        if (current == null) return defaultValue;
        try {
            if (defaultValue instanceof Integer && current instanceof Number n) {
                return (T) Integer.valueOf(n.intValue());
            }
            if (defaultValue instanceof Double && current instanceof Number n) {
                return (T) Double.valueOf(n.doubleValue());
            }
            if (defaultValue instanceof Boolean && current instanceof Boolean) {
                return (T) current;
            }
            if (defaultValue instanceof String && current instanceof String) {
                return (T) current;
            }
            if (defaultValue instanceof List<?> && current instanceof List<?>) {
                return (T) current;
            }
            return (T) current;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    static Path getLangDir() {
        return langDir;
    }
}
