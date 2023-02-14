package com.ongakucraft.core.block.define;

import com.ongakucraft.core.block.BlockId;
import lombok.*;

import java.util.Collections;
import java.util.List;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = LAZY, of = "id")
@Getter
@ToString
public final class BlockDefine {
    public static BlockDefine of(BlockId id, List<BlockPropertyDefine> properties, boolean collisionShapeFullBlock) {
        return new BlockDefine(id, Collections.unmodifiableList(properties), collisionShapeFullBlock);
    }

    @NonNull private final BlockId id;
    @NonNull private final List<BlockPropertyDefine> properties;
    private final boolean collisionShapeFullBlock;
}
