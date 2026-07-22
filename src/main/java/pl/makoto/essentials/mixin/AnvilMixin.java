package pl.makoto.essentials.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.util.MessageUtils;

/**
 * Applies color/format codes to the item name typed in an anvil (converted to section codes before
 * it becomes the item's custom name). Gated by the {@code text-colors.anvil} config option.
 * <p>
 * The player isn't reachable here (the {@code player} field lives in the superclass), so no
 * per-player permission filtering is applied — the config toggle is the gate.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMixin {

    @ModifyVariable(method = "setItemName", at = @At("HEAD"), argsOnly = true, require = 0)
    private String mktessentials$colorItemName(String name) {
        if (name == null || name.isEmpty() || !Settings.isAnvilColorEnabled()) {
            return name;
        }
        return MessageUtils.legacyColors(null, name);
    }
}
