package pl.makoto.essentials.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a player's avatar (face) into rows of colored block characters for the chat greeting —
 * the "skin face" effect from FlectonePulse. Each row is a MiniMessage string of
 * {@code <color:#RRGGBB>█} pixels. The image is fetched from a configurable avatar API by UUID.
 */
public final class FaceRenderer {

    private static final int TARGET_WIDTH = 8;

    private FaceRenderer() {}

    /** Fetches and renders the avatar at the URL into pixel rows; empty list on any failure. */
    public static List<String> render(String url) {
        BufferedImage image = fetch(url);
        return image == null ? List.of() : render(image);
    }

    /**
     * Renders a full Minecraft skin texture (64x64 / legacy 64x32, e.g. from SkinsRestorer or
     * textures.minecraft.net) by cropping the 8x8 face region and compositing the hat overlay,
     * so the greeting shows the face rather than the whole unwrapped skin.
     */
    public static List<String> renderSkinTexture(String url) {
        BufferedImage skin = fetch(url);
        if (skin == null || skin.getWidth() < 64 || skin.getHeight() < 32) {
            return skin == null ? List.of() : render(skin);
        }
        List<String> rows = new ArrayList<>();
        for (int y = 8; y < 16; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 8; x < 16; x++) {
                int base = skin.getRGB(x, y);
                int hat = skin.getRGB(x + 32, y); // hat/hair overlay layer
                int rgb = ((hat >>> 24) & 0xFF) > 16 ? (hat & 0xFFFFFF) : (base & 0xFFFFFF);
                row.append("<color:#").append(String.format("%06X", rgb)).append(">█");
            }
            rows.add(row.toString());
        }
        return rows;
    }

    private static BufferedImage fetch(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setRequestProperty("User-Agent", "MKT-Essentials");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            return ImageIO.read(connection.getInputStream());
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> render(BufferedImage image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            if ((long) width * height >= 8L * 1024 * 1024) return List.of();

            int step = Math.max(Math.round(width / (float) TARGET_WIDTH), 1);
            List<String> rows = new ArrayList<>();
            for (int y = 0; y < height; y += step) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < width; x += step) {
                    int rgb = averageRgb(image, x, y, step, width, height);
                    row.append("<color:#").append(String.format("%06X", rgb)).append(">█");
                }
                rows.add(row.toString());
            }
            return rows;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static int averageRgb(BufferedImage image, int x, int y, int step, int width, int height) {
        if (step == 1) return image.getRGB(x, y) & 0xFFFFFF;
        int r = 0, g = 0, b = 0, count = 0;
        for (int dx = 0; dx < step; dx++) {
            for (int dy = 0; dy < step; dy++) {
                int color = image.getRGB(Math.min(x + dx, width - 1), Math.min(y + dy, height - 1));
                r += (color >> 16) & 0xFF;
                g += (color >> 8) & 0xFF;
                b += color & 0xFF;
                count++;
            }
        }
        return ((r / count) << 16) | ((g / count) << 8) | (b / count);
    }
}
