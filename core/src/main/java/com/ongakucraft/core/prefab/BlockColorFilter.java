package com.ongakucraft.core.prefab;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.color.BlockLabColor;
import lombok.NonNull;

import java.util.List;

public final class BlockColorFilter {
    public static List<BlockLabColor> filterColoredBlock(@NonNull List<BlockLabColor> blockLabColorList) {
        return blockLabColorList.stream().filter(blockLabColor -> {
            final var path = blockLabColor.getId().getPath();
            if (path.endsWith("_glazed_terracotta")) {
                return false;
            }
            if (path.endsWith("_wool")
                || path.endsWith("_terracotta")
                || path.endsWith("_concrete")
            ) {
                return true;
            }
            return false;
        }).toList();
    }

    public static List<BlockLabColor> filterSimpleColor(@NonNull List<BlockLabColor> blockLabColorList,
                                                        @NonNull BlockColorFilterOption option) {
        return blockLabColorList.stream().filter(blockLabColor -> {
            final var path = blockLabColor.getId().getPath();
            // creative mode
            if (List.of("bedrock", "structure_block", "spawner").contains(path)) {
                return false;
            }
            // melting
            if ("ice".equals(path)) {
                return false;
            }
            // growing
            if ("budding_amethyst".equals(path)) {
                return false;
            }
            // dying
            if (path.endsWith("_coral_block")) {
                return false;
            }
            // oxidising
            if (List.of("copper_block", "exposed_copper", "weathered_copper",
                        "cut_copper", "exposed_cut_copper", "weathered_cut_copper").contains(path)) {
                return false;
            }
            // falling
            if (List.of("gravel", "sand", "red_sand", "soul_sand").contains(path)
                || path.endsWith("_concrete_powder")) {
                return option.isAllowFallingBlock();
            }
            // transparent
            if (List.of("glass", "tinted_glass").contains(path) || path.endsWith("_leaves") || path.endsWith("_stained_glass")) {
                return option.isAllowTransparentBlock();
            }
            // interactable
            if (List.of("barrel", "blast_furnace", "cartography_table", "crafting_table", "fletching_table", "furnace", "loom", "smithing_table", "smoker").contains(path)) {
                return option.isAllowInteractableBlock();
            }
            // mechanical
            if (List.of("dispenser", "dropper", "note_block", "redstone_lamp", "target", "crafting_table").contains(path)) {
                return option.isAllowInteractableBlock();
            }
            // stone material variation
            if (path.startsWith("chiseled_")
                || path.startsWith("cut_")
                || path.startsWith("mossy_")
                || path.startsWith("gilded_")
                || path.endsWith("_tiles")
                || "oxidized_cut_copper".equals(path)
            ) {
                return option.isAllowStoneMaterialVariation();
            }
            // dirt material variation
            if (List.of("coarse_dirt", "rooted_dirt", "mycelium",
                        "crimson_nylium", "warped_nylium", "podzol").contains(path)) {
                return option.isAllowDirtMaterialVariation();
            }
            // utility block
            if (List.of("tnt", "bookshelf", "jukebox", "lodestone").contains(path)) {
                return option.isAllowUtilityBlock();
            }
            // ore
            if (path.endsWith("_ore")) {
                return option.isAllowOre();
            }
            // glazed terracotta
            if (path.endsWith("_glazed_terracotta")) {
                return option.isAllowGlazedTerracotta();
            }
            // building block
            if (path.endsWith("_planks")
                || path.endsWith("_slab")
                || path.endsWith("_bricks")
                || "bricks".equals(path)) {
                return option.isAllowBuildingBlock();
            }

            if (path.endsWith("_wool")
                || path.endsWith("_log")
                || path.endsWith("_wood")
                || path.endsWith("_terracotta")
                || path.endsWith("_concrete")
                || path.startsWith("polished_")
                || path.startsWith("smooth_")
                || path.startsWith("stripped_")
                || path.startsWith("raw_")
            ) {
                return true;
            }
            switch (path) {
                case "stone":
                case "granite":
                case "diorite":
                case "andesite":
                case "dirt":
                case "cobblestone":
                case "sponge":
                case "wet_sponge":
                case "lapis_block":
                case "sandstone":
                case "gold_block":
                case "iron_block":
                case "obsidian":
                case "diamond_block":
                case "snow_block":
                case "clay":
                case "pumpkin":
                case "netherrack":
                case "soul_soil":
                case "basalt":
                case "glowstone":
                case "brown_mushroom_block":
                case "red_mushroom_block":
                case "mushroom_stem":
                case "melon":
                case "end_stone":
                case "emerald_block":
                case "redstone_block":
                case "quartz_block":
                case "quartz_pillar":
                case "prismarine":
                case "dark_prismarine":
                case "sea_lantern":
                case "hay_block":
                case "terracotta":
                case "coal_block":
                case "packed_ice":
                case "red_sandstone":
                case "purpur_block":
                case "purpur_pillar":
                case "magma_block":
                case "nether_wart_block":
                case "bone_block":
                case "blue_ice":
                case "warped_stem":
                case "warped_hyphae":
                case "warped_wart_block":
                case "crimson_stem":
                case "crimson_hyphae":
                case "shroomlight":
                case "bee_nest":
                case "beehive":
                case "honeycomb_block":
                case "netherite_block":
                case "ancient_debris":
                case "crying_obsidian":
                case "blackstone":
                case "amethyst_block":
                case "tuff":
                case "calcite":
                case "dripstone_block":
                case "moss_block":
                case "deepslate":
                case "oxidized_copper":
                case "cobbled_deepslate":
                case "carved_pumpkin":
                case "jack_o_lantern":
                    return true;
            }
            throw new OcException(path);
        }).toList();
    }

    private BlockColorFilter() {}
}
