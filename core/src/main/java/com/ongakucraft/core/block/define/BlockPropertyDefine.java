package com.ongakucraft.core.block.define;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import java.util.List;

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
public final class BlockPropertyDefine {
    public static BlockPropertyDefine of(String id, String key, List<String> values) {
        return new BlockPropertyDefine(id, key, List.copyOf(values));
    }

    @NonNull private final String id;
    @NonNull private final String key;
    @NonNull private final List<String> values;

    public boolean contains(String value) {
        return values.contains(value);
    }
}
