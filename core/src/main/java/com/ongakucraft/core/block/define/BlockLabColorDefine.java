package com.ongakucraft.core.block.define;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import java.util.Collections;
import java.util.Map;

import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.color.LabColor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = LAZY, of = "id")
@Getter
@ToString
public final class BlockLabColorDefine {
    public static BlockLabColorDefine of(BlockId id, Map<Direction, LabColor> colors) {
        return new BlockLabColorDefine(id, Collections.unmodifiableMap(colors));
    }

    @NonNull private final BlockId id;
    @NonNull private final Map<Direction, LabColor> colors;
}
