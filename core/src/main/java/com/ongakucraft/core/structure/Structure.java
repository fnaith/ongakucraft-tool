package com.ongakucraft.core.structure;

import com.ongakucraft.core.block.Block;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class Structure implements Cloneable {
    Map<Position, Block> grid;

    public Structure() {
        this(new HashMap<>());
    }

    private Structure(Map<Position, Block> grid) {
        this.grid = grid;
    }

    @Override
    public Structure clone() {
        return new Structure(new HashMap<>(grid));
    }

    public void put(@NonNull Position pos, @NonNull Block t) {
        grid.put(pos, t);
    }

    public Block get(@NonNull Position position) {
        return grid.get(position);
    }

    public Block remove(@NonNull Position position) {
        return grid.remove(position);
    }

    public Range3 getRange3() {
        if (grid.isEmpty()) {
            return Range3.EMPTY;
        }
        var xMin = Integer.MAX_VALUE;
        var xMax = Integer.MIN_VALUE;
        var yMin = Integer.MAX_VALUE;
        var yMax = Integer.MIN_VALUE;
        var zMin = Integer.MAX_VALUE;
        var zMax = Integer.MIN_VALUE;
        for (final var pos : grid.keySet()) {
            xMin = Math.min(xMin, pos.getX());
            xMax = Math.max(xMax, pos.getX());
            yMin = Math.min(yMin, pos.getY());
            yMax = Math.max(yMax, pos.getY());
            zMin = Math.min(zMin, pos.getZ());
            zMax = Math.max(zMax, pos.getZ());
        }
        return Range3.of(Range.of(xMin, xMax + 1), Range.of(yMin, yMax + 1), Range.of(zMin, zMax + 1));
    }

    public void translate(int x, int y, int z) {
        grid = mapGrid(entry -> entry.getKey().translate(x, y, z), Map.Entry::getValue);
    }

    public void rotate(int times) {
        grid = mapGrid(entry -> entry.getKey().rotate(times), entry -> entry.getValue().rotate(times));
    }

    public void regulate() {
        final var range3 = getRange3();
        translate(-range3.getX().getMin(), -range3.getY().getMin(), -range3.getZ().getMin());
    }

    public Structure cut(@NonNull Range3 range3) {
        final var newGrid = copyGrid(range3);
        grid.keySet().removeAll(newGrid.keySet());
        return new Structure(newGrid);
    }

    public Structure copy(@NonNull Range3 range3) {
        final var newGrid = copyGrid(range3);
        return new Structure(newGrid);
    }

    public void fill(@NonNull Range3 range3, @NonNull Block block) {
        for (var x = range3.getX().getStart(); x < range3.getX().getStop(); ++x) {
            for (var y = range3.getY().getStart(); y < range3.getY().getStop(); ++y) {
                for (var z = range3.getZ().getStart(); z < range3.getZ().getStop(); ++z) {
                    final var position = Position.of(x, y, z);
                    grid.put(position, block);
                }
            }
        }
    }

    public void paste(@NonNull Structure src) {
        grid.putAll(src.grid);
    }

    public void mirror() {
        // TODO x-axis
    }

    public void flip() {
        // TODO x-axis
    }

    public boolean isOverlapping(@NonNull Structure other) {
        return grid.keySet().stream().anyMatch(other.grid.keySet()::contains);
    }

    private Map<Position, Block> copyGrid(Range3 range3) {
        return filterGrid(entry -> range3.contains(entry.getKey()));
    }

    private Map<Position, Block> filterGrid(Predicate<Map.Entry<Position, Block>> predicate) {
        return grid.entrySet().stream().filter(predicate).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<Position, Block> mapGrid(Function<Map.Entry<Position, Block>, Position> positionMapper,
                                         Function<Map.Entry<Position, Block>, Block> blockMapper) {
        final Map<Position, Block> newGrid = new HashMap<>();
        for (final var entry : grid.entrySet()) {
            final var block = blockMapper.apply(entry);
            if (null != block) {
                final var position = positionMapper.apply(entry);
                newGrid.put(position, block);
            }
        }
        return newGrid;
    }
}
