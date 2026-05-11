package pl.makoto.essentials.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class KitData {
    private String name;
    private long cooldownSeconds;
    private List<String> itemsSnbt;

    public KitData(String name, long cooldownSeconds) {
        this.name = name;
        this.cooldownSeconds = cooldownSeconds;
        this.itemsSnbt = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public List<String> getItemsSnbt() {
        return itemsSnbt;
    }

    public void setItems(List<ItemStack> inventory, HolderLookup.Provider registries) {
        this.itemsSnbt.clear();
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                CompoundTag tag = (CompoundTag) stack.save(registries);
                this.itemsSnbt.add(tag.toString());
            }
        }
    }

    public List<ItemStack> restoreItems(HolderLookup.Provider registries) {
        List<ItemStack> restored = new ArrayList<>();
        for (String snbt : itemsSnbt) {
            try {
                CompoundTag tag = TagParser.parseTag(snbt);
                ItemStack.parse(registries, tag).ifPresent(restored::add);
            } catch (Exception e) {
                // Skip failed items
            }
        }
        return restored;
    }
}
