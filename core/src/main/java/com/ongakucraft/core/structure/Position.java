package com.ongakucraft.core.structure;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.ongakucraft.core.block.Direction;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(cacheStrategy = LAZY)
@Getter
public final class Position {
    public static final Position ZERO = of(0, 0, 0);

    private final int x;
    private final int y;
    private final int z;

    public Position step(@NonNull Direction dir) {
        return step(dir, 1);
    }

    public Position step(@NonNull Direction dir, int times) {
        return translate(dir.getX() * times, dir.getY() * times, dir.getZ() * times);
    }

    public Position jump(int y) {
        return translate(0, y, 0);
    }

    public Position move(int x, int z) {
        return translate(x, 0 ,z);
    }

    public Position translate(int x, int y, int z) {
        return of(this.x + x, this.y + y, this.z + z);
    }

    public Position rotate(boolean clockwise) {
        return clockwise ? of(z, y, -x - 1) : of(-z - 1, y, x);
    }

    @Override
    public String toString() {
        return String.format("(%d,%d,%d)", x, y, z);
    }
}
