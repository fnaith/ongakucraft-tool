package com.ongakucraft.app.nbt;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import com.ongakucraft.app.data.DataLoadingApp;
import com.ongakucraft.app.graphics.GraphicUtils;
import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.Block;
import com.ongakucraft.core.block.BlockDatasetVersion;
import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.color.RgbColor;
import com.ongakucraft.core.prefab.AnimationBuilder;
import com.ongakucraft.core.prefab.BeaconSpectrumBuilder;
import com.ongakucraft.core.prefab.BlockColorFilter;
import com.ongakucraft.core.prefab.BlockColorFilterOption;
import com.ongakucraft.core.prefab.PixelArtBuilder;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Structure;

import lombok.extern.slf4j.Slf4j;

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

    private static void findColorToBlockIdMapByLabColor(String inputFilePath) throws Exception {
        final var image = GraphicUtils.toRgbImage(ImageIO.read(new File(inputFilePath)));
        final var blockDataset = DataLoadingApp.loadBlockDataset(VERSION);
        final var blockLabColorDefineList = blockDataset.getBlockLabColorDefineList().stream()
                .filter(blockLabColorDefine -> {
                   final var path = blockLabColorDefine.getId().getPath();
                   return !path.equals("smoker") &&
                           !path.equals("target") &&
                           !path.equals("shroomlight") &&
                           !path.equals("deepslate_bricks") &&
                           !path.endsWith("_stained_glass") &&
                           !path.endsWith("_glazed_terracotta") &&
                           !path.startsWith("end_stone")
                           ;
                }).toList();
        final var colorBlockIdMap = PixelArtBuilder.findColorToBlockIdMapByLabColor(image, blockLabColorDefineList, Direction.N);
        for (final var entry : colorBlockIdMap.entrySet()) {
            log.info("color : {}", entry.getKey());
            log.info("block id : {}", entry.getValue());
        }
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
        BlockColorFilter.filterSimpleColor(blockDataset.getBlockLabColorDefineList(), BlockColorFilterOption.DEFAULT);
    }

    private static Structure shishiroPixelArt(BlockDatasetVersion version, String inputFilePath) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var bufferedImage = ImageIO.read(new File(inputFilePath));
        final var scaledBufferedImage = GraphicUtils.scaleByHeight(bufferedImage, 192);
        final var image = GraphicUtils.toRgbImage(scaledBufferedImage);
        final var colorBlockList = BlockColorFilter.filterSimpleColor(blockDataset.getBlockLabColorDefineList(), BlockColorFilterOption.DEFAULT);
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
                final var bufferedImage = ImageIO.read(new File(framePath));
                final var scaledBufferedImage = GraphicUtils.scaleByHeight(bufferedImage, 64);
                imageList.add(GraphicUtils.toRgbImage(scaledBufferedImage));
            }
            final var blockDataset = DataLoadingApp.loadBlockDataset(version);
            final var nbtWriter = NbtWriter.of(version);
            final List<Block[][]> blockGridList = new ArrayList<>();
            final var colorBlockList = BlockColorFilter.filterColoredBlock(blockDataset.getBlockLabColorDefineList());
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

    public static void main(String[] args) {
        try {
            final var nbtWriter = NbtWriter.of(VERSION);

//            final var structure = oneBlock(VERSION, "note_block");
//            final var snbt = nbtWriter.dump(structure);
//            log.info("snbt : {}", snbt);

//            final var structure = watamePixelArt(VERSION);
//            final var outputFilePath = String.format("%s/%s/structure/watamePixelArt.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            final var structure = beaconSpectrum(VERSION);
//            final var outputFilePath = String.format("%s/%s/structure/beaconSpectrum.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            filterSimpleColor(VERSION);

//            final var inputFilePath = String.format("%s/input/shishiro/shishiro.png", ROOT_DIR_PATH);
//            final var structure = shishiroPixelArt(VERSION, inputFilePath);
//            final var outputFilePath = String.format("%s/%s/structure/shishiroPixelArt.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

            final var inputDirPath = String.format("%s/input/bocchi/frames", ROOT_DIR_PATH);
            final var outputDirPath = String.format("%s/%s/structure/bocchi", ROOT_DIR_PATH, VERSION.getMcVersion());
            bocchiTheRockAnimation(VERSION, inputDirPath, outputDirPath);
        } catch (Exception e) {
            log.error("PrefabUtils", e);
        }
    }

    private PrefabUtils() {}
}
