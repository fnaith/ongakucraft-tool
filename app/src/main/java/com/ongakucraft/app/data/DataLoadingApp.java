package com.ongakucraft.app.data;

import com.ongakucraft.core.block.define.BlockDatasetDefine;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DataLoadingApp {
    private static final String VERSION = "1.18.2";

    private static BlockDatasetDefine loadBlockDatasetDefine(String version) throws Exception {
        final var blockPropertyDefineList = ArticDataApp.generateBlockPropertyDefineList(version);
        final var blockDefineList = ArticDataApp.generateBlockDefineList(VERSION, blockPropertyDefineList);
        return BlockDatasetDefine.of(version, blockPropertyDefineList, blockDefineList);
    }

    public static void main(String[] args) {
        try {
            final var blockDatasetDefine = loadBlockDatasetDefine(VERSION);
            final var blockList = blockDatasetDefine.generateBlockList();
            log.info("blockList : {}", blockList.size());
        } catch (Exception e) {
            log.error("DataGenerationApp", e);
        }
    }

    private DataLoadingApp() {}
}
