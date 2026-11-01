package com.human.common.gameplay.gene;

import com.alien.common.gameplay.entity.living.alien.ovomorph.Ovomorph;
import com.human.HumanResources;
import com.human.common.model.GeneCarrier;
import com.human.compatibility.avp_alien.AVPAlien;
import com.just.core.functional.function.Function2;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every gene, its CAP and the ONE operation it is dosed in.
 * <p>
 * [stated] Oct 25/26 genetics design:
 * <ul>
 * <li>Every gene has a new, lower cap. A syringe draw extracts 100% of the donor's species value, and a Prime donor
 * maxes a gene in 8 draws (Strong 12, Average 16, Weak 32) - see {@link GeneTier}.</li>
 * <li>Species weaknesses are real: every gene accepts negative values down to minus its cap.</li>
 * <li>Each gene is dosed in exactly one operation ({@link #operation}): ADDITIVE genes are flat amounts (hearts, armour
 * points, blocks), MULTIPLICATIVE genes are fractions (0.25 = 25%).</li>
 * </ul>
 * ⚠ The caps are the tuning dials. Changing one rescales every species value and dose for that gene automatically,
 * because species data is generated from {@link #cap} and the tier (see GeneTier).
 * </p>
 */
public class Genes {

    /** Who a gene does anything for. A player infusion skips MOBS genes (see PlayerGeneInfusion). */
    public enum Who {
        ALL,
        MOBS,
        PLAYERS
    }

    /** Reader grouping. LOOT and HARVEST arrive with the loot/harvest gene pass. */
    public enum Category {
        STAT,
        ADVANCED,
        DEFECT,
        LOOT,
        HARVEST
    }

    public record Spec(
        GeneOperationType operation,
        double cap,
        Who who,
        Category category
    ) {

        public Spec(GeneOperationType operation, double cap, Who who) {
            this(operation, cap, who, Category.STAT);
        }
    }

    private static final Map<Gene, Spec> SPECS = java.util.Collections.synchronizedMap(new LinkedHashMap<>());

    private static final Map<String, Gene> BY_NAME = new HashMap<>();

    // ---------------------------------------------------------------- existing genes (ids unchanged)

    public static final Gene ACID_RESISTANCE = simple("acid_resistance", GeneOperationType.MULTIPLICATIVE, 1.0, Who.ALL);

    // Controls how explosive xenomorph acid is. This results in boilers.
    public static final Gene ACID_VOLATILITY = simple("acid_volatility", GeneOperationType.MULTIPLICATIVE, 1.0, Who.MOBS);

    public static final Gene ACIDIC_BLOOD = simple("acidic_blood", GeneOperationType.MULTIPLICATIVE, 1.0, Who.ALL);

    public static final Gene ARMOR = attribute("armor", Attributes.ARMOR, GeneOperationType.ADDITIVE, 12, Who.ALL);

    public static final Gene ARMOR_TOUGHNESS = attribute(
        "armor_toughness",
        Attributes.ARMOR_TOUGHNESS,
        GeneOperationType.ADDITIVE,
        8,
        Who.ALL
    );

    public static final Gene ATTACK_DAMAGE = attribute("attack_damage", Attributes.ATTACK_DAMAGE, GeneOperationType.ADDITIVE, 8, Who.ALL);

    public static final Gene BONUS_EMBRYO_COUNT = simple("bonus_embryo_count", GeneOperationType.ADDITIVE, 2, Who.MOBS);

    public static final Gene BONUS_PARASITE_COUNT = register(
        "bonus_parasite_count",
        new Spec(GeneOperationType.ADDITIVE, 2, Who.MOBS),
        clampTransformer(2, 0),
        transformer -> new Gene.Effect(HumanResources.location("bonus_parasite_count"), Genes::handleBonusParasiteCount, transformer)
    );

    public static final Gene COLD_RESISTANCE = simple("cold_resistance", GeneOperationType.MULTIPLICATIVE, 1.0, Who.ALL);

    public static final Gene FIRE_RESISTANCE = simple("fire_resistance", GeneOperationType.MULTIPLICATIVE, 1.0, Who.ALL);

    /**
     * Not a dosed gene: the line's health. 0 is perfect; -1 / -2 / -3 are the Unstable / Volatile / Fatal thresholds
     * (GeneDecayLevelType). Kept on its legacy range.
     */
    public static final Gene GENETIC_INTEGRITY = register(
        "genetic_integrity",
        new Spec(GeneOperationType.ADDITIVE, 3, Who.ALL),
        (value, operation) -> switch (operation) {
            case ADDITIVE -> Math.clamp(value, -300, 100);
            case MULTIPLICATIVE -> Math.clamp(value, -3, 3);
        },
        transformer -> new Gene.Simple(HumanResources.location("genetic_integrity"), transformer)
    );

    public static final Gene INTELLIGENCE = simple("intelligence", GeneOperationType.ADDITIVE, 100, Who.ALL);

    public static final Gene KNOCKBACK_RESISTANCE = attribute(
        "knockback_resistance",
        Attributes.KNOCKBACK_RESISTANCE,
        GeneOperationType.ADDITIVE,
        0.8,
        Who.ALL
    );

    public static final Gene MAX_HEALTH = attribute("max_health", Attributes.MAX_HEALTH, GeneOperationType.ADDITIVE, 20, Who.ALL);

    public static final Gene MOVE_SPEED = attribute(
        "move_speed",
        Attributes.MOVEMENT_SPEED,
        GeneOperationType.MULTIPLICATIVE,
        0.4,
        Who.ALL
    );

    public static final Gene POISON = simple("poison", GeneOperationType.MULTIPLICATIVE, 0.6, Who.MOBS);

    public static final Gene POISONOUS_BARBS = simple("poisonous_barbs", GeneOperationType.MULTIPLICATIVE, 0.6, Who.ALL);

    /** [stated] Reflect merged into Thorns. Flat damage returned to melee attackers. */
    public static final Gene THORNS = simple("thorns", GeneOperationType.ADDITIVE, 4, Who.ALL);

    public static final Gene WARP = simple("warp", GeneOperationType.ADDITIVE, 1.0, Who.ALL);

    // ---------------------------------------------------------------- new base genes (Oct 25 final list)
    // Vanilla stat = backed by a vanilla attribute and working now. Hook = registered and readable now; its effect is
    // wired in the next genetics pass.

    public static final Gene SWIM_SPEED = attribute(
        "swim_speed",
        Attributes.WATER_MOVEMENT_EFFICIENCY,
        GeneOperationType.ADDITIVE,
        1.0,
        Who.ALL
    );

    /** ⚠ Never applied to xenomorphs: their step height is pinned to the crusher charge and chrysalis roll. */
    public static final Gene STEP_HEIGHT = attribute("step_height", Attributes.STEP_HEIGHT, GeneOperationType.ADDITIVE, 1.0, Who.ALL);

    public static final Gene JUMP_HEIGHT = attribute("jump_height", Attributes.JUMP_STRENGTH, GeneOperationType.ADDITIVE, 0.3, Who.ALL);

    public static final Gene FALL_RESISTANCE = attribute(
        "fall_resistance",
        Attributes.SAFE_FALL_DISTANCE,
        GeneOperationType.ADDITIVE,
        8,
        Who.ALL
    );

    /** Was Gravity Modifier. A positive % that LOWERS gravity (scale -1). */
    public static final Gene LIGHTWEIGHT = attribute(
        "lightweight",
        Attributes.GRAVITY,
        GeneOperationType.MULTIPLICATIVE,
        0.5,
        Who.ALL,
        -1.0
    );

    public static final Gene AIR_CAPACITY = attribute("air_capacity", Attributes.OXYGEN_BONUS, GeneOperationType.ADDITIVE, 4, Who.ALL);

    public static final Gene SNEAK_SPEED = attribute(
        "sneak_speed",
        Attributes.SNEAKING_SPEED,
        GeneOperationType.ADDITIVE,
        0.5,
        Who.PLAYERS
    );

    public static final Gene KNOCKBACK = attribute("knockback", Attributes.ATTACK_KNOCKBACK, GeneOperationType.ADDITIVE, 2, Who.ALL);

    /** Was Blast Stability. */
    public static final Gene STURDINESS = attribute(
        "sturdiness",
        Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,
        GeneOperationType.ADDITIVE,
        0.8,
        Who.ALL
    );

    public static final Gene KEEN_SENSES = attribute(
        "keen_senses",
        Attributes.FOLLOW_RANGE,
        GeneOperationType.MULTIPLICATIVE,
        0.5,
        Who.MOBS
    );

    public static final Gene MINING_SPEED = attribute(
        "mining_speed",
        Attributes.BLOCK_BREAK_SPEED,
        GeneOperationType.MULTIPLICATIVE,
        0.5,
        Who.PLAYERS
    );

    public static final Gene LUCK = attribute("luck", Attributes.LUCK, GeneOperationType.ADDITIVE, 2, Who.PLAYERS);

    public static final Gene ATTACK_SPEED = attribute(
        "attack_speed",
        Attributes.ATTACK_SPEED,
        GeneOperationType.MULTIPLICATIVE,
        0.4,
        Who.PLAYERS
    );

    /** Share of the target's armour and toughness ignored. Cap 60%; does not pierce resistance genes. (hook) */
    public static final Gene ARMOR_PENETRATION = simple("armor_penetration", GeneOperationType.MULTIPLICATIVE, 0.6, Who.ALL);

    /** Mobs notice the carrier later; 100% = silent footsteps (Silent Steps merged in). (hook) */
    public static final Gene STEALTH = simple("stealth", GeneOperationType.MULTIPLICATIVE, 1.0, Who.ALL);

    /** Extra loot from kills. (hook) */
    public static final Gene FORAGER = simple("forager", GeneOperationType.MULTIPLICATIVE, 0.5, Who.PLAYERS);

    /** Health regained per second. (hook) */
    public static final Gene REGENERATION = simple("regeneration", GeneOperationType.ADDITIVE, 0.5, Who.ALL);

    /** Hunger drains slower. (hook) */
    public static final Gene METABOLISM = simple("metabolism", GeneOperationType.MULTIPLICATIVE, 0.5, Who.PLAYERS);

    /** 100% = immune. (hook) */
    public static final Gene RADIATION_RESISTANCE = simple("radiation_resistance", GeneOperationType.MULTIPLICATIVE, 1.0, Who.ALL);

    /** Poison and wither; 100% = immune. (hook) */
    public static final Gene TOXIN_RESISTANCE = simple("toxin_resistance", GeneOperationType.MULTIPLICATIVE, 1.0, Who.ALL);

    /** Was Acid Potency. Acid hits harder. (hook) */
    public static final Gene CORROSIVE_DAMAGE = simple("corrosive_damage", GeneOperationType.MULTIPLICATIVE, 1.0, Who.MOBS);

    /** Reads cooler on predator thermal vision. (hook, cross-mod) */
    public static final Gene COLD_BLOODED = simple("cold_blooded", GeneOperationType.MULTIPLICATIVE, 1.0, Who.ALL);

    /** Faster molts and growing up. (hook) */
    public static final Gene ACCELERATED_GROWTH = simple("accelerated_growth", GeneOperationType.MULTIPLICATIVE, 1.0, Who.MOBS);

    /** Shorter embryo time in hosts AND shorter breeding cooldown. (hook) */
    public static final Gene RAPID_GESTATION = simple("rapid_gestation", GeneOperationType.MULTIPLICATIVE, 0.5, Who.MOBS);

    /** Queens lay faster. (hook) */
    public static final Gene FERTILITY = simple("fertility", GeneOperationType.MULTIPLICATIVE, 1.0, Who.MOBS);

    /** See in the dark. (hook, players) */
    public static final Gene NIGHT_VISION = simple("night_vision", GeneOperationType.MULTIPLICATIVE, 1.0, Who.PLAYERS);

    // ---------------------------------------------------------------- advanced genes (see AdvancedGenes)
    // Never dosed and never in species data: a mob has one while its recipe's base genes are all above threshold
    // (a MUTATION), or because a machine locked it in (ENGINEERED). One per category.

    public static final Gene BERSERKER = advanced("berserker", Who.ALL);

    public static final Gene HEMOPHAGE = advanced("hemophage", Who.ALL);

    public static final Gene AMBUSHER = advanced("ambusher", Who.ALL);

    public static final Gene REACH = advanced("reach", Who.PLAYERS);

    public static final Gene LEAPER = advanced("leaper", Who.ALL);

    public static final Gene MOONWALKER = advanced("moonwalker", Who.ALL);

    public static final Gene AMPHIBIOUS = advanced("amphibious", Who.ALL);

    public static final Gene PHASE_STEP = advanced("phase_step", Who.ALL);

    public static final Gene CLIMBING = advanced("climbing", Who.PLAYERS);

    public static final Gene PHOTOSYNTHESIS = advanced("photosynthesis", Who.PLAYERS);

    public static final Gene ECHOLOCATION = advanced("echolocation", Who.PLAYERS);

    public static final Gene VOLATILE_ACID = advanced("volatile_acid", Who.ALL);

    // ---------------------------------------------------------------- defect genes (see GeneDefects)
    // Stored like any gene (1.0 = carried) so they pass down; a carrier is anything at 0.5 or more.

    public static final Gene PHOTOPHOBIA = defect("photophobia");

    public static final Gene BRITTLE_ARMOR = defect("brittle_armor");

    public static final Gene HAEMOPHILIA = defect("haemophilia");

    public static final Gene STERILE = defect("sterile");

    public static final Gene SHORT_LIVED = defect("short_lived");

    // ---------------------------------------------------------------- harvest genes (see HarvestGenes)
    // ACTION genes decide how a carrier is harvested and how often (concentration shortens the cooldown); SUBSTANCE
    // genes decide what comes out. Dosed like stats; donors are all Prime.

    public static final Gene MILKABLE = harvest("milkable");

    public static final Gene BOWL_FEEDING = harvest("bowl_feeding");

    public static final Gene SHEARABLE = harvest("shearable");

    public static final Gene EGG_LAYING = harvest("egg_laying");

    public static final Gene MILK = harvest("milk");

    public static final Gene STEW = harvest("stew");

    public static final Gene SLIME = harvest("slime");

    public static final Gene HONEY = harvest("honey");

    public static final Gene INK = harvest("ink");

    /**
     * Called from Human.initialize, inside the mod's registration window. It also registers the harvest-gene items,
     * recipe, synced flag and packets (HumanHarvest) so this build needs no change to Human.java.
     */
    public static void initialize() {
        com.human.common.registry.init.HumanHarvest.initialize();
        com.human.common.registry.init.HumanGeneSync.initialize();
        com.human.common.registry.init.HumanGeneReader.initialize();
        com.human.common.registry.init.HumanGeneTubes.initialize();
        com.human.common.registry.init.HumanGeneticist.initialize();
        com.human.common.registry.init.HumanGeneStorage.initialize();
        com.human.common.registry.init.HumanGeneAnalyzer.initialize();
        com.human.common.registry.init.HumanGeneSeparator.initialize();
        com.human.common.registry.init.HumanGeneRecombinator.initialize();
        com.human.common.registry.init.HumanGeneSplicer.initialize();
    }

    public static Category category(Gene gene) {
        var spec = SPECS.get(gene);
        return spec == null ? Category.STAT : spec.category();
    }

    // ---------------------------------------------------------------- lookups

    public static Map<Gene, Spec> specs() {
        return Collections.unmodifiableMap(SPECS);
    }

    public static Spec spec(Gene gene) {
        return SPECS.get(gene);
    }

    /** The one operation this gene is dosed in. */
    public static GeneOperationType operation(Gene gene) {
        var spec = SPECS.get(gene);
        return spec == null ? GeneOperationType.ADDITIVE : spec.operation();
    }

    /** Maximum (and, negated, minimum) value of this gene. */
    public static double cap(Gene gene) {
        var spec = SPECS.get(gene);
        return spec == null ? Double.MAX_VALUE : spec.cap();
    }

    /** Records the spec of a gene made at runtime (loot genes). */
    static void putSpec(Gene gene, Spec spec) {
        SPECS.put(gene, spec);
    }

    public static Gene byName(String name) {
        return BY_NAME.get(name);
    }

    // ---------------------------------------------------------------- registration

    private static Gene attribute(String name, Holder<Attribute> attribute, GeneOperationType operation, double cap, Who who) {
        return attribute(name, attribute, operation, cap, who, 1.0);
    }

    private static Gene attribute(
        String name,
        Holder<Attribute> attribute,
        GeneOperationType operation,
        double cap,
        Who who,
        double attributeScale
    ) {
        return register(
            name,
            new Spec(operation, cap, who),
            clampTransformer(cap, -cap),
            transformer -> new Gene.Attribute(HumanResources.location(name), attribute, transformer, attributeScale)
        );
    }

    private static Gene simple(String name, GeneOperationType operation, double cap, Who who) {
        return register(
            name,
            new Spec(operation, cap, who),
            clampTransformer(cap, -cap),
            transformer -> new Gene.Simple(HumanResources.location(name), transformer)
        );
    }

    private static Gene advanced(String name, Who who) {
        return register(
            name,
            new Spec(GeneOperationType.ADDITIVE, 1.0, who, Category.ADVANCED),
            clampTransformer(1.0, 0.0),
            transformer -> new Gene.Simple(HumanResources.location(name), transformer)
        );
    }

    private static Gene harvest(String name) {
        return register(
            name,
            new Spec(GeneOperationType.ADDITIVE, 1.0, Who.ALL, Category.HARVEST),
            clampTransformer(1.0, 0.0),
            transformer -> new Gene.Simple(HumanResources.location(name), transformer)
        );
    }

    private static Gene defect(String name) {
        return register(
            name,
            new Spec(GeneOperationType.ADDITIVE, 1.0, Who.ALL, Category.DEFECT),
            clampTransformer(1.0, 0.0),
            transformer -> new Gene.Simple(HumanResources.location(name), transformer)
        );
    }

    /** Same clamp for both operations, so old saves holding the other operation stay inside the new range. */
    private static Function2<Double, GeneOperationType, Double> clampTransformer(double max, double min) {
        return (value, operation) -> Math.clamp(value, min, max);
    }

    private static Gene register(
        String name,
        Spec spec,
        Function2<Double, GeneOperationType, Double> transformer,
        java.util.function.Function<Function2<Double, GeneOperationType, Double>, Gene> factory
    ) {
        var gene = GeneRegistry.register(() -> factory.apply(transformer));
        SPECS.put(gene, spec);
        BY_NAME.put(name, gene);
        return gene;
    }

    private static void handleBonusParasiteCount(LivingEntity entity) {
        if (!AVPAlien.MOD.isLoaded()) {
            return;
        }

        if (!(entity instanceof Ovomorph ovomorph)) {
            return;
        }

        var scaleAttribute = ovomorph.getAttribute(Attributes.SCALE);

        if (scaleAttribute != null) {
            var geneContainer = ((GeneCarrier) ovomorph).getOrCreateGeneManager().getGeneContainer();
            var totalParasiteCount = geneContainer.getActiveGeneMap().getValue(Genes.BONUS_PARASITE_COUNT);
            var modifier = new AttributeModifier(
                Genes.BONUS_PARASITE_COUNT.id(),
                totalParasiteCount / 2.0,
                AttributeModifier.Operation.ADD_VALUE
            );

            ovomorph.maxSpawnCount.set((byte) (1 + totalParasiteCount));
            scaleAttribute.addOrReplacePermanentModifier(modifier);
        }
    }
}
