package com.human.neoforge.compat.create;

import com.human.common.property.HumanProperties.Blocks.CreateConverters;
import com.human.common.property.HumanPropertyAccess;

/** The converters' tunable numbers, read from avp_human.properties (the create_converters section). */
public final class ConverterSettings {

    private ConverterSettings() {}

    public static float alternatorFePerRpm() {
        return Math.max(0F, HumanPropertyAccess.INSTANCE.get(CreateConverters.ALTERNATOR_FE_PER_RPM));
    }

    public static float gearboxFePerRpm() {
        return Math.max(0F, HumanPropertyAccess.INSTANCE.get(CreateConverters.GEARBOX_FE_PER_RPM));
    }

    public static long gearboxMinDraw() {
        return Math.max(0, HumanPropertyAccess.INSTANCE.get(CreateConverters.GEARBOX_MIN_DRAW));
    }

    public static float stressPerRpm() {
        return Math.max(0F, HumanPropertyAccess.INSTANCE.get(CreateConverters.STRESS_PER_RPM));
    }

    public static long buffer() {
        return Math.max(1, HumanPropertyAccess.INSTANCE.get(CreateConverters.BUFFER));
    }

    public static boolean alternatorUpgrades() {
        return HumanPropertyAccess.INSTANCE.get(CreateConverters.ALTERNATOR_UPGRADES);
    }
}
