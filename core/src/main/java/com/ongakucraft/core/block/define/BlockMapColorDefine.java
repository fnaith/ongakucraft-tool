package com.ongakucraft.core.block.define;

import com.ongakucraft.core.color.RgbColor;
import lombok.*;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = LAZY, of = "id")
@Getter
@ToString
public final class BlockMapColorDefine {
    public static BlockMapColorDefine of(int id, RgbColor color) {
        return new BlockMapColorDefine(id, color);
    }

    private final int id;
    @NonNull private final RgbColor color;
}
