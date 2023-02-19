package com.ongakucraft.app.nbt;

import com.ongakucraft.app.data.DataLoadingApp;
import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.block.BlockDatasetVersion;
import com.ongakucraft.core.color.RgbColor;
import com.ongakucraft.core.prefab.BeaconSpectrumBuilder;
import com.ongakucraft.core.prefab.PixelArtBuilder;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Structure;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

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

    private static RgbColor[][] toRgbImage(BufferedImage bufferedImage) {
        final var h = bufferedImage.getHeight();
        final var w = bufferedImage.getWidth();
        final var image = new RgbColor[h][];
        for (var y = 0; y < h; ++y) {
            image[y] = new RgbColor[w];
            for (var x = 0; x < w; ++x) {
                if (0 == (bufferedImage.getRGB(x,y) >> 24)) {
                    continue;
                }
                final var color = new Color(bufferedImage.getRGB(x, y), true);
                image[y][x] = RgbColor.of(color.getRed(), color.getGreen(), color.getBlue());
            }
        }
        return image;
    }

    private static void findColorToBlockIdMapByLabColor(String inputFilePath) throws Exception {
        final var image = toRgbImage(ImageIO.read(new File(inputFilePath)));
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

    public static void main(String[] args) {
        try {
            final var nbtWriter = NbtWriter.of(VERSION);

//            final var structure = oneBlock(VERSION, "note_block");
//            final var snbt = nbtWriter.dump(structure);
//            log.info("snbt : {}", snbt);

//            final var structure = watamePixelArt(VERSION);
//            final var outputFilePath = String.format("%s/%s/structure/watamePixelArt.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

            final var structure = beaconSpectrum(VERSION);
            final var outputFilePath = String.format("%s/%s/structure/beaconSpectrum.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
            nbtWriter.write(structure, outputFilePath);
        } catch (Exception e) {
            log.error("PrefabUtils", e);
        }
    }

    private PrefabUtils() {}
}
