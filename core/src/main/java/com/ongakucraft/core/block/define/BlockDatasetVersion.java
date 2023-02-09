package com.ongakucraft.core.block.define;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(cacheStrategy = LAZY)
@Getter
public final class BlockDatasetVersion {
    @NonNull private final String mcVersion;
    private final int dataVersion;
}
