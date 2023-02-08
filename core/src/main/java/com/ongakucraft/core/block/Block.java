package com.ongakucraft.core.block;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.define.BlockDefine;
import com.ongakucraft.core.block.define.BlockPropertyDefine;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.With;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = LAZY, exclude = "propertyDefineMap")
@Getter
@ToString
public final class Block {
    public static Block of(BlockDefine blockDefine) {
        return new Block(blockDefine.getId(), blockDefine.getProperties().stream().collect(
                Collectors.toMap(BlockPropertyDefine::getKey, Function.identity())), Direction.N, new HashMap<>());
    }

    @NonNull private final BlockId id;
    @NonNull private final Map<String, BlockPropertyDefine> propertyDefineMap;
    @NonNull @With(AccessLevel.PRIVATE) private final Direction facing;
    @NonNull @With(AccessLevel.PRIVATE) private final Map<String, String> propertyValueMap;

    public Block rotate(int times) {
        if (0 == times % 4) {
            return this;
        }
        return withFacing(facing.rotate(times));
    }

    public String get(String property) {
        return propertyValueMap.get(property);
    }

    public Block put(String property, boolean value) {
        return put(property, String.valueOf(value));
    }

    public Block put(String property, int value) {
        return put(property, String.valueOf(value));
    }

    public Block put(String property, String value) {
        if (propertyDefineMap.containsKey(property) && !propertyDefineMap.get(property).contains(value)) {
            throw new OcException("invalid property value : %s %s", property, value);
        }
        final var newPropertyValueMap = new HashMap<>(propertyValueMap);
        newPropertyValueMap.put(property, value);
        return withPropertyValueMap(newPropertyValueMap);
    }
}
