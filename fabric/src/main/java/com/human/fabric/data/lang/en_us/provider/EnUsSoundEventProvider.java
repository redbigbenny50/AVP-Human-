package com.human.fabric.data.lang.en_us.provider;

import com.human.common.registry.init.HumanSoundEvents;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EnUsSoundEventProvider {

    public static final Consumer<FabricLanguageProvider.TranslationBuilder> CONSUMER = builder -> {
        addSound(builder, HumanSoundEvents.ITEM_ARMOR_EQUIP_APE, "APE armor rustles");
        addSound(builder, HumanSoundEvents.MACHINE_ELECTRIC_FURNACE, "Electric furnace hums");
        addSound(builder, HumanSoundEvents.CRYO_CONTROLLER_ACTIVATE, "Cryotube controller powers up");
        addSound(builder, HumanSoundEvents.CRYO_CONTROLLER_DEACTIVATE, "Cryotube controller powers down");
        addSound(builder, HumanSoundEvents.MACHINE_ELECTRIC_SMOKER, "Electric smoker hums");
        addSound(builder, HumanSoundEvents.MACHINE_ELECTRIC_BLAST_FURNACE, "Electric blast furnace roars");
        addSound(builder, HumanSoundEvents.MACHINE_ELECTRIC_INDUSTRIAL_FURNACE, "Industrial furnace roars");
        addSound(builder, HumanSoundEvents.MACHINE_ELECTRIC_GRINDER, "Grinder grinds");
        addSound(builder, HumanSoundEvents.MACHINE_ELECTRIC_PRESS, "Press stamps");
        addSound(builder, HumanSoundEvents.MACHINE_ELECTRIC_HYDRATOR, "Hydrator sprays");
        addSound(builder, HumanSoundEvents.MACHINE_ELECTRIC_FREEZER, "Freezer hums");
        addSound(builder, HumanSoundEvents.ITEM_ARMOR_EQUIP_MK50, "MK50 armor rustles");
        addSound(builder, HumanSoundEvents.ITEM_ARMOR_EQUIP_PRESSURE, "Pressure armor rustles");
        addSound(builder, HumanSoundEvents.ITEM_ARMOR_EQUIP_STEEL, "Steel armor clanks");
        addSound(builder, HumanSoundEvents.ITEM_ARMOR_EQUIP_TACTICAL, "Tactical armor rustles");
        addSound(builder, HumanSoundEvents.ITEM_ARMOR_EQUIP_TITANIUM, "Titanium armor clanks");
        addSound(builder, HumanSoundEvents.MARINE_RADIO_CHATTER, "Marine radio chatter");

        // ⚠ ALL FOUR SMART HUD CUES NEED SUBTITLES, NOT JUST FLAVOUR. The lock tone grants accuracy and armour
        // penetration, so a player without sound loses a MECHANIC if it is unsubtitled, not just some atmosphere.
        addSound(builder, HumanSoundEvents.SMART_HUD_ACQUIRE, "Targeting system detects contact");
        addSound(builder, HumanSoundEvents.SMART_HUD_BOOT, "Targeting system powers on");
        addSound(builder, HumanSoundEvents.SMART_HUD_LOCK, "Targeting system locks on");
        addSound(builder, HumanSoundEvents.SMART_HUD_LOCK_LOST, "Targeting system loses lock");

        addSound(builder, HumanSoundEvents.WEAPON_FLAMETHROWER_SEVASTOPOL_RELOAD_FINISH, "Flamethrower finishes reloading");
        addSound(builder, HumanSoundEvents.WEAPON_FLAMETHROWER_SEVASTOPOL_RELOAD_START, "Flamethrower reloads");
        addSound(builder, HumanSoundEvents.WEAPON_FLAMETHROWER_SEVASTOPOL_SHOOT, "Flamethrower shoots");
        addSound(builder, HumanSoundEvents.WEAPON_FX_RICOCHET_DIRT, "Bullet ricochets off of dirt");
        addSound(builder, HumanSoundEvents.WEAPON_FX_RICOCHET_GENERIC, "Bullet ricochets");
        addSound(builder, HumanSoundEvents.WEAPON_FX_RICOCHET_GLASS, "Bullet ricochets off of glass");
        addSound(builder, HumanSoundEvents.WEAPON_FX_RICOCHET_METAL, "Bullet ricochets off of metal");
        addSound(builder, HumanSoundEvents.WEAPON_GENERIC_RELOAD, "Gun reloads");
        addSound(builder, HumanSoundEvents.WEAPON_GENERIC_SHOOT, "Gun shoots");
        addSound(builder, HumanSoundEvents.WEAPON_GENERIC_SHOOT_FAIL, "Gun fails to shoot");
        addSound(builder, HumanSoundEvents.WEAPON_M37_12_SHOTGUN_SHOOT, "M37-12 Shotgun shoots");
        addSound(builder, HumanSoundEvents.WEAPON_M41A_PULSE_RIFLE_SHOOT, "M41A Pulse Rifle shoots");
        addSound(builder, HumanSoundEvents.WEAPON_M42A3_SNIPER_RIFLE_SHOOT, "M42A3 Sniper Rifle shoots");
        addSound(builder, HumanSoundEvents.WEAPON_M4RA_BATTLE_RIFLE_SHOOT, "M4RA Battle Rifle shoots");
        addSound(builder, HumanSoundEvents.WEAPON_M56_SMARTGUN_SHOOT, "M56 Smartgun shoots");
        addSound(builder, HumanSoundEvents.WEAPON_M6B_ROCKET_LAUNCHER_RELOAD_FINISH, "M6B Rocket Launcher finishes reloading");
        addSound(builder, HumanSoundEvents.WEAPON_M6B_ROCKET_LAUNCHER_RELOAD_START, "M6B Rocket Launcher reloads");
        addSound(builder, HumanSoundEvents.WEAPON_M6B_ROCKET_LAUNCHER_SHOOT, "M6B Rocket Launcher shoots");
        addSound(builder, HumanSoundEvents.WEAPON_M88_MOD_4_COMBAT_PISTOL_RELOAD, "M88 Mod 4 Combat Pistol reloads");
        addSound(builder, HumanSoundEvents.WEAPON_M88_MOD_4_COMBAT_PISTOL_SHOOT, "M88 Mod 4 Combat Pistol shoots");
        addSound(builder, HumanSoundEvents.WEAPON_OLD_PAINLESS_SHOOT, "Old painless shoots");
        addSound(builder, HumanSoundEvents.WEAPON_OLD_PAINLESS_SHOOT_FINISH, "Old Painless stops shooting");
        addSound(builder, HumanSoundEvents.WEAPON_OLD_PAINLESS_SHOOT_SPINNING, "Old Painless barrel spins");
        addSound(builder, HumanSoundEvents.WEAPON_OLD_PAINLESS_SHOOT_START, "Old Painless barrel starts spinning");
        addSound(builder, HumanSoundEvents.WEAPON_ZX_76_SHOTGUN_SHOOT, "ZX-76 Shotgun shoots");
    };

    private static void addSound(
        FabricLanguageProvider.TranslationBuilder translationBuilder,
        Supplier<SoundEvent> soundEventSupplier,
        String value
    ) {
        addSound(translationBuilder, soundEventSupplier.get(), value);
    }

    private static void addSound(FabricLanguageProvider.TranslationBuilder translationBuilder, SoundEvent soundEvent, String value) {
        translationBuilder.add("subtitles." + soundEvent.getLocation().getPath(), value);
    }
}
