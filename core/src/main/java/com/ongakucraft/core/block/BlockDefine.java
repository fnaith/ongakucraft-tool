package com.ongakucraft.core.block;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

@EqualsAndHashCode(cacheStrategy = LAZY, of = "id")
@Getter
@ToString
public final class BlockDefine {
    private final BlockId id;
    private final boolean collisionShapeFullBlock;
    private final List<BlockPropertyDefine> properties;

    public BlockDefine(@NonNull BlockId id,
                       boolean collisionShapeFullBlock,
                       @NonNull List<BlockPropertyDefine> properties) {
        this.id = id;
        this.collisionShapeFullBlock = collisionShapeFullBlock;
        this.properties = List.copyOf(properties);
    }
}
