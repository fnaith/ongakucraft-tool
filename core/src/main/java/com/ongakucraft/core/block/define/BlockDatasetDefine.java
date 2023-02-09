package com.ongakucraft.core.block.define;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ongakucraft.core.block.Block;
import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.color.LabColor;
import com.ongakucraft.core.color.RgbColor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class BlockDatasetDefine {
    public static BlockDatasetDefine of(BlockDatasetVersion version,
                                        List<BlockPropertyDefine> blockPropertyDefineList,
                                        List<BlockDefine> blockDefineList,
                                        List<BlockRgbColorDefine> blockRgbColorDefineList,
                                        List<BlockLabColorDefine> blockLabColorDefineList) {
        final var blockMap = blockDefineList.stream().collect(Collectors.toMap(BlockDefine::getId, Block::of));
        final var rgbColorMap = blockRgbColorDefineList.stream().collect(Collectors.toMap(BlockRgbColorDefine::getId, Function.identity()));
        final var labColorMap = blockLabColorDefineList.stream().collect(Collectors.toMap(BlockLabColorDefine::getId, Function.identity()));
        return new BlockDatasetDefine(version, List.copyOf(blockPropertyDefineList), Collections.unmodifiableMap(blockMap),
                                      Collections.unmodifiableMap(rgbColorMap), Collections.unmodifiableMap(labColorMap));
    }

    @NonNull private final BlockDatasetVersion version;
    @NonNull private final List<BlockPropertyDefine> blockPropertyDefineList;
    @NonNull private final Map<BlockId, Block> blockMap;
    @NonNull private final Map<BlockId, BlockRgbColorDefine> rgbColorDefineMap;
    @NonNull private final Map<BlockId, BlockLabColorDefine> labColorDefineMap;

    public Block getBlock(String path) {
        return blockMap.get(BlockId.of(path));
    }

    public RgbColor getRgbColor(Direction direction, String path) {
        return rgbColorDefineMap.get(BlockId.of(path)).getColors().get(direction);
    }

    public LabColor getLabColor(Direction direction, String path) {
        return labColorDefineMap.get(BlockId.of(path)).getColors().get(direction);
    }
}
