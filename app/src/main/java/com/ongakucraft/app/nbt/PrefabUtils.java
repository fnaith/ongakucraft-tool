package com.ongakucraft.app.nbt;

import com.ongakucraft.app.data.DataLoadingApp;
import com.ongakucraft.app.graphics.GraphicUtils;
import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.Block;
import com.ongakucraft.core.block.BlockDatasetVersion;
import com.ongakucraft.core.color.RgbColor;
import com.ongakucraft.core.prefab.*;
import com.ongakucraft.core.structure.Cursor;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Range;
import com.ongakucraft.core.structure.Structure;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public final class PrefabUtils {
    private static final String ROOT_DIR_PATH = "./data/generated";
    private static final BlockDatasetVersion VERSION = BlockDatasetVersion.of("1.18.2", 2975);

    private static Structure oneBlock(BlockDatasetVersion version, String path) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var block = blockDataset.getBlock(path);
        final var structure = new Structure();
        structure.put(Position.ZERO, block);
        return structure;
    }

    private static Structure wall(BlockDatasetVersion version, String csv) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var structure = new Structure();
        csv = csv.trim();
        final var rows = csv.split("\n");
        final var h = rows.length;
        for (var y = 0; y < h; ++y) {
            final var row = rows[y];
            final var cells = row.trim().split(",");
            final var w = cells.length;
            for (var x = 0; x < w; ++x) {
                final var path = cells[x];
                final var block = blockDataset.getBlock(path);
                final var position = Position.of(w - 1 - x, h - 1 - y, 0);
                if (null == block) {
                    continue;
                }
                structure.put(position, block);
            }
        }
        return structure;
    }

    private static Structure watamePixelArt(BlockDatasetVersion version) throws Exception {
        final var csv =
                """
                0,0,1,0,1,1,1,1,1,0,1,0,0
                0,1,2,1,6,5,5,5,6,1,2,1,0
                1,2,3,6,7,6,6,6,7,E,3,2,1
                1,3,6,7,8,7,7,7,8,7,6,3,1
                0,1,6,8,K,8,8,8,K,8,6,1,0
                1,6,7,8,4,8,8,8,4,8,7,6,1
                0,1,6,E,9,9,F,9,9,E,6,1,0
                0,1,6,A,B,C,G,C,B,A,6,1,0
                1,6,A,1,J,D,D,D,J,1,A,6,1
                1,6,1,8,I,8,C,8,I,8,1,6,1
                0,1,E,1,3,8,1,8,3,1,E,1,0
                0,0,1,0,1,1,0,1,1,0,1,0,0
                """
                        .replaceAll("0", "air")
                        .replaceAll("1", "black_concrete")
                        .replaceAll("2", "tuff")
                        .replaceAll("3", "polished_deepslate")
                        .replaceAll("4", "clay")
                        .replaceAll("5", "bone_block")
                        .replaceAll("6", "smooth_sandstone")
                        .replaceAll("7", "cut_sandstone")
                        .replaceAll("8", "smooth_quartz")
                        .replaceAll("9", "white_terracotta")
                        .replaceAll("A", "sand")
                        .replaceAll("B", "birch_planks")
                        .replaceAll("C", "white_wool")
                        .replaceAll("D", "quartz_bricks")
                        .replaceAll("E", "pink_concrete_powder")
                        .replaceAll("F", "copper_block")
                        .replaceAll("G", "red_mushroom_block")
                        .replaceAll("I", "pink_wool")
                        .replaceAll("J", "polished_diorite")
                        .replaceAll("K", "crying_obsidian")
        ;
        return wall(version, csv);
    }

    private static Structure beaconSpectrum(BlockDatasetVersion version) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        return BeaconSpectrumBuilder.buildBeaconSpectrum(5, blockDataset);
    }

    private static void filterSimpleColor(BlockDatasetVersion version) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        BlockColorFilter.filterSimpleColor(blockDataset.getBlockLabColorList(), BlockColorFilterOption.DEFAULT);
    }

    private static Structure shishiroPixelArt(BlockDatasetVersion version, String inputFilePath) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var bufferedImage = GraphicUtils.readImage(inputFilePath);
        final var scaledBufferedImage = GraphicUtils.scaleByHeight(bufferedImage, 192);
        final var image = GraphicUtils.toRgbImage(scaledBufferedImage);
        final var colorBlockList = BlockColorFilter.filterSimpleColor(blockDataset.getBlockLabColorList(), BlockColorFilterOption.DEFAULT);
        final var blockGrid = PixelArtBuilder.frontWallBlockGrid(image, colorBlockList, blockDataset);
        final var structure = PixelArtBuilder.frontWall(blockGrid);
        return structure;
    }

    private static void bocchiTheRockAnimation(BlockDatasetVersion version, String inputDirPath, String outputDirPath) {
        try (final var walk = Files.walk(Paths.get(inputDirPath))) {
            final List<String> framePathList = new ArrayList<>();
            walk.forEach(path -> {
                if (Files.isDirectory(path)) {
                    return;
                }
                final var filePath = path.toString();
                final var fileTokens = filePath.split("\\\\");
                final var fileName = fileTokens[fileTokens.length - 1];
                if (fileName.endsWith(".gif")) {
                    framePathList.add(filePath);
                }
            });
            Collections.sort(framePathList);
//            log.info(String.join("\n", framePathList));
            final List<RgbColor[][]> imageList = new ArrayList<>();
            for (final var framePath : framePathList) {
                final var bufferedImage = GraphicUtils.readImage(framePath);
                final var scaledBufferedImage = GraphicUtils.scaleByHeight(bufferedImage, 64);
                imageList.add(GraphicUtils.toRgbImage(scaledBufferedImage));
            }
            final var blockDataset = DataLoadingApp.loadBlockDataset(version);
            final var nbtWriter = NbtWriter.of(version);
            final List<Block[][]> blockGridList = new ArrayList<>();
            final var colorBlockList = BlockColorFilter.filterColoredBlock(blockDataset.getBlockLabColorList());
            for (final var image : imageList) {
                final var blockGrid = PixelArtBuilder.frontWallBlockGrid(image, colorBlockList, blockDataset);
                blockGridList.add(blockGrid);
            }
            for (var i = 0; i < blockGridList.size(); ++i) {
                final var blockGrid = blockGridList.get(i);
                final var structure = PixelArtBuilder.frontWall(blockGrid);
                nbtWriter.write(structure, String.format("%s/image-%02d.nbt", outputDirPath, i));
            }
            final var diffBlockGridList = AnimationBuilder.diffBlockGridList(blockGridList);
            final List<Structure> diffStructureList = new ArrayList<>();
            for (final var diffBlockGrid : diffBlockGridList) {
                final var diffStructure = PixelArtBuilder.frontWall(diffBlockGrid);
                diffStructureList.add(diffStructure);
                log.info("range : {}, size : {}", diffStructure.getRange3(), diffStructure.size());
            }
            for (var i = 0; i < diffStructureList.size(); ++i) {
                final var diffStructure = diffStructureList.get(i);
                nbtWriter.write(diffStructure, String.format("%s/diff-%02d.nbt", outputDirPath, i));
            }
        } catch (Exception e) {
            throw new OcException(e.getMessage());
        }
    }

    private static Structure demoMapArtColor(BlockDatasetVersion version) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var blockMapColorList = blockDataset.getBlockMapFlatColorList();
        final var structure = new Structure();
        final var grassBlock = blockDataset.getBlock("grass_block");
        for (var i = 0; i < blockMapColorList.size(); ++i) {
            final var mapColor = blockMapColorList.get(i);
            final var block = blockDataset.getBlock(mapColor.getId());
            for (var j = -1; j <= 1; ++j) {
                final var position = Position.of(i, j, j);
                if (mapColor.getId().getPath().endsWith("_leaves")) {
                    structure.put(position, block.putProperty("persistent", true));
                } else {
                    structure.put(position, block);
                }
                if (!blockDataset.getBlockDefine(block.getId()).isCollisionShapeFullBlock()) {
                    structure.put(position.jump(-1), grassBlock);
                }
            }
        }
        return MapArtBuilder.surroundWater(structure, blockDataset);
    }

    private static Structure uberSheepMapArt(BlockDatasetVersion version, String inputFilePath) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var bufferedImage = GraphicUtils.readImage(inputFilePath);
        final var scaledBufferedImage = GraphicUtils.scaleByHeight(bufferedImage, 128);
        final var image = GraphicUtils.toRgbImage(scaledBufferedImage);
        final var blockMapColorList = blockDataset.getBlockMapFlatColorList();
        final var structure = MapArtBuilder.build(image, blockMapColorList, blockDataset);
        return structure;
    }

    private static void towaMapArt(BlockDatasetVersion version, String inputFilePath, String outputDirPath) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var bufferedImage = GraphicUtils.readImage(inputFilePath);
        final var blockMapColorList = blockDataset.getBlockMapColorList();
        final var nbtWriter = NbtWriter.of(version);
        final var rows = bufferedImage.getHeight() / MapArtBuilder.BLOCK_LENGTH_PER_MAP;
        final var cols = bufferedImage.getWidth() / MapArtBuilder.BLOCK_LENGTH_PER_MAP;
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

    private static Structure demoNoDelay(BlockDatasetVersion version) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var structure = new Structure();
        final int[] keys = { 6, 8, 10, 11, 13 };
        final var floor = "iron_block";
        final var stub = "redstone_lamp";
        final var trunkCursorLow = new Cursor(blockDataset, structure);
        final var trunkCursorHigh = trunkCursorLow.clone().jump(1);
        trunkCursorLow.place(floor).step();
        trunkCursorHigh.put(trunkCursorHigh.getBlock("oak_button").putProperty("face", "floor")).step();
        for (var i = 0; i < keys.length; ++i) {
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.placeRepeater(2).step();
            trunkCursorLow.place("iron_block").step();
            trunkCursorHigh.placeNoteBlock(keys[i]).step();
            if (i < keys.length - 1) {
                trunkCursorLow.place(floor).step();
                trunkCursorHigh.placeRepeater(2).step();
                trunkCursorLow.place(floor).step();
                trunkCursorHigh.place(stub).step();
            }
        }
        return structure;
    }

    private static Structure demoWithDelay(BlockDatasetVersion version) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var structure = new Structure();
        final int[] keys = { 6, 8, 10, 11, 13 };
        final var dist01 = 26;
        final var dist12 = 7;
        final var dist13 = 12;
        final var floor = "iron_block";
        final var stub = "redstone_lamp";
        final var trunkCursorLow = new Cursor(blockDataset, structure);
        final var trunkCursorHigh = trunkCursorLow.clone().jump(1);
        trunkCursorLow.place(floor).step();
        trunkCursorHigh.put(trunkCursorHigh.getBlock("oak_button").putProperty("face", "floor")).step();
        for (var i = 0; i < keys.length; ++i) {
            final var rotateTimes = 0 == i % 2 ? -1 : 1;
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.placeRepeater(2).step();
            trunkCursorLow.place("iron_block");
            trunkCursorHigh.placeNoteBlock(keys[i]);

            final var branchCursorLow1 = trunkCursorLow.clone().rotate(rotateTimes).step();
            final var repeaterCount1 = placeRedstoneWireLine(branchCursorLow1, floor, dist01);
            if (1 != repeaterCount1) {
                throw new OcException("repeaterCount1 must be 1 : %d", repeaterCount1);
            }
            branchCursorLow1.place("iron_block");
            branchCursorLow1.clone().jump(1).placeNoteBlock(keys[i]);
            branchCursorLow1.step();

            final var branchCursorLow2 = trunkCursorLow.clone().rotate(-rotateTimes).step();
            final var repeaterCount2 = placeRedstoneWireLine(branchCursorLow2, floor, dist01);
            if (1 != repeaterCount2) {
                throw new OcException("repeaterCount2 be 1 : %d", repeaterCount2);
            }
            branchCursorLow2.place(floor);
            branchCursorLow2.clone().jump(1).placeRepeater(1);
            branchCursorLow2.step();
            final var repeaterCount3 = placeRedstoneWireLine(branchCursorLow2, floor, dist12);
            if (0 != repeaterCount3) {
                throw new OcException("repeaterCount3 be 0 : %d", repeaterCount3);
            }
            branchCursorLow2.place("iron_block");
            branchCursorLow2.clone().jump(1).placeNoteBlock(keys[i]);
            branchCursorLow2.step();

            final var branchCursorLow3 = branchCursorLow1.clone();
            final var repeaterCount4 = placeRedstoneWireLine(branchCursorLow3, floor, dist13);
            if (0 != repeaterCount4) {
                throw new OcException("repeaterCount4 be 0 : %d", repeaterCount4);
            }
            branchCursorLow3.place("iron_block");
            branchCursorLow3.clone().jump(1).placeNoteBlock(keys[i]);
            branchCursorLow3.step();

            trunkCursorLow.step().place(floor).step();
            trunkCursorHigh.step().placeRepeater(2).step();
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.place(stub).step();
        }
        return structure;
    }

    private static Structure demoReverb(BlockDatasetVersion version) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var structure = new Structure();
        final int[] keys = { 6, 10, 13, 18 };
        final var dist01 = 29;
        final var dist12 = 3;
        final var dist13 = 7;
        final var floor = "iron_block";
        final var stub = "redstone_lamp";
        final var trunkCursorLow = new Cursor(blockDataset, structure);
        final var trunkCursorHigh = trunkCursorLow.clone().jump(1);
        trunkCursorLow.place(floor).step();
        trunkCursorHigh.put(trunkCursorHigh.getBlock("oak_button").putProperty("face", "floor")).step();
        for (var i = 0; i < keys.length; ++i) {
            final var rotateTimes = 0 == i % 2 ? -1 : 1;
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.placeRepeater(2).step();
            trunkCursorLow.place("iron_block");
            trunkCursorHigh.placeNoteBlock(keys[i]);

            final var branchCursorLow1 = trunkCursorLow.clone().rotate(rotateTimes).step();
            final var repeaterCount1 = placeRedstoneWireLine(branchCursorLow1, floor, dist01);
            if (1 != repeaterCount1) {
                throw new OcException("repeaterCount1 must be 1 : %d", repeaterCount1);
            }
            branchCursorLow1.place("iron_block");
            branchCursorLow1.clone().jump(1).placeNoteBlock(keys[i]);
            branchCursorLow1.step();

            final var branchCursorLow2 = trunkCursorLow.clone().rotate(-rotateTimes).step();
            final var repeaterCount2 = placeRedstoneWireLine(branchCursorLow2, floor, dist01);
            if (1 != repeaterCount2) {
                throw new OcException("repeaterCount2 be 1 : %d", repeaterCount2);
            }
            branchCursorLow2.place(floor);
            branchCursorLow2.clone().jump(1).placeRepeater(1);
            branchCursorLow2.step();
            final var repeaterCount3 = placeRedstoneWireLine(branchCursorLow2, floor, dist12);
            if (0 != repeaterCount3) {
                throw new OcException("repeaterCount3 be 0 : %d", repeaterCount3);
            }
            branchCursorLow2.place("iron_block");
            branchCursorLow2.clone().jump(1).placeNoteBlock(keys[i]);
            branchCursorLow2.step();

            final var branchCursorLow3 = branchCursorLow1.clone();
            final var repeaterCount4 = placeRedstoneWireLine(branchCursorLow3, floor, dist13);
            if (0 != repeaterCount4) {
                throw new OcException("repeaterCount4 be 0 : %d", repeaterCount4);
            }
            branchCursorLow3.place("iron_block");
            branchCursorLow3.clone().jump(1).placeNoteBlock(keys[i]);
            branchCursorLow3.step();

            trunkCursorLow.step().place(floor).step();
            trunkCursorHigh.step().placeRepeater(2).step();
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.place(stub).step();
        }
        return structure;
    }

    private static Structure demoSustain(BlockDatasetVersion version) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var structure = new Structure();
        final int[] keys = { 6, 10, 13, 18 };
        final int[] dist01 = { 22, 2, 2, 2, 2, 1, 1, 1, 1, 1 };
        final int[] dist02 = { 24, 2, 2, 2, 1, 1, 1, 1, 1 };
        final var floor = "iron_block";
        final var stub = "redstone_lamp";
        final var trunkCursorLow = new Cursor(blockDataset, structure);
        final var trunkCursorHigh = trunkCursorLow.clone().jump(1);
        trunkCursorLow.place(floor).step();
        trunkCursorHigh.put(trunkCursorHigh.getBlock("oak_button").putProperty("face", "floor")).step();
        for (var i = 0; i < keys.length; ++i) {
            final var rotateTimes = 0 == i % 2 ? -1 : 1;
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.placeRepeater(2).step();
            trunkCursorLow.place("iron_block");
            trunkCursorHigh.placeNoteBlock(keys[i]);

            final var branchCursorLow1 = trunkCursorLow.clone().rotate(rotateTimes).step();
            for (final var dist : dist01) {
                final var prevCursorLow = branchCursorLow1.clone();
                final var repeaterCount = placeRedstoneWireLine(branchCursorLow1, floor, dist);
                branchCursorLow1.place("iron_block");
                branchCursorLow1.clone().jump(1).placeNoteBlock(keys[i]);
                branchCursorLow1.step();
                if (0 == repeaterCount) {
                    prevCursorLow.jump(1).placeRepeater(2);
                }
            }

            final var branchCursorLow2 = trunkCursorLow.clone().rotate(-rotateTimes).step();
            for (final var dist : dist02) {
                final var prevCursorLow = branchCursorLow2.clone();
                final var repeaterCount = placeRedstoneWireLine(branchCursorLow2, floor, dist);
                branchCursorLow2.place("iron_block");
                branchCursorLow2.clone().jump(1).placeNoteBlock(keys[i]);
                branchCursorLow2.step();
                if (0 == repeaterCount) {
                    prevCursorLow.jump(1).placeRepeater(2);
                } else {
                    branchCursorLow2.clone().jump(1).step(-2).placeRepeater(1);
                }
            }

            trunkCursorLow.step().place(floor).step();
            trunkCursorHigh.step().placeRepeater(2).step();
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.place(stub).step();
        }
        return structure;
    }

    private static Structure demoCymbal(BlockDatasetVersion version, int key) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var structure = new Structure();
        final int[] keys = { key };
        final int[] dist01 = { 24, 7, 5, 3, 1, 1 };
        final int[] dist02 = { 27, 6, 4, 2, 1, 1 };
        final var floor = "sand";
        final var stub = "redstone_lamp";
        final var trunkCursorLow = new Cursor(blockDataset, structure);
        final var trunkCursorHigh = trunkCursorLow.clone().jump(1);
        trunkCursorLow.place(floor).step();
        trunkCursorHigh.put(trunkCursorHigh.getBlock("oak_button").putProperty("face", "floor")).step();
        for (var i = 0; i < keys.length; ++i) {
            final var rotateTimes = 0 == i % 2 ? -1 : 1;
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.placeRepeater(2).step();
            trunkCursorLow.place("sand");
            trunkCursorHigh.placeNoteBlock(keys[i]);

            final var branchCursorLow1 = trunkCursorLow.clone().rotate(rotateTimes).step();
            for (final var dist : dist01) {
                final var prevCursorLow = branchCursorLow1.clone();
                final var repeaterCount = placeRedstoneWireLine(branchCursorLow1, floor, dist);
                branchCursorLow1.place("sand");
                branchCursorLow1.clone().jump(1).placeNoteBlock(keys[i]);
                branchCursorLow1.step();
                if (0 == repeaterCount) {
                    prevCursorLow.jump(1).placeRepeater(2);
                }
            }

            final var branchCursorLow2 = trunkCursorLow.clone().rotate(-rotateTimes).step();
            for (final var dist : dist02) {
                final var prevCursorLow = branchCursorLow2.clone();
                final var repeaterCount = placeRedstoneWireLine(branchCursorLow2, floor, dist);
                branchCursorLow2.place("sand");
                branchCursorLow2.clone().jump(1).placeNoteBlock(keys[i]);
                branchCursorLow2.step();
                if (0 == repeaterCount) {
                    prevCursorLow.jump(1).placeRepeater(2);
                } else {
                    branchCursorLow2.clone().jump(1).step(-2).placeRepeater(1);
                }
            }

            trunkCursorLow.step().place(floor).step();
            trunkCursorHigh.step().placeRepeater(2).step();
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.place(stub).step();
        }
        return structure;
    }

    private static void demoSpatialDelay(BlockDatasetVersion version, String outputFilePath) throws Exception {
        final var demoStructureList = List.of(
            demoNoDelay(VERSION),
            demoWithDelay(VERSION),
            demoReverb(VERSION),
            demoSustain(VERSION),
            demoCymbal(VERSION, 0),
            demoCymbal(VERSION, 12),
            demoCymbal(VERSION, 24)
        );
        final var structure = new Structure();
        for (final var demoStructure : demoStructureList) {
            demoStructure.regulate();
            demoStructure.translate(-demoStructure.getRange3().getX().length() / 2, 0, structure.getRange3().getZ().getMax() + 8);
            structure.paste(demoStructure);
        }
        structure.regulate();
        final var nbtWriter = NbtWriter.of(version);
        nbtWriter.write(structure, outputFilePath);
    }

    private static int placeRedstoneWireLine(Cursor cursor, String floor, int length) {
        var repeaterCount = 0;
        final var redstoneLimitLength = 15;
        final var sides = List.of(cursor.getFacing(), cursor.getFacing().back());
        while (redstoneLimitLength < length) {
            for (var i = 0; i < redstoneLimitLength; ++i) {
                cursor.place(floor).jump(1).placeRedstoneWire(sides).jump(-1).step();
            }
            cursor.place(floor).jump(1).placeRepeater(1).jump(-1).step();
            length -= (redstoneLimitLength + 1);
            ++repeaterCount;
        }
        for (var i = 0; i < length; ++i) {
            cursor.place(floor).jump(1).placeRedstoneWire(sides).jump(-1).step();
        }
        return repeaterCount;
    }

    public static void main(String[] args) {
        try {
            final var nbtWriter = NbtWriter.of(VERSION);

//            final var structure = oneBlock(VERSION, "note_block");
//            final var snbt = nbtWriter.dump(structure);
//            log.info("snbt : {}", snbt);

//            final var structure = watamePixelArt(VERSION);
//            final var outputFilePath = String.format("%s/%s/structure/watame-pixel-art.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            final var structure = beaconSpectrum(VERSION);
//            final var outputFilePath = String.format("%s/%s/structure/beacon-spectrum.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            filterSimpleColor(VERSION);

//            final var inputFilePath = String.format("%s/input/shishiro/shishiro.png", ROOT_DIR_PATH);
//            final var structure = shishiroPixelArt(VERSION, inputFilePath);
//            final var outputFilePath = String.format("%s/%s/structure/shishiro-pixel-art.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            final var inputDirPath = String.format("%s/input/bocchi/frames", ROOT_DIR_PATH);
//            final var outputDirPath = String.format("%s/%s/structure/bocchi", ROOT_DIR_PATH, VERSION.getMcVersion());
//            bocchiTheRockAnimation(VERSION, inputDirPath, outputDirPath);

//            final var structure = demoMapArtColor(VERSION);
//            final var outputFilePath = String.format("%s/%s/structure/map-art-color.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            final var inputFilePath = String.format("%s/input/uber-sheep/uber-sheep.jpg", ROOT_DIR_PATH);
//            final var structure = uberSheepMapArt(VERSION, inputFilePath);
//            final var outputFilePath = String.format("%s/%s/structure/uber-sheep-map-art.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            final var inputFilePath = String.format("%s/input/towa/towa-1.png", ROOT_DIR_PATH);
//            final var outputDirPath = String.format("%s/%s/structure/towa", ROOT_DIR_PATH, VERSION.getMcVersion());
//            towaMapArt(VERSION, inputFilePath, outputDirPath);

            final var outputDirPath = String.format("%s/%s/structure/delay-demo/spatial-delay.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
            demoSpatialDelay(VERSION, outputDirPath);
        } catch (Exception e) {
            log.error("PrefabUtils", e);
        }
    }

    private PrefabUtils() {}
}
