package com.ongakucraft.app.data;

import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.block.define.BlockDatasetDefine;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DataLoadingApp {
    private static final String VERSION = "1.18.2";

    private static BlockDatasetDefine loadBlockDatasetDefine(String version) throws Exception {
        final var blockPropertyDefineList = ArticDataApp.generateBlockPropertyDefineList(version);
        final var blockDefineList = ArticDataApp.generateBlockDefineList(version, blockPropertyDefineList);
        final var blockRgbColorDefineList = DataGenerationApp.loadBlockRgbColorDefineList(version);
        final var blockLabColorDefineList = DataGenerationApp.loadBlockLabColorDefineList(version);
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
