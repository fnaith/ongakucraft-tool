package com.ongakucraft.core.color;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "of")
@Getter
@ToString
public final class LabColor {
    private final double l;
    private final double a;
    private final double b;

    public double distance(LabColor other) {
        return CIEDE2000.calculateDeltaE(l, a, b, other.l, other.a, other.b);
    }
}
