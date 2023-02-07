package com.ongakucraft.core.structure;

import com.ongakucraft.core.block.Direction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public final class Cursor {
    @NonNull private Position position;
    @NonNull private Direction facing;

    public Cursor() {
        this(Position.ZERO, Direction.N);
    }

    public void step(@NonNull Direction dir) {
        position = position.step(dir, 1);
    }

    public void step(@NonNull Direction dir, int times) {
        position = position.step(dir, times);
    }

    public void jump(int y) {
        position = position.jump(y);
    }

    public void move(int x, int z) {
        position = position.move(x, z);
    }

    public void translate(int x, int y, int z) {
        position = position.translate(x, y ,z);
    }

    public void back() {
        facing = facing.back();
    }

    public void left() {
        facing = facing.left();
    }

    public void right() {
        facing = facing.right();
    }

    public void rotate(boolean clockwise) {
        facing = facing.rotate(clockwise);
    }
}
