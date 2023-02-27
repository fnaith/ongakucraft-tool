package com.ongakucraft.core.prefab;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ongakucraft.core.block.Block;
import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.block.define.BlockLabColorDefine;
import com.ongakucraft.core.color.LabColor;
import com.ongakucraft.core.color.RgbColor;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Structure;

public final class PixelArtBuilder {
    public static Block[][] frontWallBlockGrid(RgbColor[][] image, List<BlockLabColorDefine> blockLabColorDefineList, BlockDataset blockDataset) {
        return toBlockGrid(image, blockLabColorDefineList, Direction.N, blockDataset);
    }

    public static Structure frontWall(Block[][] blockGrid) {
        final var structure = new Structure();
        final var h = blockGrid.length;
        final var w = blockGrid[0].length;
        for (var y = 0; y < h; ++y) {
            for (var x = 0; x < w; ++x) {
                final var block = blockGrid[y][x];
                if (null != block) {
                    final var position = Position.of(w - 1 - x, h - 1 - y, 0);
                    structure.put(position, block);
                }
            }
        }
        return structure;
    }

    public static Block[][] sideWallBlockGrid(RgbColor[][] image, List<BlockLabColorDefine> blockLabColorDefineList, BlockDataset blockDataset) {
        return toBlockGrid(image, blockLabColorDefineList, Direction.E, blockDataset);
    }

    public static Structure sideWall(Block[][] blockGrid) {
        final var structure = new Structure();
        final var h = blockGrid.length;
        final var w = blockGrid[0].length;
        for (var y = 0; y < h; ++y) {
            for (var x = 0; x < w; ++x) {
                final var block = blockGrid[y][x];
                if (null != block) {
                    final var position = Position.of(w - 1 - x, h - 1 - y, 0);
                    structure.put(position, block);
                }
            }
        }
        return structure;
    }

    private static Block[][] toBlockGrid(RgbColor[][] image, List<BlockLabColorDefine> blockLabColorDefineList,
                                         Direction facing, BlockDataset blockDataset) {
        final var colorToBlockId = findColorToBlockIdMapByLabColor(image, blockLabColorDefineList, facing);
        final var h = image.length;
        final var w = image[0].length;
        final var blockGrid = new Block[h][];
        for (var y = 0; y < h; ++y) {
            blockGrid[y] = new Block[w];
            for (var x = 0; x < w; ++x) {
                final var rgbColor = image[y][x];
                if (null != rgbColor && colorToBlockId.containsKey(rgbColor)) {
                    final var blockId = colorToBlockId.get(rgbColor);
                    blockGrid[y][x] = blockDataset.getBlock(blockId).withFacing(facing);
                }
            }
        }
        return blockGrid;
    }

    public static Map<RgbColor, BlockId> findColorToBlockIdMapByLabColor(RgbColor[][] image, List<BlockLabColorDefine> blockLabColorDefineList,
                                                                         Direction facing) {
        final Map<RgbColor, BlockId> colorToBlockId = new HashMap<>();
        for (final var row : image) {
            for (final var rgbColor : row) {
                if (null == rgbColor) {
                    continue;
                }
                final var blockIdOptional = findBlockIdByLabColor(rgbColor, facing, blockLabColorDefineList);
                blockIdOptional.ifPresent(blockId -> colorToBlockId.put(rgbColor, blockId));
            }
        }
        return colorToBlockId;
    }

    private static Optional<BlockId> findBlockIdByLabColor(RgbColor rgbColor, Direction facing, List<BlockLabColorDefine> blockLabColorDefineList) {
        final var targetColor = LabColor.of(rgbColor);
        var minDeviation = Double.MAX_VALUE;
        BlockId minBlockId = null;
        for (final var blockLabColorDefine : blockLabColorDefineList) {
            final var blockColor = blockLabColorDefine.getColors().get(facing);
            final var deviation = targetColor.distance(blockColor);
            if (deviation < minDeviation) {
                minDeviation = deviation;
                minBlockId = blockLabColorDefine.getId();
            }
        }
        return Optional.ofNullable(minBlockId);
    }

    private PixelArtBuilder() {}
}
