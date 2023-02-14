package com.ongakucraft.core.block.define;

import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.color.RgbColor;
import lombok.*;

import java.util.Collections;
import java.util.Map;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = LAZY, of = "id")
@Getter
@ToString
public final class BlockRgbColorDefine {
    public static BlockRgbColorDefine of(BlockId id, Map<Direction, RgbColor> colors) {
        return new BlockRgbColorDefine(id, Collections.unmodifiableMap(colors));
    }

    @NonNull private final BlockId id;
    @NonNull private final Map<Direction, RgbColor> colors;
}
