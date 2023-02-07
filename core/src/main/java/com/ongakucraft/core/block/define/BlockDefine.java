package com.ongakucraft.core.block.define;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import java.util.List;

import com.ongakucraft.core.block.BlockId;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = LAZY, of = "id")
@Getter
@ToString
public final class BlockDefine {
    public static BlockDefine of(BlockId id, List<BlockPropertyDefine> properties, boolean collisionShapeFullBlock) {
        return new BlockDefine(id, List.copyOf(properties), collisionShapeFullBlock);
    }

    @NonNull private final BlockId id;
    @NonNull private final List<BlockPropertyDefine> properties;
    private final boolean collisionShapeFullBlock;
}
