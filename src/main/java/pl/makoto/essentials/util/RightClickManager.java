package pl.makoto.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;

/**
 * Shows a player's info to whoever right-clicks them, inspired by FlectonePulse's rightclick module.
 * Configurable via the {@code rightclick} section (enable, require-sneak, format lines). The format
 * placeholders resolve for the clicked (target) player.
 */
@EventBusSubscriber(modid = MKTEssentials.MODID)
public final class RightClickManager {

    private RightClickManager() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!Settings.isRightclickEnabled()) return;
        // Only the main hand, so the info is shown once per click instead of twice
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer clicker)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;
        if (Settings.isRightclickRequireSneak() && !clicker.isShiftKeyDown()) return;

        for (String line : Settings.getRightclickFormat()) {
            clicker.sendSystemMessage(MessageUtils.format(target, line));
        }
    }
}
