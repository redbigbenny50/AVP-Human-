package com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.strategy.impl;

import com.blib.api.common.enchantment.v1.EnchantmentUtil;
import com.human.common.gameplay.entity.living.human.ai.MathUtil;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.strategy.ArmorStrategy;
import com.just.ai.goap.state.ReadableWorldState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

public class OverallDefenseStrategy implements ArmorStrategy {

    private final ArmorItem.Type type;

    public OverallDefenseStrategy(ArmorItem.Type type) {
        this.type = type;
    }

    @Override
    public ArmorItem.Type type() {
        return type;
    }

    @Override
    public boolean isValidWorldState(LivingEntity livingEntity, ReadableWorldState worldState) {
        return true;
    }

    @Override
    public double score(LivingEntity livingEntity, ReadableWorldState worldState, ItemStack itemStack) {
        if (itemStack.getItem() instanceof ArmorItem armorItem) {
            // This strategy is not applicable to the given armor item's type, so give lowest score back.
            if (armorItem.getType() != type) {
                return 0;
            }

            var weights = Weights.DEFAULT;
            // Use netherite as a baseline for scoring. It's a really good armor set, stat-wise!
            var netherite = ArmorMaterials.NETHERITE.value();
            var material = armorItem.getMaterial().value();
            // We use 50% above netherite values so that armors other than vanilla's strongest armor are viable choices.
            var defenseFit = MathUtil.mapNormalized(material.getDefense(type), 0, netherite.getDefense(type) * 1.5);
            var knockbackResistanceFit = MathUtil.mapNormalized(material.knockbackResistance(), 0, netherite.knockbackResistance() * 1.5);
            var toughnessFit = MathUtil.mapNormalized(material.toughness(), 0, netherite.toughness() * 1.5);

            // TODO: Enchantment strategy here, perhaps.
            var protectionLevel = EnchantmentUtil.getLevel(livingEntity.level(), itemStack, Enchantments.PROTECTION);
            var enchantmentFit = protectionLevel / 4.0;

            return defenseFit * weights.defense()
                + toughnessFit * weights.toughness()
                + enchantmentFit * weights.enchantment()
                + knockbackResistanceFit * weights.knockback();
        }

        return 0;
    }
}
