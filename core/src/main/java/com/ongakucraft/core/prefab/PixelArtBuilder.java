package com.ongakucraft.core.prefab;

import com.ongakucraft.core.block.Block;
import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.block.color.BlockLabColor;
import com.ongakucraft.core.color.LabColor;
import com.ongakucraft.core.color.RgbColor;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Structure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PixelArtBuilder {
    public static Block[][] frontWallBlockGrid(RgbColor[][] image, List<BlockLabColor> blockLabColorList, BlockDataset blockDataset) {
        return toBlockGrid(image, blockLabColorList, Direction.N, blockDataset);
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

    public static Block[][] sideWallBlockGrid(RgbColor[][] image, List<BlockLabColor> blockLabColorList, BlockDataset blockDataset) {
        return toBlockGrid(image, blockLabColorList, Direction.E, blockDataset);
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

    private static Block[][] toBlockGrid(RgbColor[][] image, List<BlockLabColor> blockLabColorList,
                                         Direction facing, BlockDataset blockDataset) {
        final var colorToBlockId = findColorToBlockIdMapByLabColor(image, blockLabColorList, facing);
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

    private static Map<RgbColor, BlockId> findColorToBlockIdMapByLabColor(RgbColor[][] image, List<BlockLabColor> blockLabColorList,
                                                                         Direction facing) {
        final Map<RgbColor, BlockId> colorToBlockId = new HashMap<>();
        for (final var row : image) {
            for (final var rgbColor : row) {
                if (null == rgbColor || colorToBlockId.containsKey(rgbColor)) {
                    continue;
                }
                final var blockIdOptional = findBlockIdByLabColor(rgbColor, facing, blockLabColorList);
                blockIdOptional.ifPresent(blockId -> colorToBlockId.put(rgbColor, blockId));
            }
        }
        return colorToBlockId;
    }

    private static Optional<BlockId> findBlockIdByLabColor(RgbColor rgbColor, Direction facing, List<BlockLabColor> blockLabColorList) {
        final var targetColor = LabColor.of(rgbColor);
        var minDeviation = Double.MAX_VALUE;
        BlockId minBlockId = null;
        for (final var blockLabColor : blockLabColorList) {
            final var blockColor = blockLabColor.getColors().get(facing);
            final var deviation = targetColor.distance(blockColor);
            if (deviation < minDeviation) {
                minDeviation = deviation;
                minBlockId = blockLabColor.getId();
            }
        }
        return Optional.ofNullable(minBlockId);
    }

    private PixelArtBuilder() {}
}
