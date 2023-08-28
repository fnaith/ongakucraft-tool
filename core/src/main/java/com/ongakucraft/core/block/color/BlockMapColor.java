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
    public static BlockMapColor of(int mapColorId, BlockId blockId, RgbColor rgbColor, int gradient) {
        return new BlockMapColor(mapColorId, blockId, rgbColor, gradient);
    }

    private final int mapColorId;
    @NonNull private final BlockId blockId;
    @NonNull private final RgbColor rgbColor;
    private final int gradient;
    private final int colorId;
    @NonNull private final LabColor labColor;

    private BlockMapColor(int mapColorId, BlockId blockId, RgbColor rgbColor, int gradient) {
        this.mapColorId = mapColorId;
        this.blockId = blockId;
        this.rgbColor = rgbColor;
        this.gradient = gradient;
        this.colorId = mapColorId * 4 + gradient + 1;
        labColor = LabColor.of(rgbColor);
    }
}
