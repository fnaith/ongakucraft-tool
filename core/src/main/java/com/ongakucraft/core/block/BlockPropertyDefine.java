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
public final class BlockPropertyDefine {
    private final String id;
    private final String key;
    private final List<String> values;

    public BlockPropertyDefine(@NonNull String id, @NonNull String key, @NonNull List<String> values) {
        this.id = id;
        this.key = key;
        this.values = List.copyOf(values);
    }

    public boolean contains(String value) {
        return values.contains(value);
    }
}
