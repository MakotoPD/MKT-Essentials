package pl.makoto.essentials.util;

import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import pl.makoto.essentials.config.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inline chat "objects" (the pack-free subset of FlectonePulse's format/object): {@code [item]}
 * inserts the sender's held item with its tooltip, {@code <head>} inserts the sender's player head.
 * Both use {@link ItemStack#getDisplayName()}, which already carries the item hover — no resource
 * pack and no {@code show_item} parser support needed. Text between objects is formatted normally
 * (mentions/links/spoiler/markdown) via {@link MentionManager}.
 */
public final class ObjectManager {

    private static final Pattern TOKEN = Pattern.compile("\\[item\\]|<head>");

    /** Formatted message plus the players mentioned in it. */
    public record Result(MutableComponent message, List<ServerPlayer> mentioned) {}

    private ObjectManager() {}

    public static Result process(ServerPlayer sender, String text) {
        if (!Settings.isObjectsEnabled() || (!text.contains("[item]") && !text.contains("<head>"))) {
            MentionManager.Result r = MentionManager.process(sender, text);
            return new Result(r.message(), r.mentioned());
        }

        MutableComponent result = Component.empty();
        List<ServerPlayer> mentioned = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(text);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) appendText(sender, text.substring(last, matcher.start()), result, mentioned);
            result.append(matcher.group().equals("[item]") ? heldItem(sender) : playerHead(sender));
            last = matcher.end();
        }
        if (last < text.length()) appendText(sender, text.substring(last), result, mentioned);
        return new Result(result, mentioned);
    }

    private static void appendText(ServerPlayer sender, String segment, MutableComponent result, List<ServerPlayer> mentioned) {
        if (segment.isEmpty()) return;
        MentionManager.Result r = MentionManager.process(sender, segment);
        result.append(r.message());
        mentioned.addAll(r.mentioned());
    }

    private static Component heldItem(ServerPlayer sender) {
        ItemStack held = sender.getMainHandItem();
        if (held.isEmpty()) return MessageUtils.format("&7[empty hand]");
        return held.getDisplayName().copy();
    }

    private static Component playerHead(ServerPlayer sender) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        PropertyMap properties = sender.getGameProfile().getProperties();
        head.set(DataComponents.PROFILE, new ResolvableProfile(
                Optional.of(sender.getScoreboardName()), Optional.of(sender.getUUID()), properties));
        head.set(DataComponents.CUSTOM_NAME, MessageUtils.format("&f" + sender.getScoreboardName()));
        return head.getDisplayName().copy();
    }
}
