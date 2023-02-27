package com.ongakucraft.app.graphics;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

import com.ongakucraft.core.color.RgbColor;

import lombok.NonNull;

public final class GraphicUtils {
    public static RgbColor[][] toRgbImage(@NonNull BufferedImage bufferedImage) {
        final var h = bufferedImage.getHeight();
        final var w = bufferedImage.getWidth();
        final var image = new RgbColor[h][];
        for (var y = 0; y < h; ++y) {
            image[y] = new RgbColor[w];
            for (var x = 0; x < w; ++x) {
                if (0 == (bufferedImage.getRGB(x, y) >> 24)) {
                    continue;
                }
                final var color = new Color(bufferedImage.getRGB(x, y), true);
                image[y][x] = RgbColor.of(color.getRed(), color.getGreen(), color.getBlue());
            }
        }
        return image;
    }

    public static BufferedImage scaleByHeight(@NonNull BufferedImage bufferedImage, int h) {
        final var w = bufferedImage.getWidth() * h / bufferedImage.getHeight();
        return scaleSize(bufferedImage, h, w);
    }

    public static BufferedImage scaleSize(@NonNull BufferedImage bufferedImage, int h, int w) {
        final var out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.getGraphics().drawImage(bufferedImage.getScaledInstance(w, h, Image.SCALE_DEFAULT), 0, 0, null);
        return out;
    }

    private GraphicUtils() {}
}
