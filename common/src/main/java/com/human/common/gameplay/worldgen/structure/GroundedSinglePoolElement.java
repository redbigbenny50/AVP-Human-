package com.human.common.gameplay.worldgen.structure;

import com.human.common.registry.init.HumanStructureTypes;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A single-piece pool element that says where its GROUND is.
 * <p>
 * [stated] Oct 23 - the mining camps must sit with their grass on the world's ground: "i dont want it placed 8 blocks
 * off the ground or buried and water flows over it."
 * </p>
 * <p>
 * ⚠⚠ WHY VANILLA'S ELEMENT CANNOT DO IT. Vanilla's single_pool_element always reports a ground level of ONE block above
 * the bottom of its file. The camps are not built like that: each carries nine layers of terrain and mine under its
 * grass, which sits at y 9 of the file. The terrain adaptation (the "beard" that blends the world into a structure)
 * reads that ground level, so with vanilla's element it shaped the land around each camp to a ground EIGHT BLOCKS BELOW
 * the grass - carving a pit round the camp that water would pour into.
 * </p>
 * <p>
 * This element is vanilla's in every other way and only reports its own {@code ground_level_delta} from the pool file:
 * 10 for the camps (grass at y 9, plus the one block vanilla measures from). Both the start placement and the terrain
 * blending then treat the grass as the ground.
 * </p>
 */
public class GroundedSinglePoolElement extends SinglePoolElement {

    public static final String NAME = "grounded_single_pool_element";

    public static final MapCodec<GroundedSinglePoolElement> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            templateCodec(),
            processorsCodec(),
            projectionCodec(),
            overrideLiquidSettingsCodec(),
            Codec.intRange(0, 256).fieldOf("ground_level_delta").forGetter(element -> element.groundLevelDelta)
        )
            .apply(instance, GroundedSinglePoolElement::new)
    );

    private final int groundLevelDelta;

    protected GroundedSinglePoolElement(
        Either<ResourceLocation, StructureTemplate> template,
        Holder<StructureProcessorList> processors,
        StructureTemplatePool.Projection projection,
        Optional<LiquidSettings> overrideLiquidSettings,
        int groundLevelDelta
    ) {
        super(template, processors, projection, overrideLiquidSettings);
        this.groundLevelDelta = groundLevelDelta;
    }

    @Override
    public int getGroundLevelDelta() {
        return groundLevelDelta;
    }

    @Override
    public @NotNull StructurePoolElementType<?> getType() {
        return HumanStructureTypes.GROUNDED_SINGLE_POOL_ELEMENT.get();
    }

    @Override
    public String toString() {
        return "GroundedSingle[" + template + ", ground " + groundLevelDelta + "]";
    }
}
