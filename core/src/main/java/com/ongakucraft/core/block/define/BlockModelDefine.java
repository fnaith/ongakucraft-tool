package com.ongakucraft.core.block.define;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.ongakucraft.core.block.BlockId;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

// https://minecraft.fandom.com/wiki/Model#Block_models
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(cacheStrategy = LAZY, of = "id")
@Getter
@ToString
public final class BlockModelDefine {
    @NonNull private final BlockId id;
    @NonNull private final String down;
    @NonNull private final String up;
    @NonNull private final String north;
    @NonNull private final String south;
    @NonNull private final String west;
    @NonNull private final String east;
}
