package com.ongakucraft.app.nbt;

import com.ongakucraft.app.data.DataLoadingApp;
import com.ongakucraft.core.block.define.BlockDatasetVersion;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Structure;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PrefabUtils {
    private static final String ROOT_DIR_PATH = "./data/generated";
    private static final BlockDatasetVersion VERSION = BlockDatasetVersion.of("1.18.2", 2860);

    private static Structure oneBlock(BlockDatasetVersion version, String path) throws Exception {
        final var blockDatasetDefine = DataLoadingApp.loadBlockDatasetDefine(version);
        final var block = blockDatasetDefine.getBlock(path);
        final var structure = new Structure();
        structure.put(Position.ZERO, block);
        return structure;
    }

    private static Structure wall(BlockDatasetVersion version, String csv) throws Exception {
        final var blockDatasetDefine = DataLoadingApp.loadBlockDatasetDefine(version);
        final var structure = new Structure();
        csv = csv.trim();
        final var rows = csv.split("\n");
        final var h = rows.length;
        for (var y = 0; y < h; ++y) {
            final var row = rows[h - 1 - y];
            final var cells = row.trim().split(",");
            for (var x = 0; x < cells.length; ++x) {
                final var path = cells[x];
                final var block = blockDatasetDefine.getBlock(path);
                final var position = Position.of(x, y, 0);
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
                0,1,2,1,5,4,4,4,5,1,2,1,0
                1,2,3,5,6,5,5,5,6,7,3,2,1
                1,3,5,6,9,6,6,6,9,6,5,3,1
                0,1,5,9,A,9,9,9,A,9,5,1,0
                1,5,6,9,B,9,9,9,B,9,6,5,1
                0,1,5,8,C,C,H,C,C,8,5,1,0
                0,1,5,1,0,D,I,D,0,6,5,1,0
                1,5,1,9,E,F,F,F,E,1,6,5,1
                1,5,1,1,8,D,D,D,8,0,1,5,1
                0,1,7,8,3,9,1,1,1,1,7,1,0
                0,0,1,0,1,1,0,0,0,0,1,0,0
                """
                        .replaceAll("0", "air")
                        .replaceAll("1", "black_wool")
                        .replaceAll("2", "light_gray_wool")
                        .replaceAll("3", "gray_wool")
                        .replaceAll("4", "end_stone")
                        .replaceAll("5", "sand")
                        .replaceAll("6", "stripped_birch_log")
                        .replaceAll("7", "pink_wool")
                        .replaceAll("8", "stripped_birch_log")
                        .replaceAll("9", "bone_block")
                        .replaceAll("A", "blue_concrete")
                        .replaceAll("B", "purpur_block")
                        .replaceAll("C", "white_terracotta")
                        .replaceAll("D", "white_wool")
                        .replaceAll("E", "clay")
                        .replaceAll("F", "bone_block")
                        .replaceAll("G", "stripped_jungle_log")
                        .replaceAll("H", "stripped_acacia_log")
                        .replaceAll("I", "red_wool")
        ;
        return wall(version, csv);
    }

    public static void main(String[] args) {
        try {
//            final var structure = oneBlock(VERSION, "note_block");
            final var structure = watamePixelArt(VERSION);
            final var nbtWriter = NbtWriter.of(VERSION);
            final var snbt = nbtWriter.dump(structure);
            log.info("snbt : {}", snbt);
            final var outputFilePath = String.format("%s/%s/structure/watamePixelArt.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
            nbtWriter.write(structure, outputFilePath);
        } catch (Exception e) {
            log.error("PrefabUtils", e);
        }
    }

    private PrefabUtils() {}
}
