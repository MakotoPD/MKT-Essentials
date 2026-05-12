package pl.makoto.essentials.config;

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
