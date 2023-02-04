package com.ongakucraft.core.block;

import lombok.*;

import java.util.List;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

@AllArgsConstructor
@EqualsAndHashCode(cacheStrategy = LAZY, of = "id")
@Getter
@ToString
public final class BlockModelDefine {
    private final String id;
    private final String down;
    private final String up;
    private final String north;
    private final String south;
    private final String west;
    private final String east;
}
