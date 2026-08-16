package com.human.common.registry.init.creative_mode_tab.initializer;

import com.blib.api.common.registry.v1.BLibHolder;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.item.HumanArmorItems;
import com.human.common.registry.init.item.HumanGunItems;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class CombatCreativeModeTabInitializer {

    public static final Consumer<CreativeModeTab.Output> OUTPUT_CONSUMER = output -> {
        CreativeModeTabUtil.accept(output, HumanBlocks.AMMO_CHEST);
        CreativeModeTabUtil.accept(output, HumanItems.GRENADE);
        CreativeModeTabUtil.accept(output, HumanItems.GRENADE_INCENDIARY);
        CreativeModeTabUtil.accept(output, HumanItems.GRENADE_IRRADIATED);
        CreativeModeTabUtil.accept(output, HumanItems.CASELESS_BULLET);
        CreativeModeTabUtil.accept(output, HumanItems.HEAVY_BULLET);
        CreativeModeTabUtil.accept(output, HumanItems.SMALL_BULLET);
        CreativeModeTabUtil.accept(output, HumanItems.MEDIUM_BULLET);
        CreativeModeTabUtil.accept(output, HumanItems.SHOTGUN_SHELL);
        CreativeModeTabUtil.accept(output, HumanItems.ROCKET);
        CreativeModeTabUtil.accept(output, HumanItems.FUEL_TANK);
        CreativeModeTabUtil.accept(output, HumanItems.DRUM_CARTRIDGE);
        CreativeModeTabUtil.accept(output, HumanItems.DRUM_CANNISTER);

        CreativeModeTabUtil.accept(output, HumanGunItems.F903WE_RIFLE);
        CreativeModeTabUtil.accept(output, HumanGunItems.FLAMETHROWER_SEVASTOPOL);
        CreativeModeTabUtil.accept(output, HumanGunItems.M37_12_SHOTGUN);
        CreativeModeTabUtil.accept(output, HumanGunItems.M41A_PULSE_RIFLE);
        CreativeModeTabUtil.accept(output, HumanGunItems.M42A3_SNIPER_RIFLE);
        CreativeModeTabUtil.accept(output, HumanGunItems.M4RA_BATTLE_RIFLE);
        CreativeModeTabUtil.accept(output, HumanGunItems.M56_SMARTGUN);
        CreativeModeTabUtil.accept(output, HumanGunItems.M6B_ROCKET_LAUNCHER);
        CreativeModeTabUtil.accept(output, HumanGunItems.M88MOD4_COMBAT_PISTOL);
        CreativeModeTabUtil.accept(output, HumanGunItems.OLD_PAINLESS);
        CreativeModeTabUtil.accept(output, HumanGunItems.ZX_76_SHOTGUN);

        CreativeModeTabUtil.accept(output, HumanArmorItems.WY_APE_HELMET);
        CreativeModeTabUtil.accept(output, HumanArmorItems.WY_APE_CHESTPLATE);
        CreativeModeTabUtil.accept(output, HumanArmorItems.WY_APE_LEGGINGS);
        CreativeModeTabUtil.accept(output, HumanArmorItems.WY_APE_BOOTS);

        CreativeModeTabUtil.accept(output, HumanArmorItems.MK50_HELMET);
        CreativeModeTabUtil.accept(output, HumanArmorItems.MK50_CHESTPLATE);
        CreativeModeTabUtil.accept(output, HumanArmorItems.MK50_LEGGINGS);
        CreativeModeTabUtil.accept(output, HumanArmorItems.MK50_BOOTS);

        CreativeModeTabUtil.accept(output, applyPressureSuitDyeColor(HumanArmorItems.PRESSURE_HELMET));
        CreativeModeTabUtil.accept(output, applyPressureSuitDyeColor(HumanArmorItems.PRESSURE_CHESTPLATE));
        CreativeModeTabUtil.accept(output, applyPressureSuitDyeColor(HumanArmorItems.PRESSURE_LEGGINGS));
        CreativeModeTabUtil.accept(output, applyPressureSuitDyeColor(HumanArmorItems.PRESSURE_BOOTS));

        CreativeModeTabUtil.accept(output, HumanArmorItems.STEEL_HELMET);
        CreativeModeTabUtil.accept(output, HumanArmorItems.STEEL_CHESTPLATE);
        CreativeModeTabUtil.accept(output, HumanArmorItems.STEEL_LEGGINGS);
        CreativeModeTabUtil.accept(output, HumanArmorItems.STEEL_BOOTS);

        CreativeModeTabUtil.accept(output, HumanArmorItems.TACTICAL_HELMET);
        CreativeModeTabUtil.accept(output, HumanArmorItems.TACTICAL_CHESTPLATE);
        CreativeModeTabUtil.accept(output, HumanArmorItems.TACTICAL_LEGGINGS);
        CreativeModeTabUtil.accept(output, HumanArmorItems.TACTICAL_BOOTS);

        CreativeModeTabUtil.accept(output, HumanArmorItems.TACTICAL_CAMO_HELMET);
        CreativeModeTabUtil.accept(output, HumanArmorItems.TACTICAL_CAMO_CHESTPLATE);
        CreativeModeTabUtil.accept(output, HumanArmorItems.TACTICAL_CAMO_LEGGINGS);
        CreativeModeTabUtil.accept(output, HumanArmorItems.TACTICAL_CAMO_BOOTS);

        CreativeModeTabUtil.accept(output, HumanArmorItems.TITANIUM_HELMET);
        CreativeModeTabUtil.accept(output, HumanArmorItems.TITANIUM_CHESTPLATE);
        CreativeModeTabUtil.accept(output, HumanArmorItems.TITANIUM_LEGGINGS);
        CreativeModeTabUtil.accept(output, HumanArmorItems.TITANIUM_BOOTS);

        CreativeModeTabUtil.accept(output, HumanArmorItems.WY_COMMANDO_HELMET);
        CreativeModeTabUtil.accept(output, HumanArmorItems.WY_COMMANDO_CHESTPLATE);
        CreativeModeTabUtil.accept(output, HumanArmorItems.WY_COMMANDO_LEGGINGS);
        CreativeModeTabUtil.accept(output, HumanArmorItems.WY_COMMANDO_BOOTS);

        CreativeModeTabUtil.accept(output, HumanArmorItems.WY_ELITE_HELMET);
        CreativeModeTabUtil.accept(output, HumanArmorItems.WY_ELITE_CHESTPLATE);
        CreativeModeTabUtil.accept(output, HumanArmorItems.WY_ELITE_LEGGINGS);
        CreativeModeTabUtil.accept(output, HumanArmorItems.WY_ELITE_BOOTS);
    };

    private static @NotNull ItemStack applyPressureSuitDyeColor(BLibHolder<ArmorItem> pressureChestplate) {
        var chestplateItemStack = new ItemStack(pressureChestplate.get(), 1);
        chestplateItemStack = DyedItemColor.applyDyes(chestplateItemStack, List.of(DyeItem.byColor(DyeColor.BLUE)));
        return chestplateItemStack;
    }
}
