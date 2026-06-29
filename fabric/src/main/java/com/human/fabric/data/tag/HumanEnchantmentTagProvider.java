package com.human.fabric.data.tag;

import com.human.common.registry.tag.HumanEnchantmentTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.concurrent.CompletableFuture;

public class HumanEnchantmentTagProvider extends FabricTagProvider.EnchantmentTagProvider {

    public HumanEnchantmentTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        getOrCreateTagBuilder(HumanEnchantmentTags.GUN_ENCHANTMENTS)
            .add(
                Enchantments.FLAME,
                Enchantments.MULTISHOT,
                Enchantments.PIERCING,
                Enchantments.POWER,
                Enchantments.PUNCH,
                Enchantments.QUICK_CHARGE,
                Enchantments.UNBREAKING,
                Enchantments.VANISHING_CURSE
            );
    }
}
