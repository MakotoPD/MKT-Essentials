package pl.makoto.essentials.data;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class Kit {
    private final String name;
    private final List<String> items; // NBT-serialized or simplified
    private long cooldown = 0;

    public Kit(String name, List<String> items) {
        this.name = name;
        this.items = items;
    }

    public String getName() { return name; }
    public List<String> getItems() { return items; }
    public long getCooldown() { return cooldown; }
    public void setCooldown(long cooldown) { this.cooldown = cooldown; }
}
