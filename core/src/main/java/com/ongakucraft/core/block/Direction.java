package com.ongakucraft.core.block;

import com.ongakucraft.core.OcException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public enum Direction {
    E(1, 0, 0, "east", 5),
    W(-1, 0, 0, "west", 4),
    S(0, 0, 1, "south", 3),
    N(0, 0, -1, "north", 2),
    U(0, 1, 0, "up", 1),
    D(0, -1, 0, "down", 0);

    public static Direction of(@NonNull String text) {
        return switch (text) {
            case "east" -> E;
            case "west" -> W;
            case "south" -> S;
            case "north" -> N;
            case "up" -> U;
            case "down" -> D;
            default -> throw new OcException("invalid direction text : {}", text);
        };
    }

    private final int x;
    private final int y;
    private final int z;
    private final String text;
    private final int value;

    public Direction back() {
        return switch (this) {
            case E -> W;
            case W -> E;
            case S -> N;
            case N -> S;
            case U -> U;
            case D -> D;
        };
    }

    public Direction left() {
        return switch (this) {
            case E -> N;
            case W -> S;
            case S -> E;
            case N -> W;
            case U -> U;
            case D -> D;
        };
    }

    public Direction right() {
        return switch (this) {
            case E -> S;
            case W -> N;
            case S -> W;
            case N -> E;
            case U -> U;
            case D -> D;
        };
    }

    public Direction rotate(int times) {
        return switch (((times % 4 + 4) % 4)) {
            case 0 -> this;
            case 1 -> left();
            case 2 -> back();
            case 3 -> right();
            default -> throw new OcException("");
        };
    }

    @Override
    public String toString() {
        return text;
    }
}
