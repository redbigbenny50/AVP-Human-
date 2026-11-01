package com.human.fabric.data.gene_bonus_data;

import com.blib.api.common.entity.v1.EntityTypePredicate;
import com.human.Human;
import com.human.common.gameplay.gene.Gene;
import com.human.common.gameplay.gene.GeneBonusData;
import com.human.common.gameplay.gene.GeneBonusDataEntry;
import com.human.common.gameplay.gene.GeneTier;
import com.human.common.gameplay.gene.Genes;
import com.human.common.registry.init.HumanEntityTypes;
import com.just.core.functional.tuple.Tuple2;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Species gene data for vanilla and avp_human mobs, written as the team-reviewed donor table (Oct 25/26).
 * <p>
 * Each entry names a gene and the donor's TIER for it; the value is derived from the gene's cap
 * ({@link GeneTier#speciesValue}), so re-tuning a cap in {@link Genes} rescales every donor at once. WEAKNESS entries
 * carry the gene as a penalty. Undead, constructs, spirits, elementals and bosses carry no genes. xenomorph data lives
 * in avp_alien, yautja data in avp_predator.
 * </p>
 */
public class GeneBonusDataSubProvider extends GeneBonusDataProvider {

    private final List<EntityType<?>> touchedEntries;

    public GeneBonusDataSubProvider(FabricDataOutput output) {
        super(output);
        this.touchedEntries = new ArrayList<>();
    }

    @Override
    protected void generate() {
        add(
            EntityType.ARMADILLO,
            List.of(
                gene(Genes.ARMOR, GeneTier.PRIME),
                gene(Genes.ARMOR_TOUGHNESS, GeneTier.STRONG),
                gene(Genes.STURDINESS, GeneTier.AVERAGE),
                gene(Genes.MINING_SPEED, GeneTier.STRONG)
            )
        );
        add(
            EntityType.AXOLOTL,
            List.of(
                gene(Genes.SWIM_SPEED, GeneTier.AVERAGE),
                gene(Genes.AIR_CAPACITY, GeneTier.STRONG),
                gene(Genes.LUCK, GeneTier.STRONG),
                gene(Genes.REGENERATION, GeneTier.PRIME),
                gene(Genes.COLD_BLOODED, GeneTier.STRONG)
            )
        );
        add(
            EntityType.BAT,
            List.of(
                gene(Genes.LIGHTWEIGHT, GeneTier.AVERAGE),
                gene(Genes.NIGHT_VISION, GeneTier.STRONG)
            )
        );
        add(
            EntityType.BEE,
            List.of(
                gene(Genes.LIGHTWEIGHT, GeneTier.STRONG),
                gene(Genes.TOXIN_RESISTANCE, GeneTier.STRONG),
                gene(Genes.POISON, GeneTier.STRONG),
                gene(Genes.POISONOUS_BARBS, GeneTier.STRONG),
                gene(Genes.HONEY, GeneTier.PRIME)
            )
        );
        add(
            EntityType.CAMEL,
            List.of(
                gene(Genes.STEP_HEIGHT, GeneTier.PRIME),
                gene(Genes.METABOLISM, GeneTier.PRIME),
                gene(Genes.FIRE_RESISTANCE, GeneTier.WEAK),
                gene(Genes.COLD_RESISTANCE, GeneTier.WEAKNESS)
            )
        );
        add(
            EntityType.CAT,
            List.of(
                gene(Genes.FALL_RESISTANCE, GeneTier.PRIME),
                gene(Genes.SNEAK_SPEED, GeneTier.PRIME),
                gene(Genes.STEALTH, GeneTier.STRONG),
                gene(Genes.NIGHT_VISION, GeneTier.PRIME),
                gene(Genes.ATTACK_SPEED, GeneTier.AVERAGE),
                gene(Genes.LUCK, GeneTier.AVERAGE)
            )
        );
        add(
            EntityType.CAVE_SPIDER,
            List.of(
                gene(Genes.MAX_HEALTH, GeneTier.WEAKNESS),
                gene(Genes.TOXIN_RESISTANCE, GeneTier.STRONG),
                gene(Genes.POISON, GeneTier.PRIME)
            )
        );
        add(
            EntityType.CHICKEN,
            List.of(
                gene(Genes.FALL_RESISTANCE, GeneTier.STRONG),
                gene(Genes.LIGHTWEIGHT, GeneTier.PRIME),
                gene(Genes.ACCELERATED_GROWTH, GeneTier.PRIME),
                gene(Genes.RAPID_GESTATION, GeneTier.AVERAGE),
                gene(Genes.FERTILITY, GeneTier.PRIME),
                gene(Genes.EGG_LAYING, GeneTier.PRIME)
            )
        );
        add(
            EntityType.COD,
            List.of(
                gene(Genes.SWIM_SPEED, GeneTier.AVERAGE),
                gene(Genes.COLD_BLOODED, GeneTier.AVERAGE)
            )
        );
        add(
            HumanEntityTypes.COLONIST.get(),
            List.of(
                gene(Genes.LUCK, GeneTier.AVERAGE),
                gene(Genes.INTELLIGENCE, GeneTier.PRIME)
            )
        );
        add(
            EntityType.COW,
            List.of(
                gene(Genes.MAX_HEALTH, GeneTier.WEAK),
                gene(Genes.KNOCKBACK_RESISTANCE, GeneTier.WEAK),
                gene(Genes.BONUS_EMBRYO_COUNT, GeneTier.WEAK),
                gene(Genes.INTELLIGENCE, GeneTier.WEAKNESS),
                gene(Genes.MILKABLE, GeneTier.PRIME),
                gene(Genes.MILK, GeneTier.PRIME)
            )
        );
        add(
            EntityType.CREEPER,
            List.of(
                gene(Genes.STURDINESS, GeneTier.WEAKNESS),
                gene(Genes.ACID_VOLATILITY, GeneTier.STRONG)
            )
        );
        add(
            EntityType.DOLPHIN,
            List.of(
                gene(Genes.SWIM_SPEED, GeneTier.PRIME),
                gene(Genes.AIR_CAPACITY, GeneTier.STRONG)
            )
        );
        add(
            EntityType.DONKEY,
            List.of(
                gene(Genes.MOVE_SPEED, GeneTier.AVERAGE),
                gene(Genes.STEP_HEIGHT, GeneTier.AVERAGE)
            )
        );
        add(
            EntityType.ELDER_GUARDIAN,
            List.of(
                gene(Genes.MAX_HEALTH, GeneTier.STRONG),
                gene(Genes.SWIM_SPEED, GeneTier.STRONG),
                gene(Genes.AIR_CAPACITY, GeneTier.STRONG),
                gene(Genes.THORNS, GeneTier.PRIME)
            )
        );
        add(
            EntityType.ENDERMAN,
            List.of(
                gene(Genes.NIGHT_VISION, GeneTier.AVERAGE),
                gene(Genes.WARP, GeneTier.PRIME)
            )
        );
        add(
            EntityType.ENDERMITE,
            List.of(
                gene(Genes.WARP, GeneTier.STRONG)
            )
        );
        add(
            EntityType.EVOKER,
            List.of(
                gene(Genes.INTELLIGENCE, GeneTier.STRONG)
            )
        );
        add(
            EntityType.FOX,
            List.of(
                gene(Genes.MOVE_SPEED, GeneTier.AVERAGE),
                gene(Genes.JUMP_HEIGHT, GeneTier.WEAK),
                gene(Genes.SNEAK_SPEED, GeneTier.STRONG),
                gene(Genes.STEALTH, GeneTier.AVERAGE),
                gene(Genes.KEEN_SENSES, GeneTier.STRONG),
                gene(Genes.NIGHT_VISION, GeneTier.AVERAGE),
                gene(Genes.ATTACK_SPEED, GeneTier.PRIME),
                gene(Genes.FORAGER, GeneTier.PRIME),
                gene(Genes.INTELLIGENCE, GeneTier.AVERAGE)
            )
        );
        add(
            EntityType.FROG,
            List.of(
                gene(Genes.JUMP_HEIGHT, GeneTier.STRONG),
                gene(Genes.FALL_RESISTANCE, GeneTier.AVERAGE),
                gene(Genes.AIR_CAPACITY, GeneTier.AVERAGE),
                gene(Genes.STEALTH, GeneTier.AVERAGE),
                gene(Genes.COLD_BLOODED, GeneTier.PRIME),
                gene(Genes.BONUS_PARASITE_COUNT, GeneTier.STRONG),
                gene(Genes.ACCELERATED_GROWTH, GeneTier.AVERAGE),
                gene(Genes.FERTILITY, GeneTier.STRONG)
            )
        );
        add(
            EntityType.GHAST,
            List.of(
                gene(Genes.LIGHTWEIGHT, GeneTier.STRONG)
            )
        );
        add(
            EntityType.GLOW_SQUID,
            List.of(
                gene(Genes.AIR_CAPACITY, GeneTier.WEAK),
                gene(Genes.INK, GeneTier.PRIME)
            )
        );
        add(
            EntityType.GOAT,
            List.of(
                gene(Genes.ATTACK_DAMAGE, GeneTier.WEAK),
                gene(Genes.STEP_HEIGHT, GeneTier.STRONG),
                gene(Genes.JUMP_HEIGHT, GeneTier.STRONG),
                gene(Genes.FALL_RESISTANCE, GeneTier.AVERAGE),
                gene(Genes.KNOCKBACK, GeneTier.PRIME),
                gene(Genes.COLD_RESISTANCE, GeneTier.STRONG),
                gene(Genes.ARMOR_PENETRATION, GeneTier.WEAK),
                gene(Genes.MILKABLE, GeneTier.PRIME),
                gene(Genes.MILK, GeneTier.PRIME)
            )
        );
        add(
            EntityType.GUARDIAN,
            List.of(
                gene(Genes.SWIM_SPEED, GeneTier.STRONG),
                gene(Genes.AIR_CAPACITY, GeneTier.AVERAGE),
                gene(Genes.THORNS, GeneTier.STRONG),
                gene(Genes.COLD_BLOODED, GeneTier.AVERAGE)
            )
        );
        add(
            EntityType.HOGLIN,
            List.of(
                gene(Genes.MAX_HEALTH, GeneTier.STRONG),
                gene(Genes.KNOCKBACK_RESISTANCE, GeneTier.STRONG),
                gene(Genes.KNOCKBACK, GeneTier.STRONG),
                gene(Genes.FIRE_RESISTANCE, GeneTier.STRONG),
                gene(Genes.ARMOR_PENETRATION, GeneTier.AVERAGE)
            )
        );
        add(
            EntityType.HORSE,
            List.of(
                gene(Genes.MAX_HEALTH, GeneTier.AVERAGE),
                gene(Genes.MOVE_SPEED, GeneTier.PRIME),
                gene(Genes.STEP_HEIGHT, GeneTier.STRONG),
                gene(Genes.JUMP_HEIGHT, GeneTier.AVERAGE)
            )
        );
        add(
            EntityType.ILLUSIONER,
            List.of(
                gene(Genes.INTELLIGENCE, GeneTier.STRONG)
            )
        );
        add(
            EntityType.LLAMA,
            List.of(
                gene(Genes.MOVE_SPEED, GeneTier.WEAK),
                gene(Genes.STEP_HEIGHT, GeneTier.WEAK),
                gene(Genes.KEEN_SENSES, GeneTier.WEAK),
                gene(Genes.METABOLISM, GeneTier.AVERAGE),
                gene(Genes.COLD_RESISTANCE, GeneTier.WEAK)
            )
        );
        add(
            EntityType.MAGMA_CUBE,
            List.of(
                gene(Genes.FIRE_RESISTANCE, GeneTier.STRONG),
                gene(Genes.SLIME, GeneTier.PRIME)
            )
        );
        add(
            HumanEntityTypes.MARINE.get(),
            List.of(
                gene(Genes.INTELLIGENCE, GeneTier.PRIME)
            )
        );
        add(
            HumanEntityTypes.MARINE_DOG.get(),
            List.of(
                gene(Genes.KEEN_SENSES, GeneTier.PRIME),
                gene(Genes.MOVE_SPEED, GeneTier.AVERAGE)
            )
        );
        add(
            EntityType.MOOSHROOM,
            List.of(
                gene(Genes.RADIATION_RESISTANCE, GeneTier.STRONG),
                gene(Genes.MILKABLE, GeneTier.PRIME),
                gene(Genes.BOWL_FEEDING, GeneTier.PRIME),
                gene(Genes.STEW, GeneTier.PRIME)
            )
        );
        add(
            EntityType.MULE,
            List.of(
                gene(Genes.MOVE_SPEED, GeneTier.WEAK),
                gene(Genes.STEP_HEIGHT, GeneTier.AVERAGE)
            )
        );
        add(
            EntityType.OCELOT,
            List.of(
                gene(Genes.MOVE_SPEED, GeneTier.STRONG),
                gene(Genes.FALL_RESISTANCE, GeneTier.STRONG),
                gene(Genes.SNEAK_SPEED, GeneTier.STRONG),
                gene(Genes.STEALTH, GeneTier.PRIME),
                gene(Genes.ATTACK_SPEED, GeneTier.STRONG)
            )
        );
        add(
            EntityType.PANDA,
            List.of(
                gene(Genes.ATTACK_DAMAGE, GeneTier.WEAKNESS),
                gene(Genes.MOVE_SPEED, GeneTier.WEAKNESS),
                gene(Genes.KEEN_SENSES, GeneTier.WEAKNESS),
                gene(Genes.METABOLISM, GeneTier.STRONG)
            )
        );
        add(
            EntityType.PARROT,
            List.of(
                gene(Genes.FALL_RESISTANCE, GeneTier.WEAK),
                gene(Genes.LIGHTWEIGHT, GeneTier.STRONG),
                gene(Genes.LUCK, GeneTier.STRONG),
                gene(Genes.FORAGER, GeneTier.AVERAGE)
            )
        );
        add(
            EntityType.PIG,
            List.of(
                gene(Genes.MAX_HEALTH, GeneTier.WEAKNESS),
                gene(Genes.FORAGER, GeneTier.STRONG),
                gene(Genes.METABOLISM, GeneTier.WEAKNESS),
                gene(Genes.BONUS_EMBRYO_COUNT, GeneTier.PRIME),
                gene(Genes.RAPID_GESTATION, GeneTier.STRONG),
                gene(Genes.INTELLIGENCE, GeneTier.WEAKNESS)
            )
        );
        add(
            EntityType.PIGLIN,
            List.of(
                gene(Genes.KEEN_SENSES, GeneTier.AVERAGE),
                gene(Genes.FIRE_RESISTANCE, GeneTier.AVERAGE),
                gene(Genes.INTELLIGENCE, GeneTier.STRONG)
            )
        );
        add(
            EntityType.PIGLIN_BRUTE,
            List.of(
                gene(Genes.ATTACK_DAMAGE, GeneTier.STRONG),
                gene(Genes.FIRE_RESISTANCE, GeneTier.AVERAGE),
                gene(Genes.INTELLIGENCE, GeneTier.STRONG)
            )
        );
        add(
            EntityType.PILLAGER,
            List.of(
                gene(Genes.INTELLIGENCE, GeneTier.STRONG)
            )
        );
        add(
            EntityType.PLAYER,
            List.of(
                gene(Genes.INTELLIGENCE, GeneTier.PRIME)
            )
        );
        add(
            EntityType.POLAR_BEAR,
            List.of(
                gene(Genes.MAX_HEALTH, GeneTier.AVERAGE),
                gene(Genes.ATTACK_DAMAGE, GeneTier.AVERAGE),
                gene(Genes.KNOCKBACK_RESISTANCE, GeneTier.AVERAGE),
                gene(Genes.KNOCKBACK, GeneTier.WEAK),
                gene(Genes.COLD_RESISTANCE, GeneTier.PRIME)
            )
        );
        add(
            EntityType.PUFFERFISH,
            List.of(
                gene(Genes.ATTACK_DAMAGE, GeneTier.WEAKNESS),
                gene(Genes.POISON, GeneTier.STRONG),
                gene(Genes.POISONOUS_BARBS, GeneTier.PRIME),
                gene(Genes.THORNS, GeneTier.STRONG)
            )
        );
        add(
            EntityType.RABBIT,
            List.of(
                gene(Genes.ARMOR, GeneTier.WEAKNESS),
                gene(Genes.MOVE_SPEED, GeneTier.AVERAGE),
                gene(Genes.JUMP_HEIGHT, GeneTier.PRIME),
                gene(Genes.MINING_SPEED, GeneTier.AVERAGE),
                gene(Genes.LUCK, GeneTier.PRIME),
                gene(Genes.BONUS_EMBRYO_COUNT, GeneTier.STRONG),
                gene(Genes.ACCELERATED_GROWTH, GeneTier.STRONG),
                gene(Genes.RAPID_GESTATION, GeneTier.PRIME)
            )
        );
        add(
            EntityType.RAVAGER,
            List.of(
                gene(Genes.MAX_HEALTH, GeneTier.STRONG),
                gene(Genes.ATTACK_DAMAGE, GeneTier.STRONG),
                gene(Genes.KNOCKBACK_RESISTANCE, GeneTier.PRIME),
                gene(Genes.KNOCKBACK, GeneTier.STRONG),
                gene(Genes.STURDINESS, GeneTier.PRIME),
                gene(Genes.ARMOR_PENETRATION, GeneTier.STRONG)
            )
        );
        add(
            EntityType.SALMON,
            List.of(
                gene(Genes.SWIM_SPEED, GeneTier.AVERAGE),
                gene(Genes.COLD_BLOODED, GeneTier.AVERAGE)
            )
        );
        add(
            EntityType.SHEEP,
            List.of(
                gene(Genes.COLD_RESISTANCE, GeneTier.AVERAGE),
                gene(Genes.BONUS_EMBRYO_COUNT, GeneTier.AVERAGE),
                gene(Genes.RAPID_GESTATION, GeneTier.WEAK),
                gene(Genes.SHEARABLE, GeneTier.PRIME)
            )
        );
        add(
            EntityType.SHULKER,
            List.of(
                gene(Genes.ARMOR, GeneTier.WEAK),
                gene(Genes.ARMOR_TOUGHNESS, GeneTier.WEAK),
                gene(Genes.WARP, GeneTier.STRONG)
            )
        );
        add(
            EntityType.SILVERFISH,
            List.of(
                gene(Genes.MINING_SPEED, GeneTier.STRONG)
            )
        );
        add(
            EntityType.SLIME,
            List.of(
                gene(Genes.REGENERATION, GeneTier.STRONG),
                gene(Genes.SLIME, GeneTier.PRIME)
            )
        );
        add(
            EntityType.SNIFFER,
            List.of(
                gene(Genes.ATTACK_DAMAGE, GeneTier.WEAKNESS),
                gene(Genes.MOVE_SPEED, GeneTier.WEAKNESS),
                gene(Genes.ATTACK_SPEED, GeneTier.WEAKNESS),
                gene(Genes.MINING_SPEED, GeneTier.PRIME),
                gene(Genes.METABOLISM, GeneTier.AVERAGE),
                gene(Genes.BONUS_PARASITE_COUNT, GeneTier.PRIME)
            )
        );
        add(
            EntityType.SPIDER,
            List.of(
                gene(Genes.SNEAK_SPEED, GeneTier.AVERAGE),
                gene(Genes.STEALTH, GeneTier.AVERAGE),
                gene(Genes.NIGHT_VISION, GeneTier.WEAK),
                gene(Genes.ATTACK_SPEED, GeneTier.STRONG),
                gene(Genes.TOXIN_RESISTANCE, GeneTier.PRIME)
            )
        );
        add(
            EntityType.SQUID,
            List.of(
                gene(Genes.SWIM_SPEED, GeneTier.WEAK),
                gene(Genes.AIR_CAPACITY, GeneTier.AVERAGE),
                gene(Genes.INK, GeneTier.PRIME)
            )
        );
        add(
            EntityType.STRIDER,
            List.of(
                gene(Genes.FIRE_RESISTANCE, GeneTier.PRIME),
                gene(Genes.COLD_RESISTANCE, GeneTier.WEAKNESS)
            )
        );
        add(
            HumanEntityTypes.TACTICAL_MARINE.get(),
            List.of(
                gene(Genes.INTELLIGENCE, GeneTier.PRIME),
                gene(Genes.KEEN_SENSES, GeneTier.AVERAGE)
            )
        );
        add(
            EntityType.TRADER_LLAMA,
            List.of(
                gene(Genes.MOVE_SPEED, GeneTier.WEAK),
                gene(Genes.STEP_HEIGHT, GeneTier.WEAK),
                gene(Genes.KEEN_SENSES, GeneTier.WEAK),
                gene(Genes.METABOLISM, GeneTier.AVERAGE),
                gene(Genes.COLD_RESISTANCE, GeneTier.WEAK)
            )
        );
        add(
            EntityType.TROPICAL_FISH,
            List.of(
                gene(Genes.SWIM_SPEED, GeneTier.AVERAGE)
            )
        );
        add(
            EntityType.TURTLE,
            List.of(
                gene(Genes.ARMOR, GeneTier.STRONG),
                gene(Genes.ARMOR_TOUGHNESS, GeneTier.STRONG),
                gene(Genes.MOVE_SPEED, GeneTier.WEAKNESS),
                gene(Genes.SWIM_SPEED, GeneTier.STRONG),
                gene(Genes.JUMP_HEIGHT, GeneTier.WEAKNESS),
                gene(Genes.AIR_CAPACITY, GeneTier.PRIME),
                gene(Genes.STURDINESS, GeneTier.AVERAGE),
                gene(Genes.REGENERATION, GeneTier.AVERAGE),
                gene(Genes.COLD_BLOODED, GeneTier.STRONG),
                gene(Genes.BONUS_PARASITE_COUNT, GeneTier.STRONG),
                gene(Genes.ACCELERATED_GROWTH, GeneTier.WEAKNESS),
                gene(Genes.FERTILITY, GeneTier.STRONG),
                gene(Genes.EGG_LAYING, GeneTier.PRIME)
            )
        );
        add(
            EntityType.VILLAGER,
            List.of(
                gene(Genes.INTELLIGENCE, GeneTier.PRIME)
            )
        );
        add(
            EntityType.VINDICATOR,
            List.of(
                gene(Genes.ATTACK_DAMAGE, GeneTier.STRONG),
                gene(Genes.INTELLIGENCE, GeneTier.STRONG)
            )
        );
        add(
            EntityType.WANDERING_TRADER,
            List.of(
                gene(Genes.INTELLIGENCE, GeneTier.PRIME)
            )
        );
        add(
            EntityType.WARDEN,
            List.of(
                gene(Genes.MAX_HEALTH, GeneTier.STRONG),
                gene(Genes.ATTACK_DAMAGE, GeneTier.STRONG),
                gene(Genes.KNOCKBACK_RESISTANCE, GeneTier.PRIME),
                gene(Genes.KEEN_SENSES, GeneTier.PRIME),
                gene(Genes.NIGHT_VISION, GeneTier.WEAKNESS),
                gene(Genes.STURDINESS, GeneTier.STRONG),
                gene(Genes.ARMOR_PENETRATION, GeneTier.PRIME)
            )
        );
        add(
            EntityType.WITCH,
            List.of(
                gene(Genes.TOXIN_RESISTANCE, GeneTier.STRONG)
            )
        );
        add(
            EntityType.WOLF,
            List.of(
                gene(Genes.MAX_HEALTH, GeneTier.WEAKNESS),
                gene(Genes.ATTACK_DAMAGE, GeneTier.AVERAGE),
                gene(Genes.KEEN_SENSES, GeneTier.PRIME),
                gene(Genes.ATTACK_SPEED, GeneTier.AVERAGE),
                gene(Genes.COLD_RESISTANCE, GeneTier.AVERAGE),
                gene(Genes.BONUS_EMBRYO_COUNT, GeneTier.AVERAGE),
                gene(Genes.INTELLIGENCE, GeneTier.AVERAGE)
            )
        );
        add(
            HumanEntityTypes.WY_APE_TROOPER.get(),
            List.of(
                gene(Genes.INTELLIGENCE, GeneTier.PRIME),
                gene(Genes.MAX_HEALTH, GeneTier.WEAK)
            )
        );
        add(
            HumanEntityTypes.WY_COMMANDO.get(),
            List.of(
                gene(Genes.INTELLIGENCE, GeneTier.PRIME),
                gene(Genes.ATTACK_DAMAGE, GeneTier.WEAK)
            )
        );
        add(
            HumanEntityTypes.WY_ELITE.get(),
            List.of(
                gene(Genes.INTELLIGENCE, GeneTier.PRIME),
                gene(Genes.ATTACK_DAMAGE, GeneTier.WEAK)
            )
        );
        add(
            HumanEntityTypes.WY_SPEC_OPS_COMMANDO.get(),
            List.of(
                gene(Genes.INTELLIGENCE, GeneTier.PRIME),
                gene(Genes.STEALTH, GeneTier.AVERAGE)
            )
        );
        add(
            HumanEntityTypes.WY_SPEC_OPS_ELITE.get(),
            List.of(
                gene(Genes.INTELLIGENCE, GeneTier.PRIME),
                gene(Genes.STEALTH, GeneTier.AVERAGE)
            )
        );

        // [stated] No genes at all: undead, constructs, spirits, elementals and bosses.
        touchedEntries.add(EntityType.ZOMBIE);
        touchedEntries.add(EntityType.HUSK);
        touchedEntries.add(EntityType.DROWNED);
        touchedEntries.add(EntityType.ZOMBIE_VILLAGER);
        touchedEntries.add(EntityType.SKELETON);
        touchedEntries.add(EntityType.STRAY);
        touchedEntries.add(EntityType.BOGGED);
        touchedEntries.add(EntityType.WITHER_SKELETON);
        touchedEntries.add(EntityType.PHANTOM);
        touchedEntries.add(EntityType.ZOMBIFIED_PIGLIN);
        touchedEntries.add(EntityType.ZOGLIN);
        touchedEntries.add(EntityType.SKELETON_HORSE);
        touchedEntries.add(EntityType.ZOMBIE_HORSE);
        touchedEntries.add(EntityType.IRON_GOLEM);
        touchedEntries.add(EntityType.SNOW_GOLEM);
        touchedEntries.add(EntityType.ALLAY);
        touchedEntries.add(EntityType.VEX);
        touchedEntries.add(EntityType.BLAZE);
        touchedEntries.add(EntityType.BREEZE);
        touchedEntries.add(EntityType.ENDER_DRAGON);
        touchedEntries.add(EntityType.WITHER);
        touchedEntries.add(EntityType.GIANT);
        touchedEntries.add(EntityType.TADPOLE);

        BuiltInRegistries.ENTITY_TYPE.stream()
            .filter(Predicate.not(touchedEntries::contains))
            .filter(entityType -> entityType.getCategory() != MobCategory.MISC)
            .forEach(entityType -> Human.LOGGER.warn("No gene bonuses provided for {}", BuiltInRegistries.ENTITY_TYPE.getKey(entityType)));
    }

    private static Tuple2<Gene, GeneTier> gene(Gene gene, GeneTier tier) {
        return new Tuple2<>(gene, tier);
    }

    private void add(EntityType<?> entityType, List<Tuple2<Gene, GeneTier>> genes) {
        touchedEntries.add(entityType);
        add(
            BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath(),
            new GeneBonusData(
                new EntityTypePredicate.Single(entityType),
                genes.stream()
                    .map(
                        tuple -> new GeneBonusDataEntry(
                            tuple.v1().id(),
                            Genes.operation(tuple.v1()),
                            tuple.v2().speciesValue(tuple.v1())
                        )
                    )
                    .toList()
            )
        );
    }
}
