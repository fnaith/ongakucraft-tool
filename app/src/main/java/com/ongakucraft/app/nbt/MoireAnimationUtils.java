package com.ongakucraft.app.nbt;

import com.ongakucraft.app.data.DataLoadingApp;
import com.ongakucraft.app.graphics.GraphicUtils;
import com.ongakucraft.core.block.BlockDatasetVersion;
import com.ongakucraft.core.prefab.MapArtBuilder;
import com.ongakucraft.core.structure.Range;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class MoireAnimationUtils {
    private static final String ROOT_DIR_PATH = "./data/generated";
    private static final BlockDatasetVersion VERSION = BlockDatasetVersion.of("1.18.2", 2975);
    private static void fubuzillaMapAnimation(BlockDatasetVersion version, String inputDirPath, String outputDirPath) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final List<String> inputFilePathList = new ArrayList<>();
        for (var i = 2; i <= 4/*13*/; ++i) {
            inputFilePathList.add(String.format("%s/frame_%02d_delay-0.1s.png", inputDirPath, i));
        }
//        log.info("{}", inputFilePathList);
        final var firstFrame = GraphicUtils.readImage(inputFilePathList.get(0));
        final var w = firstFrame.getWidth();
        final var h = firstFrame.getHeight();
//        log.info("{} {}", w, h);
        final var frames = inputFilePathList.size();
        final var slitFrames = GraphicUtils.scaleSize(firstFrame, w * frames, h);
        for (var i = 0; i < frames; ++i) {
            final var bufferedImage = GraphicUtils.readImage(inputFilePathList.get(i));
            for (var y = 0; y < h; ++y) {
                for (var x = 0; x < w; ++x) {
                    slitFrames.setRGB(x * frames + i, y, bufferedImage.getRGB(x, y));
                }
            }
        }
//        GraphicUtils.writeImage(slitFrames, String.format("%s/slitMap.png", inputDirPath));
        final var rows = (h - 1) / MapArtBuilder.BLOCK_LENGTH_PER_MAP + 1;
        final var cols = (w * frames - 1) / MapArtBuilder.BLOCK_LENGTH_PER_MAP + 1;
        final var slitMap = GraphicUtils.newImage(cols * MapArtBuilder.BLOCK_LENGTH_PER_MAP, rows * MapArtBuilder.BLOCK_LENGTH_PER_MAP);
        GraphicUtils.fill(slitMap, Color.LIGHT_GRAY.getRGB());
        GraphicUtils.paste(slitMap, slitFrames,
                (cols * MapArtBuilder.BLOCK_LENGTH_PER_MAP - w * frames) / 2,
                (rows * MapArtBuilder.BLOCK_LENGTH_PER_MAP - h) / 2);
        log.info("{} {}",
                (cols * MapArtBuilder.BLOCK_LENGTH_PER_MAP - w * frames) / 2,
                (rows * MapArtBuilder.BLOCK_LENGTH_PER_MAP - h) / 2);
//        GraphicUtils.writeImage(slitMap, String.format("%s/slitMap.png", inputDirPath));

        final var bufferedImage = slitMap;
        final var blockMapColorList = blockDataset.getBlockMapColorList();
        final var nbtWriter = NbtWriter.of(version);
        for (var row = 0; row < rows; ++row) {
            for (var col = 0; col < cols; ++col) {
                final var rangeY = Range.of(row * MapArtBuilder.BLOCK_LENGTH_PER_MAP, (row + 1) * MapArtBuilder.BLOCK_LENGTH_PER_MAP);
                final var rangeX = Range.of(col * MapArtBuilder.BLOCK_LENGTH_PER_MAP, (col + 1) * MapArtBuilder.BLOCK_LENGTH_PER_MAP);
                final var subBufferedImage = GraphicUtils.copy(bufferedImage, rangeX, rangeY);
                final var image = GraphicUtils.toRgbImage(subBufferedImage);
                final var structure = MapArtBuilder.build(image, blockMapColorList, blockDataset);
                nbtWriter.write(structure, String.format("%s/map-%d-%d.nbt", outputDirPath, row, col));
                structure.replace(structure.getRange3(), blockDataset.getBlock("air"));
                nbtWriter.write(structure, String.format("%s/map-%d-%d-.nbt", outputDirPath, row, col));
            }
        }
    }

    public static void main(String[] args) {
        try {
            final var inputDirPath = String.format("%s/input/fubuzilla", ROOT_DIR_PATH);
            final var outputDirPath = String.format("%s/%s/structure/fubuzilla", ROOT_DIR_PATH, VERSION.getMcVersion());
            fubuzillaMapAnimation(VERSION, inputDirPath, outputDirPath);
        } catch (Exception e) {
            log.error("MoireAnimationUtils", e);
        }
    }

    private MoireAnimationUtils() {}
}
