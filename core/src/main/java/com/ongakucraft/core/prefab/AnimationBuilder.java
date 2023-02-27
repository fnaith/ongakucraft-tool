package com.ongakucraft.core.prefab;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.ongakucraft.core.block.Block;

import lombok.NonNull;

public final class AnimationBuilder {
    public static List<Block[][]> diffBlockGridList(List<Block[][]> blockGridList) {
        final List<Block[][]> diffList = new ArrayList<>();
        final var size = blockGridList.size();
        for (var i = 0; i < size; ++i) {
            final var src = blockGridList.get(i % size);
            final var dist = blockGridList.get((i + 1) % size);
            final var diff = diffBlockGrid(src, dist);
            diffList.add(diff);
        }
        return diffList;
    }

    public static Block[][] diffBlockGrid(@NonNull Block[][] src, @NonNull Block[][] dist) {
        final var h = src.length;
        final var w = src[0].length;
        final var diff = new Block[h][];
        for (var y = 0; y < h; ++y) {
            diff[y] = new Block[w];
            for (var x = 0; x < w; ++x) {
                if (Objects.equals(src[y][x], dist[y][x])) {
                    continue;
                }
                diff[y][x] = dist[y][x];
            }
        }
        return diff;
    }

    private AnimationBuilder() {}
}
