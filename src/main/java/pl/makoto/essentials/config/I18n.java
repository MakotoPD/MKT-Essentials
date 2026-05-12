package pl.makoto.essentials.config;

import pl.makoto.essentials.MKTEssentials;

import java.nio.file.Path;
import java.util.*;

public final class I18n {
    private static Map<String, String> activeMessages = Map.of();
    private static Map<String, String> fallbackMessages = Map.of();

    private I18n() {}

    public static void init(String language) {
        Path langDir = ConfigManager.getLangDir();
        // Always load en_us as fallback
        Map<String, Object> enMap = ConfigManager.parseYaml(langDir.resolve("en_us.yml"));
        fallbackMessages = enMap != null ? flatten(enMap) : Map.of();

        if ("en_us".equals(language)) {
            activeMessages = fallbackMessages;
        } else {
            Path langFile = langDir.resolve(language + ".yml");
            Map<String, Object> langMap = ConfigManager.parseYaml(langFile);
            if (langMap != null) {
                activeMessages = flatten(langMap);
            } else {
                MKTEssentials.LOGGER.warn("Language file '{}' not found, falling back to en_us", language);
                activeMessages = fallbackMessages;
            }
        }
    }

    public static void reload() {
        init(Settings.getLanguage());
    }

    public static String get(String key, Object... args) {
        String template = activeMessages.getOrDefault(key, fallbackMessages.getOrDefault(key, key));
        if (args.length == 0) return template;
        // Named placeholder substitution: args are pairs (key, value)
        String result = template;
        for (int i = 0; i < args.length - 1; i += 2) {
            String placeholder = "{" + args[i].toString() + "}";
            String value = args[i + 1] != null ? args[i + 1].toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    private static Map<String, String> flatten(Map<String, Object> map) {
        Map<String, String> result = new HashMap<>();
        flattenRecursive(map, "", result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void flattenRecursive(Map<String, Object> map, String prefix, Map<String, String> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                flattenRecursive((Map<String, Object>) nested, key, result);
            } else if (value != null) {
                result.put(key, value.toString());
            }
        }
    }
}
