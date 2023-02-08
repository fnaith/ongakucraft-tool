package com.ongakucraft.core.block.define;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.Direction;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

// https://minecraft.fandom.com/wiki/Model#Block_models
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = LAZY, of = "id")
@Getter
@ToString
public final class BlockModelDefine {
    public static BlockModelDefine of(BlockId id, String down, String up, String north,
                                      String south, String west, String east) {
        final Map<Direction, String> textures = new EnumMap<>(Direction.class);
        textures.put(Direction.D, down);
        textures.put(Direction.U, up);
        textures.put(Direction.N, north);
        textures.put(Direction.S, south);
        textures.put(Direction.W, west);
        textures.put(Direction.E, east);
        return of(id, textures);
    }

    public static BlockModelDefine of(BlockId id, Map<Direction, String> textures) {
        return new BlockModelDefine(id, Collections.unmodifiableMap(textures));
    }

    @NonNull private final BlockId id;
    @NonNull private final Map<Direction, String> textures;
}
