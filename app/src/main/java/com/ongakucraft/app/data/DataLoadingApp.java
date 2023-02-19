package com.ongakucraft.app.data;

import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.block.BlockDatasetVersion;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DataLoadingApp {
    private static final BlockDatasetVersion VERSION = BlockDatasetVersion.of("1.18.2", 2975);

    public static BlockDataset loadBlockDataset(BlockDatasetVersion version) throws Exception {
        final var mcVersion = version.getMcVersion();
        final var blockPropertyDefineList = ArticDataApp.generateBlockPropertyDefineList(mcVersion);
        final var blockDefineList = ArticDataApp.generateBlockDefineList(mcVersion, blockPropertyDefineList);
        final var blockRgbColorDefineList = DataGenerationApp.loadBlockRgbColorDefineList(mcVersion);
        final var blockLabColorDefineList = DataGenerationApp.loadBlockLabColorDefineList(mcVersion);
        return BlockDataset.of(version, blockPropertyDefineList, blockDefineList,
                               blockRgbColorDefineList, blockLabColorDefineList);
    }

    public static void main(String[] args) {
        try {
            final var blockDataset = loadBlockDataset(VERSION);
            final var blockMap = blockDataset.getBlockMap();
            log.info("blockMap : {}", blockMap.size());
            log.info("white_wool : {}", blockDataset.getLabColor(Direction.N, "white_wool"));
        } catch (Exception e) {
            log.error("DataGenerationApp", e);
        }
    }

    private DataLoadingApp() {}
}
