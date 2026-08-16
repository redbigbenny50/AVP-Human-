package com.human.fabric.data.recipe.impl;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.fabric.data.recipe.builder.RecipeBuilder;
import com.blib.mod.common.registry.init.BLibDataComponents;
import com.human.common.registry.init.item.HumanGunItems;
import com.human.common.registry.init.item.HumanItems;
import com.human.common.registry.tag.HumanItemTags;
import com.human.compatibility.HumanCommonItemTags;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class GunRecipeProvider {

    public static void provide(RecipeBuilder builder) {
        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .define('T', Items.TNT)
            .pattern("SSS")
            .pattern("STS")
            .pattern("SSS")
            .into(1, HumanItems.ROCKET);

        createDrumRecipes(builder);
        createBulletRecipes(builder);
        createGunPartRecipes(builder);
        createGunRecipes(builder);
        createGrenadeRecipes(builder);
    }

    /**
     * The machine guns' magazines. Brass for Old Painless's heavy cannister, clay for the smartgun's caseless drum,
     * both packed with gunpowder. The brass block is taken by COMMON TAG rather than by item, so a pack that gets its
     * brass from another mod can still feed the gun.
     */
    private static void createDrumRecipes(RecipeBuilder builder) {
        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('B', HumanCommonItemTags.STORAGE_BLOCKS_BRASS)
            .define('G', Items.GUNPOWDER)
            .pattern("GGG")
            .pattern("GBG")
            .pattern("GGG")
            .into(1, HumanItems.DRUM_CANNISTER);

        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('C', Items.CLAY)
            .define('G', Items.GUNPOWDER)
            .pattern("GGG")
            .pattern("GCG")
            .pattern("GGG")
            .into(1, HumanItems.DRUM_CARTRIDGE);
    }

    private static void createGrenadeRecipes(RecipeBuilder builder) {
        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanCommonItemTags.NUGGETS_STEEL)
            .define('B', Items.GUNPOWDER)
            .pattern("ABA")
            .pattern("ABA")
            .into(4, HumanItems.GRENADE);
        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanCommonItemTags.NUGGETS_STEEL)
            .define('B', Items.GUNPOWDER)
            .define('C', Items.BLAZE_POWDER)
            .pattern("ACA")
            .pattern("ABA")
            .into(4, HumanItems.GRENADE_INCENDIARY);
        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanCommonItemTags.NUGGETS_STEEL)
            .define('B', Items.GUNPOWDER)
            .define('C', HumanItemTags.URANIUM_NUGGET_LIKE)
            .pattern("ACA")
            .pattern("ABA")
            .into(4, HumanItems.GRENADE_IRRADIATED);
    }

    private static void createBulletRecipes(RecipeBuilder builder) {
        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanCommonItemTags.NUGGETS_BRASS)
            .define('B', Items.GUNPOWDER)
            .define('C', HumanCommonItemTags.NUGGETS_LEAD)
            .pattern(" C ")
            .pattern("ABA")
            .pattern("AAA")
            .into(24, HumanItems.SMALL_BULLET);

        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanCommonItemTags.NUGGETS_BRASS)
            .define('B', Items.GUNPOWDER)
            .define('C', HumanCommonItemTags.NUGGETS_STEEL)
            .define('D', HumanCommonItemTags.NUGGETS_LEAD)
            .pattern("ADA")
            .pattern("ABA")
            .pattern("ACA")
            .into(16, HumanItems.MEDIUM_BULLET);

        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanCommonItemTags.NUGGETS_BRASS)
            .define('B', Items.GUNPOWDER)
            .define('C', HumanCommonItemTags.NUGGETS_STEEL)
            .define('D', HumanCommonItemTags.NUGGETS_LEAD)
            .pattern("DDD")
            .pattern("CBC")
            .pattern("CAC")
            .into(8, HumanItems.HEAVY_BULLET);

        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanCommonItemTags.NUGGETS_BRASS)
            .define('B', Items.GUNPOWDER)
            .define('C', HumanItems.POLYMER)
            .define('D', HumanCommonItemTags.NUGGETS_LEAD)
            .pattern("DDD")
            .pattern("CBC")
            .pattern("CAC")
            .into(12, HumanItems.SHOTGUN_SHELL);

        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanCommonItemTags.NUGGETS_BRASS)
            .define('B', Items.GUNPOWDER)
            .define('C', Items.CLAY_BALL)
            .define('D', HumanCommonItemTags.NUGGETS_LEAD)
            .pattern(" D ")
            .pattern("CBC")
            .pattern("CAC")
            .into(16, HumanItems.CASELESS_BULLET);

        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanCommonItemTags.NUGGETS_ALUMINUM)
            .define('B', Items.MAGMA_CREAM)
            .pattern("AAA")
            .pattern("ABA")
            .pattern("AAA")
            .into(1, HumanItems.FUEL_TANK);
    }

    private static void createGunRecipes(RecipeBuilder builder) {
        createGenericGunRecipe(builder, HumanItems.BLUEPRINT_F903WE_RIFLE, HumanGunItems.F903WE_RIFLE.get(), true);
        createGenericGunRecipe(
            builder,
            HumanItems.BLUEPRINT_FLAMETHROWER_SEVASTOPOL,
            HumanGunItems.FLAMETHROWER_SEVASTOPOL.get(),
            false
        );
        createGenericGunRecipe(builder, HumanItems.BLUEPRINT_M37_12_SHOTGUN, HumanGunItems.M37_12_SHOTGUN.get(), true);
        createGenericGunRecipe(builder, HumanItems.BLUEPRINT_ZX_76_SHOTGUN, HumanGunItems.ZX_76_SHOTGUN.get(), true);
        createGenericGunRecipe(
            builder,
            HumanItems.BLUEPRINT_M88MOD4_COMBAT_PISTOL,
            HumanGunItems.M88MOD4_COMBAT_PISTOL.get(),
            false
        );
        createGenericGunRecipe(
            builder,
            HumanItems.BLUEPRINT_M42A3_SNIPER_RIFLE,
            HumanGunItems.M42A3_SNIPER_RIFLE.get(),
            true
        );
        createGenericGunRecipe(
            builder,
            HumanItems.BLUEPRINT_M4RA_BATTLE_RIFLE,
            HumanGunItems.M4RA_BATTLE_RIFLE.get(),
            true
        );

        builder.shapeless()
            .withCategory(RecipeCategory.COMBAT)
            .requires(1, HumanItems.BLUEPRINT_M41A_PULSE_RIFLE)
            .requires(1, HumanItems.BARREL)
            .requires(1, HumanItems.GRIP)
            .requires(1, HumanItems.SMART_RECEIVER)
            .requires(1, HumanItems.STOCK)
            .into(createItemStackNoAzureLibID(HumanGunItems.M41A_PULSE_RIFLE));

        builder.shapeless()
            .withCategory(RecipeCategory.COMBAT)
            .requires(1, HumanItems.BLUEPRINT_M56_SMARTGUN)
            .requires(1, HumanItems.SMART_BARREL)
            .requires(1, HumanItems.GRIP)
            .requires(1, HumanItems.SMART_RECEIVER)
            .into(createItemStackNoAzureLibID(HumanGunItems.M56_SMARTGUN));

        builder.shapeless()
            .withCategory(RecipeCategory.COMBAT)
            .requires(1, HumanItems.BLUEPRINT_OLD_PAINLESS)
            .requires(1, HumanItems.MINIGUN_BARREL)
            .requires(2, HumanItems.GRIP)
            .requires(1, HumanItems.RECEIVER)
            .into(createItemStackNoAzureLibID(HumanGunItems.OLD_PAINLESS));

        builder.shapeless()
            .withCategory(RecipeCategory.COMBAT)
            .requires(1, HumanItems.BLUEPRINT_M6B_ROCKET_LAUNCHER)
            .requires(1, HumanItems.ROCKET_BARREL)
            .requires(1, HumanItems.GRIP)
            .requires(1, HumanItems.SMART_RECEIVER)
            .into(createItemStackNoAzureLibID(HumanGunItems.M6B_ROCKET_LAUNCHER));
    }

    private static void createGunPartRecipes(RecipeBuilder builder) {
        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanCommonItemTags.INGOTS_STEEL)
            .pattern("AAA")
            .into(1, HumanItems.BARREL);

        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanItems.POLYMER)
            .define('B', HumanCommonItemTags.INGOTS_STEEL)
            .pattern("B")
            .pattern("A")
            .pattern("A")
            .into(1, HumanItems.GRIP);

        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanCommonItemTags.INGOTS_STEEL)
            .define('B', HumanItems.BARREL)
            .pattern("BBB")
            .pattern("A A")
            .pattern("BBB")
            .into(1, HumanItems.MINIGUN_BARREL);

        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanCommonItemTags.INGOTS_STEEL)
            .pattern("AAA")
            .pattern("   ")
            .pattern("AAA")
            .into(1, HumanItems.ROCKET_BARREL);

        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanCommonItemTags.INGOTS_STEEL)
            .define('B', HumanItems.POLYMER)
            .define('C', Items.TRIPWIRE_HOOK)
            .define('D', Items.STONE_BUTTON)
            .pattern("AAA")
            .pattern("BAC")
            .pattern("BDB")
            .into(1, HumanItems.RECEIVER);

        builder.shapeless()
            .withCategory(RecipeCategory.COMBAT)
            .requires(1, HumanItems.BARREL)
            .requires(1, Items.OBSERVER)
            .into(1, HumanItems.SMART_BARREL);

        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanItems.CAPACITOR)
            .define('B', HumanItems.BATTERY_PACK)
            .define('C', HumanItems.CPU)
            .define('L', HumanItems.LED_DISPLAY)
            .define('P', HumanItems.POLYMER)
            .define('R', HumanItems.RECEIVER)
            .pattern(" R ")
            .pattern("CLB")
            .pattern("PAP")
            .into(1, HumanItems.SMART_RECEIVER);

        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('A', HumanItems.POLYMER)
            .define('B', HumanCommonItemTags.INGOTS_STEEL)
            .pattern("  A")
            .pattern("BAA")
            .pattern("  A")
            .into(1, HumanItems.STOCK);
    }

    private static void createGenericGunRecipe(
        RecipeBuilder builder,
        Supplier<Item> blueprintItem,
        Item result,
        boolean hasStock
    ) {
        var shapeless = builder.shapeless()
            .withCategory(RecipeCategory.COMBAT)
            .requires(1, blueprintItem)
            .requires(1, HumanItems.BARREL)
            .requires(1, HumanItems.GRIP)
            .requires(1, HumanItems.RECEIVER);

        if (hasStock) {
            shapeless.requires(1, HumanItems.STOCK);
        }

        shapeless.into(createItemStackNoAzureLibID(result));
    }

    private static @NotNull ItemStack createItemStackNoAzureLibID(BLibHolder<? extends ItemLike> holder) {
        return createItemStackNoAzureLibID(holder.get());
    }

    private static @NotNull ItemStack createItemStackNoAzureLibID(ItemLike itemLike) {
        var itemStack = new ItemStack(itemLike, 1);

        itemStack.remove(BLibDataComponents.AZ_ID.get());

        return itemStack;
    }
}
