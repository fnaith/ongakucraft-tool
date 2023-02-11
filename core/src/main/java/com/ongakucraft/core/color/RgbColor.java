package com.ongakucraft.core.color;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.ongakucraft.core.OcException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = LAZY)
@Getter
@ToString
public final class RgbColor {
    public static RgbColor of(int r, int g, int b) {
        validateValue(r, "r");
        validateValue(g, "g");
        validateValue(b, "b");
        return new RgbColor(r, g, b);
    }

    private static void validateValue(int value, String name) {
        if (value < 0 || 255 < value) {
            throw new OcException("%s value should in [0:255] : %d", name, value);
        }
    }

    private final int r;
    private final int g;
    private final int b;

    public double distance(RgbColor other) {
        final var rDiff = r - other.r;
        final var gDiff = g - other.g;
        final var bDiff = b - other.b;
        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }
}
