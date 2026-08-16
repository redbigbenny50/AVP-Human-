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

            // VISUAL BUDGETS ONLY. The branch also exposed horizontal/upward/downward radius here, defaulting to
            // 128/64/32 - the same 128 that was diagnosed as the wrong crater size and corrected to a 5-chunk (80)
            // radius in NuclearExplosionUtil. Those leaves are deliberately NOT carried over; the crater geometry
            // stays with the constants that were tuned against it.
            public static final HumanProperty<Integer> CLOUD_PARTICLE_BUDGET = new HumanProperty<>(
                NUKE.leaf("cloud_particle_budget", BLibPropertySerializers.INT),
                8000
            );

            public static final HumanProperty<Float> SCREEN_FLASH_INTENSITY = new HumanProperty<>(
                NUKE.leaf("screen_flash_intensity", BLibPropertySerializers.FLOAT),
                0.85F
            );

            public static final HumanProperty<Float> SCREEN_SHAKE_INTENSITY = new HumanProperty<>(
                NUKE.leaf("screen_shake_intensity", BLibPropertySerializers.FLOAT),
                0.75F
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
