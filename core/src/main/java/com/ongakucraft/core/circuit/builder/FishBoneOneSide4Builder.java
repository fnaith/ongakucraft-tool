package com.ongakucraft.core.circuit.builder;

import com.ongakucraft.core.block.BlockDataset;

public final class FishBoneOneSide4Builder {
    public static FishBoneOneSideBuilder of(BlockDataset blockDataset, boolean turnRight, String floorPath, String stubPath) {
        return new FishBoneOneSideBuilder(blockDataset, 4, turnRight, floorPath, stubPath);
    }
}
