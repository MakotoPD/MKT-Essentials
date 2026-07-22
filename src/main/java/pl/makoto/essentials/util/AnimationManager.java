package pl.makoto.essentials.util;

import net.minecraft.server.MinecraftServer;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Named reusable animations, inspired by FlectonePulse's format/animation module.
 * <p>
 * Any message can embed {@code <animation:name>}; it is replaced with the current frame of the
 * named animation (defined under {@code animations:} in chat.yml). The frame advances every
 * {@code interval} ticks, driven by the server tick — so it animates consistently everywhere it is
 * refreshed (tab header/footer, boss bar, MOTD, chat, ...). Unknown animations resolve to empty.
 */
public final class AnimationManager {

    private static final Pattern TAG = Pattern.compile("<animation:([a-zA-Z0-9_-]+)>");

    private AnimationManager() {}

    public static String apply(String text) {
        if (text == null || text.isEmpty() || !text.contains("<animation:")) return text;

        MinecraftServer server = MKTEssentials.getServer();
        long tick = server != null ? server.getTickCount() : 0L;

        Matcher matcher = TAG.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            Settings.Animation animation = Settings.getAnimation(matcher.group(1));
            String frame = "";
            if (animation != null && !animation.frames().isEmpty()) {
                int interval = Math.max(1, animation.interval());
                int index = (int) Math.floorMod(tick / interval, animation.frames().size());
                frame = animation.frames().get(index);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(frame));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
