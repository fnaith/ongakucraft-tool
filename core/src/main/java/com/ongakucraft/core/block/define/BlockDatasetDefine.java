package com.ongakucraft.core.block.define;

import java.util.List;

import com.ongakucraft.core.block.Block;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class BlockDatasetDefine {
    public static BlockDatasetDefine of(String version,
                                        List<BlockPropertyDefine> blockPropertyDefineList,
                                        List<BlockDefine> blockDefineList) {
        return new BlockDatasetDefine(version, List.copyOf(blockPropertyDefineList), List.copyOf(blockDefineList));
    }

    @NonNull private final String version;
    @NonNull private final List<BlockPropertyDefine> blockPropertyDefineList;
    @NonNull private final List<BlockDefine> blockDefineList;

    public List<Block> generateBlockList() {
        return blockDefineList.stream().map(Block::of).toList();
    }
}
