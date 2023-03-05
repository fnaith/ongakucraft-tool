package com.ongakucraft.core.prefab;

import com.ongakucraft.core.block.Block;
import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Range;
import com.ongakucraft.core.structure.Range3;
import com.ongakucraft.core.structure.Structure;

import java.util.List;
import java.util.stream.Stream;

public final class BeaconSpectrumBuilder {
    private static final List<String> GLASS_PATH_LIST = Stream.of(
            "red", "orange", "yellow", "lime", "green", "cyan",
            "light_blue", "blue", "purple", "magenta", "pink", "white"
    ).map(path -> path + "_stained_glass").toList();

    public static Structure buildBeaconSpectrum(int layer, BlockDataset blockDataset) {
        final var structure = buildGlassWall(layer, blockDataset);
        structure.translate(0, 1, 0);
        final var width = structure.getRange3().getX().length();
        final var beacon = blockDataset.getBlock("beacon");
        structure.fill(Range3.of(Range.of(width), Range.UNIT, Range.UNIT), beacon);
        final var ironBlock = blockDataset.getBlock("iron_block");
        structure.translate(1, 1, 1);
        structure.fill(Range3.of(Range.of(width + 2), Range.UNIT, Range.of(3)), ironBlock);
        return structure;
    }

    private static Structure buildGlassWall(int layer, BlockDataset blockDataset) {
        final var structure = new Structure();
        final var glassPathCount = GLASS_PATH_LIST.size();
        for (var i = 0; i < glassPathCount - 1; ++i) {
            final var begin = blockDataset.getBlock(GLASS_PATH_LIST.get(i));
            final var end = blockDataset.getBlock(GLASS_PATH_LIST.get(i + 1));
            final var glassWall = buildGlassWall(layer, begin, end);
            final var width = glassWall.getRange3().getX().length();
            structure.translate(width, 0, 0);
            structure.paste(glassWall);
        }
        final var end = blockDataset.getBlock(GLASS_PATH_LIST.get(glassPathCount - 1));
        structure.translate(1, 0, 0);
        structure.fill(Range3.of(Range.UNIT, Range.of(layer + 1), Range.UNIT), end);
        return structure;
    }

    private static Structure buildGlassWall(int layer, Block begin, Block end) {
        final var structure = new Structure();
        final var width = 1 << layer;
        for (var i = 0; i < width; ++i) {
            final var x = width - 1 - i;
            for (var j = 0; j < layer; ++j) {
                final var y = j + 1;
                final var position = Position.of(x, y, 0);
                final var block = 0 == (i & (1 << j)) ? begin : end;
                structure.put(position, block);
            }
            final var position = Position.of(x, 0, 0);
            structure.put(position, begin);
        }
        return structure;
    }

    private BeaconSpectrumBuilder() {}
}
