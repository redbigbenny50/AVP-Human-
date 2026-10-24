package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.HumanResources;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public class HumanSoundEvents {

    private static final BLibRegistry<SoundEvent> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.SOUND_EVENT);

    public static final BLibHolder<SoundEvent> ITEM_ARMOR_EQUIP_APE = create("item.armor.equip_ape");

    /** [stated] Oct 23 - the Weyland-Yutani music disc: "Building Better Worlds" by Rotch Gwylt. */
    public static final BLibHolder<SoundEvent> JUKEBOX_SOUNDS_BUILDING_BETTER_WORLDS = create("jukebox_sounds.building_better_worlds");

    public static final BLibHolder<SoundEvent> ITEM_ARMOR_EQUIP_MK50 = create("item.armor.equip_mk50");

    public static final BLibHolder<SoundEvent> ITEM_ARMOR_EQUIP_PRESSURE = create("item.armor.equip_pressure");

    public static final BLibHolder<SoundEvent> ITEM_ARMOR_EQUIP_STEEL = create("item.armor.equip_steel");

    public static final BLibHolder<SoundEvent> ITEM_ARMOR_EQUIP_TACTICAL = create("item.armor.equip_tactical");

    public static final BLibHolder<SoundEvent> ITEM_ARMOR_EQUIP_TITANIUM = create("item.armor.equip_titanium");

    public static final BLibHolder<SoundEvent> WEAPON_FLAMETHROWER_SEVASTOPOL_RELOAD_FINISH = create(
        "item.weapon.flamethrower_sevastopol.reload_finish"
    );

    public static final BLibHolder<SoundEvent> WEAPON_FLAMETHROWER_SEVASTOPOL_RELOAD_START = create(
        "item.weapon.flamethrower_sevastopol.reload_start"
    );

    public static final BLibHolder<SoundEvent> WEAPON_FLAMETHROWER_SEVASTOPOL_SHOOT = create(
        "item.weapon.flamethrower_sevastopol.shoot"
    );

    public static final BLibHolder<SoundEvent> WEAPON_FX_RICOCHET_DIRT = create("item.weapon.fx.ricochet.dirt");

    public static final BLibHolder<SoundEvent> WEAPON_FX_RICOCHET_GENERIC = create("item.weapon.fx.ricochet.generic");

    public static final BLibHolder<SoundEvent> WEAPON_FX_RICOCHET_GLASS = create("item.weapon.fx.ricochet.glass");

    public static final BLibHolder<SoundEvent> WEAPON_FX_RICOCHET_METAL = create("item.weapon.fx.ricochet.metal");

    public static final BLibHolder<SoundEvent> WEAPON_GENERIC_RELOAD = create("item.weapon.generic.reload");

    public static final BLibHolder<SoundEvent> WEAPON_GENERIC_SHOOT = create("item.weapon.generic.shoot");

    public static final BLibHolder<SoundEvent> WEAPON_GENERIC_SHOOT_FAIL = create("item.weapon.generic.shoot_fail");

    public static final BLibHolder<SoundEvent> WEAPON_M37_12_SHOTGUN_SHOOT = create("item.weapon.m37_12_shotgun.shoot");

    public static final BLibHolder<SoundEvent> WEAPON_M41A_PULSE_RIFLE_SHOOT = create("item.weapon.m41a_pulse_rifle.shoot");

    public static final BLibHolder<SoundEvent> WEAPON_M42A3_SNIPER_RIFLE_SHOOT = create("item.weapon.m42a3_sniper_rifle.shoot");

    public static final BLibHolder<SoundEvent> WEAPON_M4RA_BATTLE_RIFLE_SHOOT = create("item.weapon.m4ra_battle_rifle.shoot");

    public static final BLibHolder<SoundEvent> WEAPON_M56_SMARTGUN_SHOOT = create("item.weapon.m56_smartgun.shoot");

    public static final BLibHolder<SoundEvent> WEAPON_M6B_ROCKET_LAUNCHER_RELOAD_FINISH = create(
        "item.weapon.m6b_rocket_launcher.reload_finish"
    );

    public static final BLibHolder<SoundEvent> WEAPON_M6B_ROCKET_LAUNCHER_RELOAD_START = create(
        "item.weapon.m6b_rocket_launcher.reload_start"
    );

    public static final BLibHolder<SoundEvent> WEAPON_M6B_ROCKET_LAUNCHER_SHOOT = create("item.weapon.m6b_rocket_launcher.shoot");

    public static final BLibHolder<SoundEvent> WEAPON_M88_MOD_4_COMBAT_PISTOL_RELOAD = create(
        "item.weapon.m88_mod_4_combat_pistol.reload"
    );

    public static final BLibHolder<SoundEvent> WEAPON_M88_MOD_4_COMBAT_PISTOL_SHOOT = create(
        "item.weapon.m88_mod_4_combat_pistol.shoot"
    );

    public static final BLibHolder<SoundEvent> WEAPON_OLD_PAINLESS_SHOOT = create("item.weapon.old_painless.shoot");

    public static final BLibHolder<SoundEvent> WEAPON_OLD_PAINLESS_SHOOT_FINISH = create("item.weapon.old_painless.shoot_finish");

    public static final BLibHolder<SoundEvent> WEAPON_OLD_PAINLESS_SHOOT_SPINNING = create(
        "item.weapon.old_painless.shoot_spinning"
    );

    public static final BLibHolder<SoundEvent> WEAPON_OLD_PAINLESS_SHOOT_START = create("item.weapon.old_painless.shoot_start");

    /**
     * ⚠ NAMED FOR THE SYSTEM, NOT THE WEAPON. Every smart tracker shares these - the smartgun today, a second smart
     * weapon and a bolt-on module later. A gun that wants its own voice names different events on its targeting profile
     * rather than earning a branch in the playback code.
     */
    public static final BLibHolder<SoundEvent> SMART_HUD_ACQUIRE = create("item.smart_hud.acquire");

    public static final BLibHolder<SoundEvent> SMART_HUD_BOOT = create("item.smart_hud.boot");

    public static final BLibHolder<SoundEvent> SMART_HUD_LOCK = create("item.smart_hud.lock");

    public static final BLibHolder<SoundEvent> SMART_HUD_LOCK_LOST = create("item.smart_hud.lock_lost");

    public static final BLibHolder<SoundEvent> WEAPON_ZX_76_SHOTGUN_SHOOT = create("item.weapon.zx_76_shotgun.shoot");

    // ---- Tier-1 machine working loops. [stated] Sep 29: his recordings, one per machine. ----

    public static final BLibHolder<SoundEvent> MACHINE_ELECTRIC_FURNACE = create("block.machine.electric_furnace");

    // ---- Cryotube controller. [stated] Oct 2: carried over from his dropship mod's MU-TH-UR terminal. ----

    public static final BLibHolder<SoundEvent> TERMINAL_MENU_OPEN = create("gui.terminal.menu_open");

    public static final BLibHolder<SoundEvent> TERMINAL_MENU_CLOSE = create("gui.terminal.menu_close");

    public static final BLibHolder<SoundEvent> TERMINAL_MOUSEOVER = create("gui.terminal.mouseover");

    public static final BLibHolder<SoundEvent> TERMINAL_SELECT = create("gui.terminal.select");

    public static final BLibHolder<SoundEvent> TERMINAL_CONFIRM = create("gui.terminal.confirm");

    public static final BLibHolder<SoundEvent> TERMINAL_CANCEL = create("gui.terminal.cancel");

    public static final BLibHolder<SoundEvent> CRYO_CONTROLLER_ACTIVATE = create("block.cryo.controller_activate");

    public static final BLibHolder<SoundEvent> CRYO_CONTROLLER_DEACTIVATE = create("block.cryo.controller_deactivate");

    public static final BLibHolder<SoundEvent> MACHINE_ELECTRIC_SMOKER = create("block.machine.electric_smoker");

    public static final BLibHolder<SoundEvent> MACHINE_ELECTRIC_BLAST_FURNACE = create("block.machine.electric_blast_furnace");

    public static final BLibHolder<SoundEvent> MACHINE_ELECTRIC_INDUSTRIAL_FURNACE = create(
        "block.machine.electric_industrial_furnace"
    );

    public static final BLibHolder<SoundEvent> MACHINE_ELECTRIC_GRINDER = create("block.machine.electric_grinder");

    /** ⚠ A 4-second, TWO-stroke loop: the second stroke is pitched down with a duller ping. Keep it one sound. */
    public static final BLibHolder<SoundEvent> MACHINE_ELECTRIC_PRESS = create("block.machine.electric_press");

    public static final BLibHolder<SoundEvent> MACHINE_ELECTRIC_HYDRATOR = create("block.machine.electric_hydrator");

    public static final BLibHolder<SoundEvent> MACHINE_ELECTRIC_FREEZER = create("block.machine.electric_freezer");

    /**
     * [stated] Oct 19 - "steam sound for the vaporizor while its working". One steam release, about four seconds from
     * hiss to silence; looped as the working sound, it vents every few seconds while the vaporizer runs.
     */
    public static final BLibHolder<SoundEvent> MACHINE_VAPORIZER = create("block.machine.vaporizer");

    /**
     * [stated] Oct 19 - the distributor "working sending out the vapor". One full cycle of the recording's machine -
     * the steam, the screech, the short lull - cut from one quiet point to the next, so it repeats like the machine
     * itself.
     */
    public static final BLibHolder<SoundEvent> MACHINE_DISTRIBUTOR = create("block.machine.distributor");

    /**
     * Oct 5 - squad callouts: a marine that is hit keys its radio, and squadmates answer. Eight scrambled radio clips,
     * cut from the chatter loops he supplied and distorted past the point of being words.
     */
    public static final BLibHolder<SoundEvent> MARINE_RADIO_CHATTER = create("entity.marine.radio_chatter");

    /** [stated] Oct 19 - "play this when enemies freeze solid". Played by FrozenSolid the moment a mob freezes. */
    public static final BLibHolder<SoundEvent> FREEZE_SOLID = create("entity.generic.freeze_solid");

    /** [stated] Oct 19 - a frozen mob broken out of the ice by a real attack or a heavy fall. FrozenSolid.onHurt. */
    public static final BLibHolder<SoundEvent> FROZEN_BREAK = create("entity.generic.frozen_break");

    /** [stated] Oct 19 - the quarry drill bit: a seamless loop played at the bit (client QuarryDrillSound). */
    public static final BLibHolder<SoundEvent> QUARRY_DRILL = create("block.quarry.drill_bit");

    /** [stated] Oct 19 - the quarry's drill head at the surface while it runs: a seamless loop at the head. */
    public static final BLibHolder<SoundEvent> QUARRY_DRILL_HEAD = create("block.quarry.drill_head");

    /** [stated] Oct 19 - the quarry's gantry arms moving the head between chunks: a servo loop. */
    public static final BLibHolder<SoundEvent> QUARRY_ARMS = create("block.quarry.arms_move");

    /**
     * [stated] Oct 19 - the chem thrower's spray, cut from one steam-leak recording into three: a fade-in when the
     * trigger goes down, a seamless loop while it is held, a fade-out when it is let go. Client ChemThrowerSound.
     */
    public static final BLibHolder<SoundEvent> WEAPON_CHEM_THROWER_START = create("item.weapon.chem_thrower.start");

    public static final BLibHolder<SoundEvent> WEAPON_CHEM_THROWER_LOOP = create("item.weapon.chem_thrower.loop");

    public static final BLibHolder<SoundEvent> WEAPON_CHEM_THROWER_STOP = create("item.weapon.chem_thrower.stop");

    private static BLibHolder<SoundEvent> create(String path) {
        return REGISTRY.createHolder(path, () -> SoundEvent.createVariableRangeEvent(HumanResources.location(path)));
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
