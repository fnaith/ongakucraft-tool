package com.ongakucraft.core.block;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@EqualsAndHashCode(cacheStrategy = LAZY, of = "id")
@Getter
public final class BlockId {
    public static final String DEFAULT_NAMESPACE = "minecraft";
    private static final char DELIMITER = ':';

    public static Optional<BlockId> of(String id) {
        final var delimiterIndex = id.indexOf(DELIMITER);
        if (-1 == delimiterIndex || delimiterIndex != id.lastIndexOf(DELIMITER)) {
            return Optional.empty();
        }
        return Optional.of(new BlockId(id.substring(0, delimiterIndex), id.substring(delimiterIndex + 1)));
    }

    private final String namespace;
    private final String path;
    private final String id;

    public BlockId(String path) {
        this(DEFAULT_NAMESPACE, path);
    }

    public BlockId(@NonNull String namespace, @NonNull String path) {
        this(namespace, path, namespace + DELIMITER + path);
    }

    @Override
    public String toString() {
        return id;
    }
}
