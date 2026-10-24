package com.human.neoforge.data.loot.modifier;

import com.human.common.data.loot.LootTableModifiers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * [stated] Oct 19 - spiders and cave spiders have a 20% chance to drop a cobweb. NeoForge's side of
 * {@link LootTableModifiers#SPIDER_COBWEB_LOOT_TABLE_MODIFIER}; Fabric injects the same pool in HumanFabric.
 */
public class SpiderCobwebLootModifier extends LootModifier {

    public static final MapCodec<SpiderCobwebLootModifier> CODEC = RecordCodecBuilder.mapCodec(
        inst -> LootModifier.codecStart(inst).apply(inst, SpiderCobwebLootModifier::new)
    );

    public static final String NAME = "spider_cobweb_loot_modifier";

    public SpiderCobwebLootModifier() {
        this(new LootItemCondition[] {});
    }

    protected SpiderCobwebLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
        @NotNull ObjectArrayList<ItemStack> objectArrayList,
        @NotNull LootContext lootContext
    ) {
        objectArrayList.addAll(
            LootTableModifiers.SPIDER_COBWEB_LOOT_TABLE_MODIFIER.roll(lootContext.getQueriedLootTableId(), lootContext)
        );
        return objectArrayList;
    }

    @Override
    public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
