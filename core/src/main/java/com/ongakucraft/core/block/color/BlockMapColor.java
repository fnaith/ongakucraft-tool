package com.ongakucraft.core.block.color;

import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.color.LabColor;
import com.ongakucraft.core.color.RgbColor;
import lombok.*;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@EqualsAndHashCode(cacheStrategy = LAZY, of = "rgbColor")
@Getter
@ToString
public final class BlockMapColor {
    public static BlockMapColor of(BlockId id, RgbColor rgbolor, int gradient) {
        return new BlockMapColor(id, rgbolor, gradient);
    }

    @NonNull private final BlockId id;
    @NonNull private final RgbColor rgbColor;
    private final int gradient;
    @NonNull private final LabColor labColor;

    private BlockMapColor(BlockId id, RgbColor rgbColor, int gradient) {
        this.id = id;
        this.rgbColor = rgbColor;
        this.gradient = gradient;
        labColor = LabColor.of(rgbColor);
    }
}
