package pl.makoto.essentials.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import pl.makoto.essentials.MKTEssentials;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Persistent storage of exact item copies, grouped into categories and optionally weighted
 * for random draws (crates, daily rewards, event prizes).
 * <p>
 * Items are stored as SNBT via {@link ItemStack#save}, the same round-trip kits use, so every
 * component survives: display name, lore, enchantments, attributes, custom model data and any
 * mod-added data.
 */
public class ItemStorageManager {

    /** A chest GUI holds 54 slots, which is also the practical cap on a category. */
    public static final int MAX_ITEMS = 54;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Random RANDOM = new Random();

    private static Path storageFile;
    private static Map<String, Category> categories = new LinkedHashMap<>();

    public static class StoredItem {
        public String snbt;
        /** Relative chance in a random draw; 1 = default, 0 = never drawn. */
        public int weight = 1;

        @SuppressWarnings("unused")
        private StoredItem() {} // Gson

        StoredItem(String snbt, int weight) {
            this.snbt = snbt;
            this.weight = weight;
        }
    }

    public static class Category {
        public String name;
        /** Whether handing out an item from this category is broadcast to everyone. */
        public boolean announce;
        public List<StoredItem> items = new ArrayList<>();

        @SuppressWarnings("unused")
        private Category() {} // Gson

        Category(String name) {
            this.name = name;
        }

        public int size() {
            return items.size();
        }
    }

    public static void init(MinecraftServer server) {
        storageFile = server.getWorldPath(LevelResource.ROOT).resolve("mktessentials").resolve("items.json");
        load();
    }

    // ── Categories ───────────────────────────────────────────────────────────

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static Category get(String name) {
        return categories.get(key(name));
    }

    public static List<String> getCategoryNames() {
        return categories.values().stream().map(c -> c.name).toList();
    }

    public static List<Category> getCategories() {
        return new ArrayList<>(categories.values());
    }

    /** @return the new category, or null if one with that name already exists */
    public static Category create(String name) {
        if (categories.containsKey(key(name))) return null;
        Category category = new Category(name);
        categories.put(key(name), category);
        save();
        return category;
    }

    public static boolean delete(String name) {
        boolean removed = categories.remove(key(name)) != null;
        if (removed) save();
        return removed;
    }

    /** @return false if the source is missing or the target name is taken */
    public static boolean rename(String from, String to) {
        Category category = categories.get(key(from));
        if (category == null || categories.containsKey(key(to))) return false;
        categories.remove(key(from));
        category.name = to;
        categories.put(key(to), category);
        save();
        return true;
    }

    public static void setAnnounce(Category category, boolean announce) {
        category.announce = announce;
        save();
    }

    // ── Items ────────────────────────────────────────────────────────────────

    /**
     * Replaces a category's contents with what the editor GUI held. Weights are carried over for
     * items that came back unchanged, so reordering the chest does not reset a crate's odds.
     */
    public static void setItems(Category category, List<ItemStack> stacks, HolderLookup.Provider registries) {
        Map<String, Integer> previousWeights = new LinkedHashMap<>();
        for (StoredItem item : category.items) previousWeights.putIfAbsent(item.snbt, item.weight);

        List<StoredItem> replacement = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            String snbt = ((CompoundTag) stack.save(registries)).toString();
            replacement.add(new StoredItem(snbt, previousWeights.getOrDefault(snbt, 1)));
        }
        category.items = replacement;
        save();
    }

    /** @return the removed item, or null if the index is out of range */
    public static StoredItem removeItem(Category category, int index) {
        if (index < 0 || index >= category.items.size()) return null;
        StoredItem removed = category.items.remove(index);
        save();
        return removed;
    }

    /** @return false if the index is out of range or the target category is full */
    public static boolean moveItem(Category from, int index, Category to) {
        if (index < 0 || index >= from.items.size()) return false;
        if (to.items.size() >= MAX_ITEMS) return false;
        to.items.add(from.items.remove(index));
        save();
        return true;
    }

    public static boolean setWeight(Category category, int index, int weight) {
        if (index < 0 || index >= category.items.size()) return false;
        category.items.get(index).weight = weight;
        save();
        return true;
    }

    /**
     * Draws items at random, with replacement, honouring per-item weights.
     * A category whose weights are all zero draws nothing.
     */
    public static List<StoredItem> drawRandom(Category category, int count) {
        int total = category.items.stream().mapToInt(i -> Math.max(0, i.weight)).sum();
        List<StoredItem> drawn = new ArrayList<>();
        if (total <= 0) return drawn;

        for (int n = 0; n < count; n++) {
            int roll = RANDOM.nextInt(total);
            for (StoredItem item : category.items) {
                roll -= Math.max(0, item.weight);
                if (roll < 0) {
                    drawn.add(item);
                    break;
                }
            }
        }
        return drawn;
    }

    /**
     * Rebuilds a stored item into a real stack.
     *
     * @return the stack, or {@link ItemStack#EMPTY} if it no longer parses — which happens when a
     *         mod that provided the item is gone, so it is logged rather than silently dropped
     */
    public static ItemStack toStack(StoredItem item, HolderLookup.Provider registries) {
        try {
            CompoundTag tag = TagParser.parseTag(item.snbt);
            return ItemStack.parse(registries, tag).orElseGet(() -> {
                MKTEssentials.LOGGER.warn("Stored item could not be rebuilt (unknown item or removed mod): {}", item.snbt);
                return ItemStack.EMPTY;
            });
        } catch (Exception e) {
            MKTEssentials.LOGGER.warn("Stored item has malformed data: {}", item.snbt, e);
            return ItemStack.EMPTY;
        }
    }

    public static List<ItemStack> toStacks(Category category, HolderLookup.Provider registries) {
        List<ItemStack> stacks = new ArrayList<>();
        for (StoredItem item : category.items) {
            ItemStack stack = toStack(item, registries);
            if (!stack.isEmpty()) stacks.add(stack);
        }
        return stacks;
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private static void load() {
        if (!Files.exists(storageFile)) {
            categories = new LinkedHashMap<>();
            return;
        }
        try (Reader reader = Files.newBufferedReader(storageFile)) {
            Type type = new TypeToken<LinkedHashMap<String, Category>>(){}.getType();
            Map<String, Category> loaded = GSON.fromJson(reader, type);
            categories = loaded != null ? new LinkedHashMap<>(loaded) : new LinkedHashMap<>();
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to load items.json", e);
            categories = new LinkedHashMap<>();
            return;
        }
        // A hand-edited file can be missing the fields Gson never called a constructor for.
        categories.forEach((key, category) -> {
            if (category.name == null) category.name = key;
            if (category.items == null) category.items = new ArrayList<>();
        });
    }

    private static void save() {
        if (storageFile == null) return;
        try {
            Files.createDirectories(storageFile.getParent());
            try (Writer writer = Files.newBufferedWriter(storageFile)) {
                GSON.toJson(categories, writer);
            }
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to save items.json", e);
        }
    }
}
