package com.human.common.property;

import com.blib.api.common.property.v1.BLibPropertyKey;
import com.blib.api.common.property.v1.serializer.BLibPropertySerializers;

public class HumanProperties {

    public static class Blocks {

        private static final BLibPropertyKey.Parent BLOCKS = BLibPropertyKey.parent("blocks");

        public static class Nuke {

            private static final BLibPropertyKey.Parent NUKE = BLOCKS.child("nuke");

            public static final HumanProperty<Boolean> ENABLED = new HumanProperty<>(
                NUKE.leaf("enabled", BLibPropertySerializers.BOOLEAN),
                false
            );
        }

        public static class Resonator {

            private static final BLibPropertyKey.Parent RESONATOR = BLOCKS.child("resonator");

            public static final HumanProperty<Integer> REPLACE_RADIUS_IN_BLOCKS = new HumanProperty<>(
                RESONATOR.leaf("replace_radius", BLibPropertySerializers.INT),
                25
            );

            public static final HumanProperty<Long> REPLACE_FREQUENCY_IN_TICKS = new HumanProperty<>(
                RESONATOR.leaf("replace_ticks", BLibPropertySerializers.LONG),
                600L
            );
        }

        public static class SentryTurret {

            private static final BLibPropertyKey.Parent SENTRY_TURRET = BLOCKS.child("sentry_turret");

            public static final HumanProperty<Integer> AMMO_CHEST_RANGE = new HumanProperty<>(
                SENTRY_TURRET.leaf("ammo_chest_range", BLibPropertySerializers.INT),
                5
            );

            public static final HumanProperty<Float> DAMAGE = new HumanProperty<>(
                SENTRY_TURRET.leaf("damage", BLibPropertySerializers.FLOAT),
                2f
            );

            public static final HumanProperty<Integer> FOV = new HumanProperty<>(
                SENTRY_TURRET.leaf("fov", BLibPropertySerializers.INT),
                45
            );

            public static final HumanProperty<Integer> RANGE = new HumanProperty<>(
                SENTRY_TURRET.leaf("range", BLibPropertySerializers.INT),
                32
            );
        }
    }

    public static class Weapons {

        private static final BLibPropertyKey.Parent WEAPONS = BLibPropertyKey.parent("weapons");

        public static final HumanProperty<Boolean> BULLETS_DAMAGE_BLOCKS_ENABLED = new HumanProperty<>(
            WEAPONS.leaf("bullets_damage_blocks_enabled", BLibPropertySerializers.BOOLEAN),
            true
        );
    }
}
