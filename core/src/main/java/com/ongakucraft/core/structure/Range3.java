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
}
