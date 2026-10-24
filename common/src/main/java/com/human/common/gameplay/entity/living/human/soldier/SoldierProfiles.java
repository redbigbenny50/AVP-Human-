package com.human.common.gameplay.entity.living.human.soldier;

import com.human.common.gameplay.fabrication.Faction;
import com.human.common.gameplay.level.patrol.decorator.gear.ApeGearDecorator;
import com.human.common.gameplay.level.patrol.decorator.gear.MarineGearDecorator;
import com.human.common.gameplay.level.patrol.decorator.gear.TacticalMarineGearDecorator;
import com.human.common.gameplay.level.patrol.decorator.gear.WYCGearDecorator;
import com.human.common.gameplay.level.patrol.decorator.gear.WYEGearDecorator;
import com.human.common.gameplay.level.patrol.decorator.gear.WYSOCGearDecorator;
import com.human.common.gameplay.level.patrol.decorator.gear.WYSOEGearDecorator;
import com.human.common.registry.init.HumanEntityTypes;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Map;

/**
 * Every soldier entity type and its profile ([stated] Oct 13: the Weyland-Yutani units and the tactical marine split
 * out of the marine, each with its own spawn egg).
 * <p>
 * All of them are the same {@code Marine} class, so the squad AI, the inventory screen, the guns and everything else
 * that works for a marine works for them unchanged; only the type - and through it this profile - differs.
 * </p>
 */
public final class SoldierProfiles {

    private static final SoldierProfile MARINE = new SoldierProfile(Faction.USCM, MarineGearDecorator.INSTANCE, false);

    private static volatile Map<EntityType<?>, SoldierProfile> byType;

    private SoldierProfiles() {}

    /** The profile of a soldier type. Anything not listed is treated as a plain marine. */
    public static SoldierProfile of(EntityType<?> type) {
        return table().getOrDefault(type, MARINE);
    }

    /** Every soldier entity type, the marine first. */
    public static List<EntityType<?>> types() {
        return List.copyOf(table().keySet());
    }

    private static Map<EntityType<?>, SoldierProfile> table() {
        var table = byType;

        if (table == null) {
            // Built on first use: the types are registered by then, and never before (a static initialiser could run
            // during class loading, ahead of the registry).
            var built = new java.util.LinkedHashMap<EntityType<?>, SoldierProfile>();

            built.put(HumanEntityTypes.MARINE.get(), MARINE);
            built.put(
                HumanEntityTypes.TACTICAL_MARINE.get(),
                new SoldierProfile(Faction.USCM, TacticalMarineGearDecorator.INSTANCE, false)
            );
            built.put(HumanEntityTypes.WY_COMMANDO.get(), new SoldierProfile(Faction.WY, WYCGearDecorator.INSTANCE, true));
            built.put(HumanEntityTypes.WY_ELITE.get(), new SoldierProfile(Faction.WY, WYEGearDecorator.INSTANCE, true));
            built.put(HumanEntityTypes.WY_SPEC_OPS_COMMANDO.get(), new SoldierProfile(Faction.WY, WYSOCGearDecorator.INSTANCE, true));
            built.put(HumanEntityTypes.WY_SPEC_OPS_ELITE.get(), new SoldierProfile(Faction.WY, WYSOEGearDecorator.INSTANCE, true));
            built.put(HumanEntityTypes.WY_APE_TROOPER.get(), new SoldierProfile(Faction.WY, ApeGearDecorator.INSTANCE, true));

            table = java.util.Collections.unmodifiableMap(built);
            byType = table;
        }

        return table;
    }
}
