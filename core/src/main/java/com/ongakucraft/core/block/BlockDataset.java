package com.ongakucraft.core.block;

import com.ongakucraft.core.block.color.BlockLabColor;
import com.ongakucraft.core.block.color.BlockMapColor;
import com.ongakucraft.core.block.color.BlockRgbColor;
import com.ongakucraft.core.block.define.BlockDefine;
import com.ongakucraft.core.block.define.BlockPropertyDefine;
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
                                  List<BlockRgbColor> blockRgbColorList,
                                  List<BlockLabColor> blockLabColorList,
                                  List<BlockMapColor> blockMapColorList) {
        final var blockList = blockDefineList.stream().map(Block::of).toList();
        final var blockMapBaseColorList = blockMapColorList.stream().filter(blockMapColor -> 0 == blockMapColor.getGradient()).toList();
        final var blockPropertyDefineMap = blockPropertyDefineList.stream().collect(Collectors.toMap(BlockPropertyDefine::getId, Function.identity()));
        final var blockDefineMap = blockDefineList.stream().collect(Collectors.toMap(BlockDefine::getId, Function.identity()));
        final var blockMap = blockList.stream().collect(Collectors.toMap(Block::getId, Function.identity()));
        final var rgbColorMap = blockRgbColorList.stream().collect(Collectors.toMap(BlockRgbColor::getId, Function.identity()));
        final var labColorMap = blockLabColorList.stream().collect(Collectors.toMap(BlockLabColor::getId, Function.identity()));
        return new BlockDataset(version,
                                Collections.unmodifiableList(blockPropertyDefineList),
                                Collections.unmodifiableList(blockDefineList),
                                blockList,
                                Collections.unmodifiableList(blockRgbColorList),
                                Collections.unmodifiableList(blockLabColorList),
                                Collections.unmodifiableList(blockMapBaseColorList),
                                Collections.unmodifiableList(blockMapColorList),
                                Collections.unmodifiableMap(blockPropertyDefineMap),
                                Collections.unmodifiableMap(blockDefineMap),
                                Collections.unmodifiableMap(blockMap),
                                Collections.unmodifiableMap(rgbColorMap),
                                Collections.unmodifiableMap(labColorMap));
    }

    @NonNull private final BlockDatasetVersion version;
    @NonNull private final List<BlockPropertyDefine> blockPropertyDefineList;
    @NonNull private final List<BlockDefine> blockDefineList;
    @NonNull private final List<Block> blockList;
    @NonNull private final List<BlockRgbColor> blockRgbColorList;
    @NonNull private final List<BlockLabColor> blockLabColorList;
    @NonNull private final List<BlockMapColor> blockMapBaseColorList;
    @NonNull private final List<BlockMapColor> blockMapColorList;
    @NonNull private final Map<String, BlockPropertyDefine> blockPropertyDefineMap;
    @NonNull private final Map<BlockId, BlockDefine> blockDefineMap;
    @NonNull private final Map<BlockId, Block> blockMap;
    @NonNull private final Map<BlockId, BlockRgbColor> rgbColorMap;
    @NonNull private final Map<BlockId, BlockLabColor> labColorMap;

    public Block getBlock(String path) {
        return getBlock(BlockId.of(path));
    }

    public Block getBlock(BlockId blockId) {
        return blockMap.get(blockId);
    }

    public BlockDefine getBlockDefine(BlockId blockId) {
        return blockDefineMap.get(blockId);
    }

    public RgbColor getRgbColor(Direction direction, String path) {
        return rgbColorMap.get(BlockId.of(path)).getColors().get(direction);
    }

    public LabColor getLabColor(Direction direction, String path) {
        return labColorMap.get(BlockId.of(path)).getColors().get(direction);
    }
}
