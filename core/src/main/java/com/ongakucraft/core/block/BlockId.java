package com.ongakucraft.core.block;

import lombok.*;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

@AllArgsConstructor
@EqualsAndHashCode(cacheStrategy = LAZY, of = "id")
@Getter
public final class BlockId {
    private final String namespace;
    private final String path;
    private final String id;

    public BlockId(@NonNull String namespace, @NonNull String path) {
        this(namespace, path, namespace + ':' + path);
    }

    public String toString() {
        return id;
    }
}
