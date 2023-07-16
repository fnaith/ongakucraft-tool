package com.ongakucraft.app.graphics;

import com.ongakucraft.core.color.RgbColor;
import com.ongakucraft.core.structure.Range;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class GraphicUtils {
    public static BufferedImage readImage(@NonNull String inputFilePath) throws Exception {
        return ImageIO.read(new File(inputFilePath));
    }

    public static void writeImage(@NonNull BufferedImage bufferedImage, @NonNull String outputFilePath) throws Exception {
        final var file = new File(outputFilePath);
        FileUtils.createParentDirectories(file);
        ImageIO.write(bufferedImage, "PNG", file);
    }

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

    public static BufferedImage newImage(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    public static BufferedImage scaleByHeight(@NonNull BufferedImage bufferedImage, int h) {
        final var w = bufferedImage.getWidth() * h / bufferedImage.getHeight();
        return scaleSize(bufferedImage, w, h);
    }

    public static BufferedImage scaleSize(@NonNull BufferedImage bufferedImage, int w, int h) {
        final var out = newImage(w, h);
        out.getGraphics().drawImage(bufferedImage.getScaledInstance(w, h, Image.SCALE_DEFAULT), 0, 0, null);
        return out;
    }

    public static BufferedImage copy(@NonNull BufferedImage bufferedImage, @NonNull Range rangeX, @NonNull Range rangeY) {
        final var h = rangeY.length();
        final var w = rangeX.length();
        final var image = newImage(w, h);
        image.getGraphics().drawImage(bufferedImage.getSubimage(rangeX.getStart(), rangeY.getStart(), w, h), 0, 0, null);
        return image;
    }

    public static void paste(@NonNull BufferedImage dist, @NonNull BufferedImage src, int x, int y) {
        dist.getGraphics().drawImage(src, x, y, null);
    }

    public static void fill(@NonNull BufferedImage bufferedImage, int color) {
        final var h = bufferedImage.getHeight();
        final var w = bufferedImage.getWidth();
        for (var y = 0; y < h; ++y) {
            for (var x = 0; x < w; ++x) {
                bufferedImage.setRGB(x, y, color);
            }
        }
    }

    public static BufferedImage rotate(BufferedImage img, boolean clockwise) {
        final var width = img.getWidth();
        final var height = img.getHeight();
        final var newImage = new BufferedImage(width, height, img.getType());
        final var g2 = newImage.createGraphics();
        g2.rotate(Math.toRadians(clockwise ? -90 : 90), width / 2.0, height / 2.0);
        g2.drawImage(img, null, 0, 0);
        return newImage;
    }

    public static BufferedImage copyWithAlpha(BufferedImage bi) {
        final var cm = bi.getColorModel();
        final boolean isAlphaPreMultiplied = cm.isAlphaPremultiplied();
        final var raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPreMultiplied, null);
    }

    private GraphicUtils() {}
}
