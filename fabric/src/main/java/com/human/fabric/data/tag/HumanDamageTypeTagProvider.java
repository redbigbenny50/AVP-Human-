package com.human.fabric.data.tag;

import com.human.common.registry.key.HumanDamageTypeKeys;
import com.human.common.registry.tag.HumanDamageTypesTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;

import java.util.concurrent.CompletableFuture;

public class HumanDamageTypeTagProvider extends FabricTagProvider<DamageType> {

    public HumanDamageTypeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, Registries.DAMAGE_TYPE, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        getOrCreateTagBuilder(DamageTypeTags.AVOIDS_GUARDIAN_THORNS)
            .add(
                HumanDamageTypeKeys.BULLET,
                HumanDamageTypeKeys.FLAMETHROW
            );

        // [stated] Oct 18 - vapor "ignores armour": the mist reaches the whole body. Armour and toughness only;
        // protection enchantments and Resistance still count.
        getOrCreateTagBuilder(DamageTypeTags.BYPASSES_ARMOR)
            .add(
                HumanDamageTypeKeys.RADIATION,
                HumanDamageTypeKeys.CRYO_VAPOR,
                HumanDamageTypeKeys.INFERNIUM_VAPOR,
                HumanDamageTypeKeys.ACID_VAPOR
            );

        getOrCreateTagBuilder(DamageTypeTags.BYPASSES_ENCHANTMENTS)
            .add(
                HumanDamageTypeKeys.RADIATION
            );

        getOrCreateTagBuilder(DamageTypeTags.BYPASSES_RESISTANCE)
            .add(
                HumanDamageTypeKeys.RADIATION
            );

        getOrCreateTagBuilder(DamageTypeTags.BYPASSES_WOLF_ARMOR)
            .add(
                HumanDamageTypeKeys.RADIATION
            );

        getOrCreateTagBuilder(DamageTypeTags.IS_PROJECTILE)
            .add(
                HumanDamageTypeKeys.BULLET,
                HumanDamageTypeKeys.FLAMETHROW
            );

        getOrCreateTagBuilder(DamageTypeTags.IS_FIRE)
            .add(
                HumanDamageTypeKeys.FLAMETHROW,
                HumanDamageTypeKeys.INFERNIUM_VAPOR
            );

        getOrCreateTagBuilder(DamageTypeTags.NO_KNOCKBACK)
            .add(
                HumanDamageTypeKeys.BULLET,
                HumanDamageTypeKeys.FLAMETHROW,
                HumanDamageTypeKeys.RADIATION,
                HumanDamageTypeKeys.RAZOR_WIRE,
                HumanDamageTypeKeys.CRYO_VAPOR,
                HumanDamageTypeKeys.INFERNIUM_VAPOR,
                HumanDamageTypeKeys.ACID_VAPOR
            );

        // All missing damage types here were excluded on purpose. Yes, including lava. Lava melts machinery.
        getOrCreateTagBuilder(HumanDamageTypesTags.DOES_NOT_HURT_SENTRY_TURRETS)
            .add(
                DamageTypes.FREEZE,
                DamageTypes.CACTUS,
                DamageTypes.CAMPFIRE,
                DamageTypes.CRAMMING,
                DamageTypes.DROWN,
                DamageTypes.FALL,
                DamageTypes.HOT_FLOOR,
                DamageTypes.IN_FIRE,
                DamageTypes.IN_WALL,
                DamageTypes.FIREWORKS,
                DamageTypes.ON_FIRE,
                HumanDamageTypeKeys.RADIATION,
                HumanDamageTypeKeys.RAZOR_WIRE,
                DamageTypes.STARVE,
                DamageTypes.STING,
                DamageTypes.SWEET_BERRY_BUSH,
                DamageTypes.THORNS,
                DamageTypes.WIND_CHARGE,
                DamageTypes.WITHER
            );
    }
}
