package com.ongakucraft.core.prefab;

import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.block.define.BlockLabColorDefine;
import com.ongakucraft.core.color.LabColor;
import com.ongakucraft.core.color.RgbColor;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Structure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PixelArtBuilder {
    public static Structure frontWall(RgbColor[][] image, BlockDataset blockDataset, BlockId transparent) {
        final var structure = new Structure();
        final var h = image.length;
        final var w = image[0].length;
        final var blockIdGrid = toBlockIdGrid(image, blockDataset.getBlockLabColorDefineList(), Direction.N, transparent);
        for (var y = 0; y < h; ++y) {
            for (var x = 0; x < w; ++x) {
                if (null != blockIdGrid[y][x]) {
                    final var position = Position.of(w - 1 - x, h - 1 - y, 0);
                    final var block = blockDataset.getBlock(blockIdGrid[y][x]);
                    structure.put(position, block);
                }
            }
        }
        return structure;
    }

    public static Structure sideWall(RgbColor[][] image, BlockDataset blockDataset, BlockId transparent) {
        final var structure = new Structure();
        final var h = image.length;
        final var w = image[0].length;
        final var blockIdGrid = toBlockIdGrid(image, blockDataset.getBlockLabColorDefineList(), Direction.N.right(), transparent);
        for (var y = 0; y < h; ++y) {
            for (var x = 0; x < w; ++x) {
                if (null != blockIdGrid[y][x]) {
                    final var position = Position.of(w - 1 - x, h - 1 - y, 0);
                    final var block = blockDataset.getBlock(blockIdGrid[y][x]);
                    structure.put(position, block.right());
                }
            }
        }
        return structure;
    }

    public static Structure floor(RgbColor[][] image, BlockDataset blockDataset, BlockId transparent) {
        final var structure = new Structure();
        final var h = image.length;
        final var w = image[0].length;
        final var blockIdGrid = toBlockIdGrid(image, blockDataset.getBlockLabColorDefineList(), Direction.U, transparent);
        for (var y = 0; y < h; ++y) {
            for (var x = 0; x < w; ++x) {
                if (null != blockIdGrid[y][x]) {
                    final var position = Position.of(w - 1 - x, 0, h - 1 - y);
                    final var block = blockDataset.getBlock(blockIdGrid[y][x]);
                    structure.put(position, block.right());
                }
            }
        }
        return structure;
    }

    public static BlockId[][] toBlockIdGrid(RgbColor[][] image, List<BlockLabColorDefine> blockLabColorDefineList,
                                            Direction facing, BlockId transparent) {
        final var colorToBlockId = findColorToBlockIdMapByLabColor(image, blockLabColorDefineList, facing);
        final var h = image.length;
        final var w = image[0].length;
        final var blockIdGrid = new BlockId[h][];
        for (var y = 0; y < h; ++y) {
            blockIdGrid[y] = new BlockId[w];
            for (var x = 0; x < w; ++x) {
                final var rgbColor = image[y][x];
                final var blockId = (null == rgbColor || !colorToBlockId.containsKey(rgbColor)) ? transparent : colorToBlockId.get(rgbColor);
                blockIdGrid[y][x] = blockId;
            }
        }
        return blockIdGrid;
    }

    public static Map<RgbColor, BlockId> findColorToBlockIdMapByLabColor(RgbColor[][] image, List<BlockLabColorDefine> blockLabColorDefineList, Direction facing) {
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
