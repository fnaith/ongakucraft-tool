package com.ongakucraft.core.prefab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

@AllArgsConstructor(staticName = "of")
@Getter
public final class BlockColorFilterOption {
    public static final BlockColorFilterOption DEFAULT = of(
            false, false, false,
            false, false, false, false,
            false, false, false);

    @With private final boolean allowFallingBlock;
    @With private final boolean allowTransparentBlock;
    @With private final boolean allowInteractableBlock;
    @With private final boolean allowMechanicalBlock;
    @With private final boolean allowStoneMaterialVariation;
    @With private final boolean allowDirtMaterialVariation;
    @With private final boolean allowUtilityBlock;
    @With private final boolean allowOre;
    @With private final boolean allowGlazedTerracotta;
    @With private final boolean allowBuildingBlock;
}
