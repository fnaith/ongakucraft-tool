package com.ongakucraft.core.block;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.define.BlockDefine;
import com.ongakucraft.core.block.define.BlockPropertyDefine;
import lombok.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = LAZY, exclude = "propertyDefineMap")
@Getter
@ToString
public final class Block {
    private static final Direction DEFAULT_FACING = Direction.N;

    public static Block of(BlockDefine blockDefine) {
        return new Block(blockDefine.getId(), blockDefine.getProperties().stream().collect(
                Collectors.toMap(BlockPropertyDefine::getKey, Function.identity())), DEFAULT_FACING, new HashMap<>());
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

    public Block back() {
        return withFacing(facing.back());
    }

    public Block left() {
        return withFacing(facing.left());
    }

    public Block right() {
        return withFacing(facing.right());
    }

    public String get(@NonNull String property) {
        return propertyValueMap.get(property);
    }

    public Block put(String property, boolean value) {
        return put(property, String.valueOf(value));
    }

    public Block put(String property, int value) {
        return put(property, String.valueOf(value));
    }

    public Block put(@NonNull String property, @NonNull String value) {
        if (propertyDefineMap.containsKey(property) && !propertyDefineMap.get(property).contains(value)) {
            throw new OcException("invalid property value : %s %s", property, value);
        }
        final var newPropertyValueMap = new HashMap<>(propertyValueMap);
        newPropertyValueMap.put(property, value);
        return withPropertyValueMap(newPropertyValueMap);
    }

    public String remove(@NonNull String property) {
        return propertyValueMap.remove(property);
    }

    public Map<String, String> getProperties() {
        final Map<String, String> properties = new HashMap<>(propertyValueMap);
        if (propertyDefineMap.containsKey("facing")) {
            properties.put("facing", facing.getText());
        }
        return Collections.unmodifiableMap(properties);
    }
}
