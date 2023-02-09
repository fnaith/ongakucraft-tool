package com.ongakucraft.core.color;

import com.ongakucraft.core.OcException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public final class LabColor {
    public static LabColor of(double l, double a, double b) {
        validateValue(0, 100, l, "l");
        validateValue(-128, 127, a, "a");
        validateValue(-128, 127, b, "b");
        return new LabColor(l, a, b);
    }

    private static void validateValue(double min, double max, double value, String name) {
        if (value < min || max < value) {
            throw new OcException("%s value should in [%f:%f] : %d", name, min, max, value);
        }
    }

    private final double l;
    private final double a;
    private final double b;

    public double distance(LabColor other) {
        return CIEDE2000.calculateDeltaE(l, a, b, other.l, other.a, other.b);
    }
}
