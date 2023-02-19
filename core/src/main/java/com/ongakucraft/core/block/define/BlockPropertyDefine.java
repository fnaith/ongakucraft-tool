package com.ongakucraft.core.block.define;

import lombok.*;

import java.util.Collections;
import java.util.List;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = LAZY, of = "id")
@Getter
@ToString
public final class BlockPropertyDefine {
    public static BlockPropertyDefine of(String id, String key, List<String> values) {
        return new BlockPropertyDefine(id, key, Collections.unmodifiableList(values));
    }

    @NonNull private final String id;
    @NonNull private final String key;
    @NonNull private final List<String> values;

    public boolean contains(@NonNull String value) {
        return values.contains(value);
    }
}
