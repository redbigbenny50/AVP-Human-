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

        /**
         * Oct 24 - the Create converters (alternator and electric gearbox), NeoForge with Create only. [stated] "option
         * 1 and config options yes". Defaults give whole numbers at Create's speeds: 384 FE/t out and 512 FE/t in at
         * 256 RPM, both multiples of 8, and the same 75% round trip as Create Crafts & Additions.
         */
        public static class CreateConverters {

            private static final BLibPropertyKey.Parent CONVERTERS = BLOCKS.child("create_converters");

            public static final HumanProperty<Float> ALTERNATOR_FE_PER_RPM = new HumanProperty<>(
                CONVERTERS.leaf("alternator_fe_per_rpm", BLibPropertySerializers.FLOAT),
                1.5F
            );

            public static final HumanProperty<Float> GEARBOX_FE_PER_RPM = new HumanProperty<>(
                CONVERTERS.leaf("electric_gearbox_fe_per_rpm", BLibPropertySerializers.FLOAT),
                2.0F
            );

            public static final HumanProperty<Integer> GEARBOX_MIN_DRAW = new HumanProperty<>(
                CONVERTERS.leaf("electric_gearbox_min_draw", BLibPropertySerializers.INT),
                8
            );

            public static final HumanProperty<Float> STRESS_PER_RPM = new HumanProperty<>(
                CONVERTERS.leaf("stress_per_rpm", BLibPropertySerializers.FLOAT),
                64.0F
            );

            public static final HumanProperty<Integer> BUFFER = new HumanProperty<>(
                CONVERTERS.leaf("buffer", BLibPropertySerializers.INT),
                5000
            );

            public static final HumanProperty<Boolean> ALTERNATOR_UPGRADES = new HumanProperty<>(
                CONVERTERS.leaf("alternator_upgrades_enabled", BLibPropertySerializers.BOOLEAN),
                true
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
