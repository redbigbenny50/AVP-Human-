package com.human.common.registry.init;

import com.blib.api.common.entity.v1.SilencedEntityTypeBuilder;
import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.blib.api.common.registry.v1.impl.BLibEntityAttributeRegistry;
import com.human.Human;
import com.human.common.gameplay.entity.living.dog.MarineDog;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.machine.SentryTurret;
import com.human.common.gameplay.entity.nuke.MushroomCloudEntity;
import com.human.common.gameplay.entity.nuke.PrimedNuke;
import com.human.common.gameplay.entity.projectile.Flamethrow;
import com.human.common.gameplay.entity.projectile.Rocket;
import com.human.common.gameplay.entity.projectile.ThrownGrenade;
import com.human.common.gameplay.util.EyeColorGenerator;
import com.human.common.gameplay.util.HairColorGenerator;
import com.human.common.gameplay.util.SkinColorGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.Vec3;

public class HumanEntityTypes {

    private static final BLibEntityAttributeRegistry ATTRIBUTE_REGISTRY = Human.MOD.registries().createEntityAttributeRegistry();

    private static final BLibRegistry<EntityType<?>> TYPE_REGISTRY = Human.MOD.registries().create(BuiltInRegistries.ENTITY_TYPE);

    public static final BLibHolder<EntityType<Flamethrow>> FLAMETHROW = create(
        "flamethrow",
        EntityType.Builder.<Flamethrow>of(Flamethrow::new, MobCategory.MISC)
            .sized(0.1F, 0.1F)
            .clientTrackingRange(8)
            .updateInterval(10)
    );

    public static final BLibHolder<EntityType<ThrownGrenade>> GRENADE_THROWN = create(
        "grenade_thrown",
        EntityType.Builder.<ThrownGrenade>of(ThrownGrenade::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
    );

    public static final BLibHolder<EntityType<MarineDog>> MARINE_DOG = create(
        "marine_dog",
        EntityType.Builder.<MarineDog>of(MarineDog::new, MobCategory.CREATURE)
            .sized(0.6F, 0.85F)
            .eyeHeight(0.68F)
            .passengerAttachments(new Vec3(0.0, 0.81875, -0.0625))
            .clientTrackingRange(10)
    );

    public static final BLibHolder<EntityType<Marine>> MARINE = create(
        "marine",
        EntityType.Builder.<Marine>of((entityType, level) -> {
            var entity = new Marine(entityType, level);

            var random = entity.getRandom();
            entity.isMale.set(random.nextBoolean());
            var isMale = entity.isMale.get();

            if (isMale) {
                entity.setBeardVariant(random.nextInt(3));
            }

            entity.eyeColor.set(EyeColorGenerator.random(random));
            entity.hairColor.set(HairColorGenerator.random(random));
            entity.hairVariant.set(random.nextInt(isMale ? 5 : 6));
            entity.skinColor.set(SkinColorGenerator.random(random));

            return entity;
        }, MobCategory.CREATURE).sized(0.6F, 1.95F)
    );

    public static final BLibHolder<EntityType<MushroomCloudEntity>> MUSHROOM_CLOUD = create(
        "mushroom_cloud",
        EntityType.Builder.of(MushroomCloudEntity::new, MobCategory.MISC)
    );

    public static final BLibHolder<EntityType<PrimedNuke>> NUKE = create(
        "nuke",
        EntityType.Builder.<PrimedNuke>of(PrimedNuke::new, MobCategory.MISC)
            .sized(0.1F, 0.1F)
            .noSummon()
            .clientTrackingRange(100)
            .updateInterval(100)
    );

    public static final BLibHolder<EntityType<Rocket>> ROCKET = create(
        "rocket",
        EntityType.Builder.<Rocket>of(Rocket::new, MobCategory.MISC)
            .sized(0.1F, 0.1F)
            .clientTrackingRange(8)
            .updateInterval(10)
    );

    public static final BLibHolder<EntityType<SentryTurret>> SENTRY_TURRET = create(
        "sentry_turret",
        EntityType.Builder.of(SentryTurret::new, MobCategory.MISC).sized(1.0F, 1.0F).noSummon()
    );

    public static <T extends Entity> BLibHolder<EntityType<T>> create(String id, EntityType.Builder<T> builder) {
        return TYPE_REGISTRY.createHolder(id, () -> ((SilencedEntityTypeBuilder) builder).blib$buildWithoutDataFixerCheck());
    }

    public static void initialize() {
        TYPE_REGISTRY.registerAll();
        ATTRIBUTE_REGISTRY.register(MARINE_DOG, Wolf::createAttributes);
        ATTRIBUTE_REGISTRY.register(MARINE, Marine::createMarineAttributes);
        ATTRIBUTE_REGISTRY.register(SENTRY_TURRET, SentryTurret::createSentryTurretAttributes);
    }
}
