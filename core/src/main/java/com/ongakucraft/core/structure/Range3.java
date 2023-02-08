package com.ongakucraft.core.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@AllArgsConstructor(staticName = "of")
@Getter
@ToString
public final class Range3 {
    public static final Range3 EMPTY = of(Range.EMPTY, Range.EMPTY, Range.EMPTY);

    @NonNull private final Range x;
    @NonNull private final Range y;
    @NonNull private final Range z;

    public boolean contains(Position position) {
        return x.contains(position.getX()) && y.contains(position.getY()) && z.contains(position.getZ());
    }

    public Range3 translate(int x, int y, int z) {
        return of(this.x.translate(x), this.y.translate(y), this.z.translate(z));
    }

    public Range3 rotate(int times) {
        if (0 == times % 4) {
            return this;
        }
        final var minPosition = Position.of(x.getMin(), 0, z.getMin()).rotate(times);
        final var maxPosition = Position.of(x.getMax(), 0, z.getMax()).rotate(times);
        final var xMin = Math.min(minPosition.getX(), maxPosition.getX());
        final var xMax = Math.max(minPosition.getX(), maxPosition.getX());
        final var zMin = Math.min(minPosition.getZ(), maxPosition.getZ());
        final var zMax = Math.max(minPosition.getZ(), maxPosition.getZ());
        return of(Range.of(xMin, xMax + 1), y, Range.of(zMin, zMax + 1));
    }
}
