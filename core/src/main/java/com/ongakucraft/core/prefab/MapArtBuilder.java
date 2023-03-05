package com.ongakucraft.core.prefab;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.block.color.BlockMapColor;
import com.ongakucraft.core.color.LabColor;
import com.ongakucraft.core.color.RgbColor;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Structure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MapArtBuilder {
    public static final int BLOCK_LENGTH_PER_MAP = 128;
    public static Structure build(RgbColor[][] image, List<BlockMapColor> blockMapColorList, BlockDataset blockDataset) {
        final var structure = new Structure();
        final var h = image.length;
        if (BLOCK_LENGTH_PER_MAP != h) {
            throw new OcException("height of image should be %d : %d", BLOCK_LENGTH_PER_MAP, h);
        }
        final var w = image[0].length;
        if (BLOCK_LENGTH_PER_MAP != w) {
            throw new OcException("width of image should be %s : %d", BLOCK_LENGTH_PER_MAP, w);
        }
        for (var y = 0; y < h; ++y) {
            for (var x = 0; x < w; ++x) {
                final var rgbColor = image[y][x];
                if (null == rgbColor) {
                    throw new OcException("image should not have null pixel : %d %d", x, y);
                }
            }
        }
        final var rgbToMapColor = findMapColorByLabColor(image, blockMapColorList);
        final var grassBlock = blockDataset.getBlock("grass_block");
        for (var x = 0; x < w; ++x) {
            final var slice = new Structure();
            var y = 0;
            slice.put(Position.of(x, y, -1), grassBlock);
            for (var z = 0; z < h; ++z) {
                final var rgbColor = image[z][x];
                final var mapColor = rgbToMapColor.get(rgbColor);
                y += mapColor.getGradient();
                final var position = Position.of(x, y, z);
                final var block = blockDataset.getBlock(mapColor.getId());
                if (mapColor.getId().getPath().endsWith("_leaves")) {
                    slice.put(position, block.put("persistent", true));
                } else {
                    slice.put(position, block);
                }
                if (!blockDataset.getBlockDefine(block.getId()).isCollisionShapeFullBlock()) {
                    slice.put(position.jump(-1), grassBlock);
                }
            }
            slice.regulate();
            slice.translate(x, 0, -1);
            structure.paste(slice);
        }
        return surroundWater(structure, blockDataset);
    }

    public static Structure surroundWater(Structure structure, BlockDataset blockDataset) {
        structure = structure.clone();
        final var barrierBlock = blockDataset.getBlock("barrier");
        final var dirs = List.of(Direction.E, Direction.S, Direction.W, Direction.N);
        for (final var position : structure.getPositionList()) {
            final var block = structure.get(position);
            if ("water".equals(block.getId().getPath())) {
                for (final var dir : dirs) {
                    final var neighborPosition = position.step(dir, 1);
                    if (!structure.has(neighborPosition)) {
                        structure.put(neighborPosition, barrierBlock);
                    }
                }
            }
        }
        return structure;
    }

    private static Map<RgbColor, BlockMapColor> findMapColorByLabColor(RgbColor[][] image, List<BlockMapColor> blockMapColorList) {
        final Map<RgbColor, BlockMapColor> rgbToMapColor = new HashMap<>();
        for (final var row : image) {
            for (final var rgbColor : row) {
                if (null == rgbColor || rgbToMapColor.containsKey(rgbColor)) {
                    continue;
                }
                final var blockMapColorOptional = findBlockIdByLabColor(rgbColor, blockMapColorList);
                blockMapColorOptional.ifPresent(blockId -> rgbToMapColor.put(rgbColor, blockId));
            }
        }
        return rgbToMapColor;
    }

    private static Optional<BlockMapColor> findBlockIdByLabColor(RgbColor rgbColor, List<BlockMapColor> blockMapColorList) {
        final var targetColor = LabColor.of(rgbColor);
        var minDeviation = Double.MAX_VALUE;
        BlockMapColor minBlockMapColor = null;
        for (final var blockMapColor : blockMapColorList) {
            final var blockColor = blockMapColor.getLabColor();
            final var deviation = targetColor.distance(blockColor);
            if (deviation < minDeviation) {
                minDeviation = deviation;
                minBlockMapColor = blockMapColor;
            }
        }
        return Optional.ofNullable(minBlockMapColor);
    }

    private MapArtBuilder() {}
}
