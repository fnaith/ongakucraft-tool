package com.ongakucraft.app.graphics;

import com.ongakucraft.core.color.LabColor;
import com.ongakucraft.core.color.RgbColor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public final class PixelArtUtils {
    private static final Map<Integer, LabColor> rgbaToLabColor = new HashMap<>();
    private static final Map<LabColor, Integer> labColorToRgba = new HashMap<>();

    private static Map<String, List<LabColor>> loadPalettes(String filePath) throws IOException {
        final var path = Path.of(filePath);
        final var lines = Files.readAllLines(path);
        final Map<String, List<LabColor>> nameToPalette = new LinkedHashMap<>();
        String name = null;
        List<LabColor> palette = null;
        for (final var line : lines) {
            if (line.startsWith("|+")) {
                name = line.substring(2);
                palette = new ArrayList<>();
                nameToPalette.put(name, palette);
            } else {
                final var r = Integer.parseInt(line.substring(0, 2), 16);
                final var g = Integer.parseInt(line.substring(2, 4), 16);
                final var b = Integer.parseInt(line.substring(4, 6), 16);
                final var rgba = 0xFF000000 | (r << 16) | (g << 8) | b;
                var labColor = rgbaToLabColor.get(rgba);
                if (null == labColor) {
                    final var rgbColor = RgbColor.of(r, g, b);
                    labColor = LabColor.of(rgbColor);
                    rgbaToLabColor.put(rgba, labColor);
                    labColorToRgba.put(labColor, rgba);
                }
                palette.add(labColor);
            }
        }
        return nameToPalette;
    }

    private static LabColor[][] toLabImage(@NonNull RgbColor[][] rgbImage) {
        final var h = rgbImage.length;
        final var w = rgbImage[0].length;
        final var labImage = new LabColor[h][];
        for (var y = 0; y < h; ++y) {
            final var rgbRow = rgbImage[y];
            final var rabRow = new LabColor[w];
            labImage[y] = rabRow;
            for (var x = 0; x < w; ++x) {
                final var rgbColor = rgbRow[x];
                final var labColor = LabColor.of(rgbColor);
                rabRow[x] = labColor;
            }
        }
        return labImage;
    }

    private static LabColor[][] sampleLabImage(@NonNull LabColor[][] labImage, int blockSize) {
        final var h = labImage.length / blockSize;
        final var w = labImage[0].length / blockSize;
        final var sampledImage = new LabColor[h][];
        final var blockPerSample = blockSize * blockSize;
        Arrays.setAll(sampledImage, i -> new LabColor[w]);
        for (var y = 0; y < h; ++y) {
            for (var x = 0; x < w; ++x) {
                var l = 0.0;
                var a = 0.0;
                var b = 0.0;
                for (var i = 0; i < blockSize; ++i) {
                    for (var j = 0; j < blockSize; ++j) {
                        final var labColor = labImage[y * blockSize + i][x * blockSize + j];
                        l += labColor.getL();
                        a += labColor.getA();
                        b += labColor.getB();
                    }
                }
                sampledImage[y][x] = LabColor.of(l / blockPerSample, a / blockPerSample, b / blockPerSample);
            }
        }
        return sampledImage;
    }

    private static LabColor findClosestColor(@NonNull LabColor labColor, @NonNull List<LabColor> palette) {
        var diff = Double.MAX_VALUE;
        LabColor closestColor = null;
        for (final var paletteColor : palette) {
            final var dist = labColor.distance(paletteColor);
            if (dist < diff) {
                diff = dist;
                closestColor = paletteColor;
            }
        }
        return closestColor;
    }

    private static double diffPalette(@NonNull LabColor[][] labImage, @NonNull List<LabColor> palette) {
        var diff = 0.0;
        final var w = labImage[0].length;
        for (final var labRow : labImage) {
            for (var x = 0; x < w; ++x) {
                final var labColor = labRow[x];
                final var closestColor = findClosestColor(labColor, palette);
                final var dist = labColor.distance(closestColor);
                diff += dist;
            }
        }
        return diff;
    }

    private static BufferedImage toBufferedImage(@NonNull LabColor[][] labImage, @NonNull List<LabColor> palette) {
        final var h = labImage.length;
        final var w = labImage[0].length;
        final var bufferedImage = GraphicUtils.newImage(w, h);
        for (var y = 0; y < h; ++y) {
            final var labRow = labImage[y];
            for (var x = 0; x < w; ++x) {
                final var labColor = labRow[x];
                final var closestColor = findClosestColor(labColor, palette);
                final var rgba = labColorToRgba.get(closestColor);
                bufferedImage.setRGB(x, y, rgba);
            }
        }
        return bufferedImage;
    }

    public static void main(String[] args) {
        try {
            // https://emulation.gametechwiki.com/index.php/Famicom_color_palette
            final String paletteFilePath = "./data/pixel_art/palette.txt";
            final var inputFilePath = "./data/generated/input/towa/towa.png";
            final var outputFilePattern = "./data/generated/input/towa/pixel_art/%02d-%s.png";
            final var blockSize = 2;
            final var nameToPalette = loadPalettes(paletteFilePath);
            final var bufferedImage = GraphicUtils.readImage(inputFilePath);
            final var rgbImage = GraphicUtils.toRgbImage(bufferedImage);
            final var labImage = toLabImage(rgbImage);
            final var sampledLabImage = sampleLabImage(labImage, blockSize);
            final Map<Double, String> diffToName = new TreeMap<>();
            for (final var entry : nameToPalette.entrySet()) {
                final var name = entry.getKey();
                final var palette = entry.getValue();
                var diff = diffPalette(sampledLabImage, palette);
//                log.info("{} : {}", name, diff);
                while (diffToName.containsKey(diff)) {
                    diff += 0.000_000_001;
                }
                diffToName.put(diff, name);
            }
            int index = 0;
            for (final var name : diffToName.values()) {
                final var palette = nameToPalette.get(name);
                final var sampledBufferedImage = toBufferedImage(sampledLabImage, palette);
                final var scaledBufferedImage = GraphicUtils.scaleSize(sampledBufferedImage,
                                                                       sampledBufferedImage.getWidth() * blockSize,
                                                                       sampledBufferedImage.getHeight() * blockSize);
                final var outputFilePath = String.format(outputFilePattern, index++, name);
                GraphicUtils.writeImage(scaledBufferedImage, outputFilePath);
            }
        } catch (Exception e) {
            log.error("PixelArtUtils", e);
        }
    }

    private PixelArtUtils() {}
}
