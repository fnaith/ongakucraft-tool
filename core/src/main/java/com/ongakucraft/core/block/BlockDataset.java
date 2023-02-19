package com.ongakucraft.core.block;

import com.ongakucraft.core.block.define.BlockDefine;
import com.ongakucraft.core.block.define.BlockLabColorDefine;
import com.ongakucraft.core.block.define.BlockPropertyDefine;
import com.ongakucraft.core.block.define.BlockRgbColorDefine;
import com.ongakucraft.core.color.LabColor;
import com.ongakucraft.core.color.RgbColor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class BlockDataset {
    public static BlockDataset of(BlockDatasetVersion version,
                                  List<BlockPropertyDefine> blockPropertyDefineList,
                                  List<BlockDefine> blockDefineList,
                                  List<BlockRgbColorDefine> blockRgbColorDefineList,
                                  List<BlockLabColorDefine> blockLabColorDefineList) {
        final var blockList = blockDefineList.stream().map(Block::of).toList();
        final var blockPropertyDefineMap = blockPropertyDefineList.stream().collect(Collectors.toMap(BlockPropertyDefine::getId, Function.identity()));
        final var blockMap = blockList.stream().collect(Collectors.toMap(Block::getId, Function.identity()));
        final var rgbColorMap = blockRgbColorDefineList.stream().collect(Collectors.toMap(BlockRgbColorDefine::getId, Function.identity()));
        final var labColorMap = blockLabColorDefineList.stream().collect(Collectors.toMap(BlockLabColorDefine::getId, Function.identity()));
        return new BlockDataset(version,
                                Collections.unmodifiableList(blockPropertyDefineList),
                                blockList,
                                Collections.unmodifiableList(blockRgbColorDefineList),
                                Collections.unmodifiableList(blockLabColorDefineList),
                                Collections.unmodifiableMap(blockPropertyDefineMap),
                                Collections.unmodifiableMap(blockMap),
                                Collections.unmodifiableMap(rgbColorMap),
                                Collections.unmodifiableMap(labColorMap));
    }

    @NonNull private final BlockDatasetVersion version;
    @NonNull private final List<BlockPropertyDefine> blockPropertyDefineList;
    @NonNull private final List<Block> blockList;
    @NonNull private final List<BlockRgbColorDefine> blockRgbColorDefineList;
    @NonNull private final List<BlockLabColorDefine> blockLabColorDefineList;
    @NonNull private final Map<String, BlockPropertyDefine> blockPropertyDefineMap;
    @NonNull private final Map<BlockId, Block> blockMap;
    @NonNull private final Map<BlockId, BlockRgbColorDefine> rgbColorDefineMap;
    @NonNull private final Map<BlockId, BlockLabColorDefine> labColorDefineMap;

    public Block getBlock(String path) {
        return getBlock(BlockId.of(path));
    }

    public Block getBlock(BlockId blockId) {
        return blockMap.get(blockId);
    }

    public RgbColor getRgbColor(Direction direction, String path) {
        return rgbColorDefineMap.get(BlockId.of(path)).getColors().get(direction);
    }

    public LabColor getLabColor(Direction direction, String path) {
        return labColorDefineMap.get(BlockId.of(path)).getColors().get(direction);
    }
}
