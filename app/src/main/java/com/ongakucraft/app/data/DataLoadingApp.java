package com.ongakucraft.app.data;

import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.block.define.BlockDatasetDefine;

import com.ongakucraft.core.block.define.BlockDatasetVersion;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DataLoadingApp {
    private static final BlockDatasetVersion VERSION = BlockDatasetVersion.of("1.18.2", 2860);

    public static BlockDatasetDefine loadBlockDatasetDefine(BlockDatasetVersion version) throws Exception {
        final var mcVersion = version.getMcVersion();
        final var blockPropertyDefineList = ArticDataApp.generateBlockPropertyDefineList(mcVersion);
        final var blockDefineList = ArticDataApp.generateBlockDefineList(mcVersion, blockPropertyDefineList);
        final var blockRgbColorDefineList = DataGenerationApp.loadBlockRgbColorDefineList(mcVersion);
        final var blockLabColorDefineList = DataGenerationApp.loadBlockLabColorDefineList(mcVersion);
        return BlockDatasetDefine.of(version, blockPropertyDefineList, blockDefineList,
                                     blockRgbColorDefineList, blockLabColorDefineList);
    }

    public static void main(String[] args) {
        try {
            final var blockDatasetDefine = loadBlockDatasetDefine(VERSION);
            final var blockMap = blockDatasetDefine.getBlockMap();
            log.info("blockMap : {}", blockMap.size());
            log.info("white_wool : {}", blockDatasetDefine.getLabColor(Direction.N, "white_wool"));
        } catch (Exception e) {
            log.error("DataGenerationApp", e);
        }
    }

    private DataLoadingApp() {}
}
