package com.ongakucraft.core.block;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import java.util.Optional;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = LAZY, of = "id")
@Getter
public final class BlockId {
    public static final String DEFAULT_NAMESPACE = "minecraft";
    private static final char DELIMITER = ':';

    public static BlockId of(String namespace, String path) {
        return new BlockId(namespace, path);
    }

    public static BlockId of(String path) {
        return of(DEFAULT_NAMESPACE, path);
    }

    public static Optional<BlockId> parse(String id) {
        final var delimiterIndex = id.indexOf(DELIMITER);
        if (-1 == delimiterIndex || delimiterIndex != id.lastIndexOf(DELIMITER)) {
            return Optional.empty();
        }
        return Optional.of(of(id.substring(0, delimiterIndex), id.substring(delimiterIndex + 1)));
    }

    @NonNull private final String namespace;
    @NonNull private final String path;
    @NonNull private final String id;

    private BlockId(String namespace, String path) {
        this(namespace, path, namespace + DELIMITER + path);
    }

    @Override
    public String toString() {
        return id;
    }
}
