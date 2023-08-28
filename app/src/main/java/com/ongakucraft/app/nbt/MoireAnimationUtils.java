package com.ongakucraft.app.nbt;

import com.ongakucraft.app.data.DataLoadingApp;
import com.ongakucraft.app.graphics.GraphicUtils;
import com.ongakucraft.core.block.BlockDatasetVersion;
import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.prefab.MapArtBuilder;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Range;
import com.ongakucraft.core.structure.Structure;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.math.BigDecimal;
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
                final var structure = MapArtBuilder.buildMap(image, blockMapColorList, blockDataset);
                nbtWriter.write(structure, String.format("%s/map-%d-%d.nbt", outputDirPath, row, col));
                structure.replace(structure.getRange3(), blockDataset.getBlock("air"));
                nbtWriter.write(structure, String.format("%s/map-%d-%d-.nbt", outputDirPath, row, col));
            }
        }
    }

    private static void shubaDuckMapAnimation(BlockDatasetVersion version, String inputDirPath, String outputDirPath) throws Exception {
/*
https://www.youtube.com/watch?v=je46kyvv-8E
https://www.youtube.com/watch?v=ogvJ-JsjHSM
https://pastebin.com/u5UBQwhS
https://www.reddit.com/r/Minecraft/comments/uznjk2/comment/iab970v/?utm_source=share&utm_medium=web2x&context=3
/summon minecraft:glow_item_frame ~ ~2 ~ {Invisible:1b,Facing:0b,ItemRotation:0b,Item:{id:"minecraft:filled_map",Count:1b,tag:{map:1}}}
/give @a minecraft:filled_map{map:0}
/fill 0 -60 0 22 -39 0 minecraft:bricks
/function ongakucraft:summon_animation

/tp @a 11.407 -48.466 43.44 // 0 RED RED
/tp @a 11.407 -48.474 43.44 // 8 PINK RED
/tp @a 11.407 -48.482 43.44 // 16 ORANGE RED
/tp @a 11.407 -48.490 43.44 // 24 YELLOW RED
/tp @a 11.407 -48.498 43.44 // 32 GREEN RED
/tp @a 11.407 -48.506 43.44 // 40 MAGENTA RED
/tp @a 11.407 -48.514 43.44 // 44 CYAN RED

/tp @a 11.407 -48.506 43.44 // 40 MAGENTA RED
/tp @a 11.415 -48.506 43.44 // 41 MAGENTA PINK
/tp @a 11.423 -48.506 43.44 // 42 MAGENTA ORANGE
/tp @a 11.431 -48.506 43.44 // 43 MAGENTA YELLOW
/tp @a 11.439 -48.506 43.44 // 44 MAGENTA GREEN
/tp @a 11.447 -48.506 43.44 // 45 MAGENTA MAGENTA
/tp @a 11.455 -48.506 43.44 // 46 MAGENTA CYAN
/tp @a 11.463 -48.506 43.44 // 47 MAGENTA BLUE

/tp @a 11.463 -48.514 43.44 // 55 CYAN BLUE
/scoreboard players set @a ticks 1
*/
        final var frameStart = 0;
        final var frameEnd = 55;
        final var framesPerEdge = 8;
        final var backgroundColor = Color.WHITE;
        final var signWidth = 4;
        final var signColors = List.of(Color.RED, Color.PINK, Color.ORANGE, Color.YELLOW,
                Color.GREEN, Color.MAGENTA, Color.CYAN, Color.BLUE);
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final List<String> inputFilePathList = new ArrayList<>();
        for (var i = frameStart; i <= frameEnd; ++i) {
            inputFilePathList.add(String.format("%s/frame_%02d_delay-0.1s.gif", inputDirPath, i));
        }

        final var frames = frameEnd - frameStart + 1;
//        log.info("{}", inputFilePathList);
        final var firstFrame = GraphicUtils.readImage(inputFilePathList.get(0));
        final var frameHeight = firstFrame.getHeight();
        final var frameWidth = firstFrame.getWidth();
//        log.info("{} {}", w, h);
        final var slitFrames = GraphicUtils.scaleSize(firstFrame, frameWidth * framesPerEdge, frameHeight * framesPerEdge);
        GraphicUtils.fill(slitFrames, backgroundColor.getRGB());
        for (var r = 0; r < framesPerEdge; ++r) {
            for (var c = 0; c < framesPerEdge; ++c) {
                final var index = c + r * framesPerEdge;
                if (index < frames) {
                    final var bufferedImage = GraphicUtils.readImage(inputFilePathList.get(index));
                    final var signedBufferedImage = GraphicUtils.copy(bufferedImage, Range.of(frameWidth), Range.of(frameHeight));
//                    GraphicUtils.fill(signedBufferedImage, signColors.get(c).getRGB(), Range.of(signWidth, signWidth * 2), Range.of(frameHeight));
//                    GraphicUtils.fill(signedBufferedImage, signColors.get(c).getRGB(), Range.of(frameWidth), Range.of(signWidth, signWidth * 2));
//                    GraphicUtils.fill(signedBufferedImage, signColors.get(r).getRGB(), Range.of(0, signWidth), Range.of(frameHeight));
//                    GraphicUtils.fill(signedBufferedImage, signColors.get(r).getRGB(), Range.of(frameWidth), Range.of(0, signWidth));
                    for (var y = 0; y < frameHeight; ++y) {
                        for (var x = 0; x < frameWidth; ++x) {
                            slitFrames.setRGB(x * framesPerEdge + c, y * framesPerEdge + r, signedBufferedImage.getRGB(x, y));
                        }
                    }
                }
            }
        }
//        GraphicUtils.writeImage(slitFrames, String.format("%s/slitMap.png", inputDirPath));

        final var rows = (slitFrames.getHeight() - 1) / MapArtBuilder.BLOCK_LENGTH_PER_MAP + 1;
        final var cols = (slitFrames.getWidth() - 1) / MapArtBuilder.BLOCK_LENGTH_PER_MAP + 1;
        final var slitMap = GraphicUtils.newImage(cols * MapArtBuilder.BLOCK_LENGTH_PER_MAP, rows * MapArtBuilder.BLOCK_LENGTH_PER_MAP);
        GraphicUtils.fill(slitMap, backgroundColor.getRGB());
        GraphicUtils.paste(slitMap, slitFrames,
                (cols * MapArtBuilder.BLOCK_LENGTH_PER_MAP - slitFrames.getWidth()) / 2,
                (rows * MapArtBuilder.BLOCK_LENGTH_PER_MAP - slitFrames.getHeight()) / 2);
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
                final var colorMap = MapArtBuilder.buildColorMap(image, blockMapColorList);
                nbtWriter.write(colorMap, String.format("%s/map_%d.dat", outputDirPath, col + row * cols));
                log.info("execute as @a[gamemode=creative] run summon minecraft:glow_item_frame {} {} {} {Invisible:1b,Facing:{}b,ItemRotation:0b,Item:{id:\"minecraft:filled_map\",Count:1b,tag:{map:{}}}}",
                         col, -60 + rows - 1 - row, 1, Direction.S.getValue(), col + row * cols);
            }
        }
        final var wallPos = Position.of(0, -60, 0);
        log.info("/fill {} {} {} {} {} {} {}", wallPos.getX(), wallPos.getY(), wallPos.getZ(),
                wallPos.getX() + cols - 1, wallPos.getY() + rows - 1, wallPos.getZ(), "minecraft:bricks");
    }

    public static Structure shubaDuckMapAnimationCircuit(BlockDatasetVersion version) throws Exception {
        final var frames = 56;
        final var framesPerEdge = 8;
        final var blocksPerLine = 7;
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var structure = new Structure();
        final var playerX = new BigDecimal("11.407");
        final var playerY = new BigDecimal("-48.466");
        final var playerZ = new BigDecimal("43.440");
        final var playerStep = new BigDecimal("0.008");
        var head = Position.ZERO;
        var dir = Direction.S;
        final var turn = dir.left();
        for (var index = 0; index < frames; ++index) {
            final var y = index / framesPerEdge;
            final var x = index % framesPerEdge;
            final var repeater = blockDataset.getBlock("repeater").putProperty("delay", 4)
                    .rotate((index / blocksPerLine) % 2 * 2);
            structure.put(head, repeater);
            head = head.step(dir);
            final var commandBlock = blockDataset.getBlock("command_block")
                    .putData("Command", String.format("/execute if entity @e[scores={ticks=1..10}] as @a at @s run tp @s %s %s %s 180 0",
                            playerX.add(playerStep.multiply(BigDecimal.valueOf(x))),
                            playerY.subtract(playerStep.multiply(BigDecimal.valueOf(y))), playerZ));
            structure.put(head, commandBlock);
            head = head.step(dir);
            if (0 == ((index + 1) % blocksPerLine)) {
                head = head.step(turn, 2);
                dir = dir.back();
                head = head.step(dir);
            }
        }
        return structure;
    }

    public static void main(String[] args) {
        try {
//            final var inputDirPath = String.format("%s/input/fubuzilla", ROOT_DIR_PATH);
//            final var outputDirPath = String.format("%s/%s/structure/fubuzilla", ROOT_DIR_PATH, VERSION.getMcVersion());
//            fubuzillaMapAnimation(VERSION, inputDirPath, outputDirPath);

            final var inputDirPath = String.format("%s/input/shuba_duck/frames", ROOT_DIR_PATH);
            final var outputDirPath = String.format("%s/%s/map/shuba_duck", ROOT_DIR_PATH, VERSION.getMcVersion());
            shubaDuckMapAnimation(VERSION, inputDirPath, outputDirPath);

            final var structure = shubaDuckMapAnimationCircuit(VERSION);
            final var outputFilePath = String.format("%s/%s/structure/shuba_duck-player.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
            NbtWriter.of(VERSION).write(structure, outputFilePath);
        } catch (Exception e) {
            log.error("MoireAnimationUtils", e);
        }
    }

    private MoireAnimationUtils() {}
}
