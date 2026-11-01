package com.human.fabric.data.lang.en_us.provider;

import com.human.common.gameplay.gene.Gene;
import com.human.common.gameplay.gene.Genes;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import java.util.function.Consumer;

/**
 * Gene names and their reader descriptions. [stated] a description says what the gene DOES, never where to get it.
 */
public class EnUsGeneProvider {

    public static final Consumer<FabricLanguageProvider.TranslationBuilder> CONSUMER = builder -> {
        addGene(builder, Genes.ACID_RESISTANCE, "Acid Resistance", "Takes less damage from acid. 100% is immune.");
        addGene(builder, Genes.ACID_VOLATILITY, "Acid Volatility", "Makes acid blood explosive. High enough breeds boilers.");
        addGene(builder, Genes.ACIDIC_BLOOD, "Acidic Blood", "Chance to splash acid on attackers when wounded.");
        addGene(builder, Genes.ARMOR, "Armor", "Adds armour points.");
        addGene(builder, Genes.ARMOR_TOUGHNESS, "Armor Toughness", "Adds armour toughness against strong hits.");
        addGene(builder, Genes.ATTACK_DAMAGE, "Attack Damage", "Adds melee damage.");
        addGene(builder, Genes.BONUS_EMBRYO_COUNT, "Bonus Embryo Count", "Chance of extra babies when bred.");
        addGene(builder, Genes.BONUS_PARASITE_COUNT, "Bonus Parasite Count", "Eggs hold extra parasites.");
        addGene(builder, Genes.COLD_RESISTANCE, "Cold Resistance", "Takes less damage from cold and freezing. 100% is immune.");
        addGene(builder, Genes.FIRE_RESISTANCE, "Fire Resistance", "Takes less damage from fire and lava. 100% is immune.");
        addGene(builder, Genes.GENETIC_INTEGRITY, "Genetic Integrity", "Health of the bloodline. Low integrity breeds defects.");
        addGene(
            builder,
            Genes.INTELLIGENCE,
            "Intelligence",
            "Mental capacity. On a player: up to 50% more XP from orbs and 30% off enchanting and anvil costs. On a mob: drops up to 50% more XP."
        );
        addGene(builder, Genes.KNOCKBACK_RESISTANCE, "Knockback Resistance", "Pushed back less by hits.");
        addGene(builder, Genes.MAX_HEALTH, "Max Health", "Adds health.");
        addGene(builder, Genes.MOVE_SPEED, "Move Speed", "Moves faster.");
        addGene(builder, Genes.POISON, "Poison", "Chance to poison targets on hit.");
        addGene(builder, Genes.POISONOUS_BARBS, "Poisonous Barbs", "Chance to poison melee attackers.");
        addGene(builder, Genes.THORNS, "Thorns", "Returns damage to melee attackers.");
        addGene(builder, Genes.WARP, "Warp", "Teleports away when hurt.");
        addGene(builder, Genes.SWIM_SPEED, "Swim Speed", "Moves faster in water.");
        addGene(builder, Genes.STEP_HEIGHT, "Step Height", "Walks up blocks without jumping. No effect on xenomorphs.");
        addGene(builder, Genes.JUMP_HEIGHT, "Jump Height", "Jumps higher.");
        addGene(builder, Genes.FALL_RESISTANCE, "Fall Resistance", "Falls further before taking damage.");
        addGene(builder, Genes.LIGHTWEIGHT, "Lightweight", "Lower gravity: floatier jumps and slower falls.");
        addGene(builder, Genes.AIR_CAPACITY, "Air Capacity", "Holds breath longer underwater.");
        addGene(builder, Genes.SNEAK_SPEED, "Sneak Speed", "Moves faster while sneaking.");
        addGene(builder, Genes.KNOCKBACK, "Knockback", "Hits push targets harder.");
        addGene(builder, Genes.STURDINESS, "Sturdiness", "Pushed back less by explosions.");
        addGene(builder, Genes.KEEN_SENSES, "Keen Senses", "Notices targets from further away.");
        addGene(builder, Genes.MINING_SPEED, "Mining Speed", "Breaks blocks faster.");
        addGene(builder, Genes.LUCK, "Luck", "Better fishing and luck-weighted loot.");
        addGene(builder, Genes.ATTACK_SPEED, "Attack Speed", "Recovers full-strength swings faster.");
        addGene(builder, Genes.ARMOR_PENETRATION, "Armor Penetration", "Attacks ignore part of the target's armour.");
        addGene(builder, Genes.STEALTH, "Stealth", "Mobs notice the carrier later. 100% is silent footsteps.");
        addGene(builder, Genes.FORAGER, "Forager", "Extra loot from kills.");
        addGene(builder, Genes.REGENERATION, "Regeneration", "Heals over time.");
        addGene(builder, Genes.METABOLISM, "Metabolism", "Hunger drains slower.");
        addGene(builder, Genes.RADIATION_RESISTANCE, "Radiation Resistance", "Takes less radiation damage. 100% is immune.");
        addGene(builder, Genes.TOXIN_RESISTANCE, "Toxin Resistance", "Less harmed by poison and wither. 100% is immune.");
        addGene(builder, Genes.CORROSIVE_DAMAGE, "Corrosive Damage", "Acid hits harder.");
        addGene(builder, Genes.COLD_BLOODED, "Cold-Blooded", "Reads cooler on predator thermal vision.");
        addGene(builder, Genes.ACCELERATED_GROWTH, "Accelerated Growth", "Grows up and molts faster.");
        addGene(builder, Genes.RAPID_GESTATION, "Rapid Gestation", "Shorter embryo time and breeding cooldown.");
        addGene(builder, Genes.FERTILITY, "Fertility", "Queens lay eggs faster.");
        addGene(builder, Genes.NIGHT_VISION, "Night Vision", "Sees in the dark.");
        addGene(builder, Genes.BERSERKER, "Berserker", "Hits harder the more hurt it is: up to +50% damage.");
        addGene(builder, Genes.HEMOPHAGE, "Hemophage", "Heals a fifth of the damage it deals.");
        addGene(builder, Genes.AMBUSHER, "Ambusher", "Mobs do not notice it until it is close.");
        addGene(builder, Genes.REACH, "Reach", "Reaches blocks and mobs 1.5 blocks further.");
        addGene(builder, Genes.LEAPER, "Leaper", "Jumps higher and takes no fall damage.");
        addGene(builder, Genes.MOONWALKER, "Moonwalker", "Half gravity: floaty jumps, slow falls, no fall damage.");
        addGene(builder, Genes.AMPHIBIOUS, "Amphibious", "Breathes underwater and mines there at full speed.");
        addGene(builder, Genes.PHASE_STEP, "Phase Step", "Blinks away from a hit instead of warping at random.");
        addGene(builder, Genes.CLIMBING, "Climbing", "Climbs walls like a spider.");
        addGene(builder, Genes.PHOTOSYNTHESIS, "Photosynthesis", "Open sunlight restores hunger. Does not heal.");
        addGene(builder, Genes.ECHOLOCATION, "Echolocation", "Senses nearby creatures through walls.");
        addGene(builder, Genes.VOLATILE_ACID, "Volatile Acid", "Bursts in an acid splash when it dies.");
        addGene(builder, Genes.PHOTOPHOBIA, "Photophobia", "Weakened and slowed in bright light.");
        addGene(builder, Genes.BRITTLE_ARMOR, "Brittle Armor", "A quarter less armour.");
        addGene(builder, Genes.HAEMOPHILIA, "Haemophilia", "Harmful effects last half again as long.");
        addGene(builder, Genes.STERILE, "Sterile", "Cannot breed. Queens lay at half speed.");
        addGene(builder, Genes.SHORT_LIVED, "Short-Lived", "All healing is halved.");
        addGene(builder, Genes.MILKABLE, "Milkable", "Use a bucket on it to collect its substance.");
        addGene(builder, Genes.BOWL_FEEDING, "Bowl-feeding", "Use a bowl on it for mushroom stew.");
        addGene(builder, Genes.SHEARABLE, "Shearable", "Shear it for wool.");
        addGene(builder, Genes.EGG_LAYING, "Egg-laying", "Lays eggs on its own.");
        addGene(builder, Genes.MILK, "Milk", "A bucket draws milk.");
        addGene(builder, Genes.STEW, "Stew", "A bowl draws mushroom stew.");
        addGene(builder, Genes.SLIME, "Slime", "A bucket draws slime. It places a slime block.");
        addGene(builder, Genes.HONEY, "Honey", "A bucket draws honey.");
        addGene(builder, Genes.INK, "Ink", "A bucket draws ink.");

        // Harvest: the substance bucket and the selection wheel.
        builder.add("item.avp_human.substance_bucket", "Substance Bucket");
        builder.add("item.avp_human.substance_bucket.of", "Bucket of %s");
        builder.add("gui.avp_human.harvest_wheel", "COLLECT");
        builder.add("gui.avp_human.gene_reader", "Gene Reader");
        builder.add("item.avp_human.empty_gene_tube", "Empty Gene Tube");
        builder.add("item.avp_human.gene_tube", "Gene Tube");
        builder.add("item.avp_human.genetic_research", "Genetic Research");
        builder.add("block.avp_human.geneticist_station", "Geneticist Station");
        // [stated] Oct 29: the cleanser and the Geneticist Station's cleanser screen.
        // [stated] Oct 29: the Gene Storage.
        builder.add("block.avp_human.gene_storage", "Gene Storage");
        builder.add("block.avp_human.wy_gene_storage", "W-Y Gene Storage");
        builder.add("block.avp_human.uscm_gene_storage", "USCM Gene Storage");
        builder.add("gui.avp_human.gene_storage.full", "Gene Storage is full");
        builder.add("gui.avp_human.gene_storage.no_power", "Gene Storage has no power");
        // [stated] Oct 29: the Gene Analyzer.
        builder.add("block.avp_human.gene_analyzer", "Gene Analyzer");
        builder.add("block.avp_human.wy_gene_analyzer", "W-Y Gene Analyzer");
        builder.add("block.avp_human.uscm_gene_analyzer", "USCM Gene Analyzer");
        builder.add("gui.avp_human.gene_analyzer.no_power", "Gene Analyzer has no power");
        builder.add("gui.avp_human.gene_analyzer.tray_full", "The tray already holds a sample");
        // [stated] Oct 29: the Gene Separator.
        builder.add("block.avp_human.gene_separator", "Gene Separator");
        builder.add("block.avp_human.wy_gene_separator", "W-Y Gene Separator");
        builder.add("block.avp_human.uscm_gene_separator", "USCM Gene Separator");
        // [stated] Oct 29: the Gene Recombinator.
        builder.add("block.avp_human.gene_recombinator", "Gene Recombinator");
        builder.add("block.avp_human.wy_gene_recombinator", "W-Y Gene Recombinator");
        builder.add("block.avp_human.uscm_gene_recombinator", "USCM Gene Recombinator");
        builder.add("gui.avp_human.gene_recombinator.no_room", "No empty holder for that tube");
        // [stated] Oct 29: the Gene Splicer.
        builder.add("block.avp_human.gene_splicer", "Gene Splicer");
        builder.add("block.avp_human.wy_gene_splicer", "W-Y Gene Splicer");
        builder.add("block.avp_human.uscm_gene_splicer", "USCM Gene Splicer");
        builder.add("gui.avp_human.gene_splicer.no_room", "No empty holder, or that gene is already in");
        // [stated] Oct 30: player self-injection.
        builder.add("effect.avp_human.gene_infusion", "Gene Infusion");
        builder.add("gamerule.humanPlayerGeneInjection", "Player Gene Injection");
        builder.add(
            "gamerule.humanPlayerGeneInjection.description",
            "Players can inject themselves with a syringe: every gene in it for 3 days, one infusion at a time."
        );
        builder.add("item.avp_human.cleanser", "Cleanser");
        builder.add("item.avp_human.cleanser.tooltip.bonus", "+%s%% integrity for one syringe");
        builder.add("item.avp_human.cleanser.tooltip.use", "Use it with a syringe in a Geneticist Station");
        builder.add("item.avp_human.syringe.cleansed", "Cleansed: +%s%% integrity");
        builder.add("container.avp_human.geneticist_station", "Geneticist Station");
        builder.add("gui.avp_human.geneticist_station.cleanse", "CLEANSE %s");
        builder.add("gui.avp_human.geneticist_station.inject", "INJECT %s");
        builder.add("gui.avp_human.geneticist_station.net", "NET %s");
        builder.add("gui.avp_human.geneticist_station.pure_shot", "PURE SHOT");
        builder.add("gui.avp_human.geneticist_station.already", "ALREADY");
        builder.add("gui.avp_human.geneticist_station.cleansed", "CLEANSED");
        builder.add("gui.avp_human.geneticist_station.one_only", "ONE ONLY");
        builder.add("gui.avp_human.geneticist_station.used_once", "USED ONCE");
        builder.add("gui.avp_human.geneticist_station.left", "%s SEC LEFT");
        builder.add("gui.avp_human.geneticist_station.done", "DONE");
        builder.add("gui.avp_human.geneticist_station.add_cleanser", "ADD CLEANSER");
        builder.add("gui.avp_human.geneticist_station.add_syringe", "ADD SYRINGE");
        builder.add("gui.avp_human.geneticist_station.output_full", "TAKE OUTPUT");
        builder.add("gui.avp_human.geneticist_station.empty", "ADD BOTH");
        builder.add("gui.avp_human.geneticist_station.hint.syringe", "A syringe - filled or empty");
        builder.add("gui.avp_human.geneticist_station.hint.cleanser", "A cleanser");
        builder.add("item.avp_human.geneticist_goggles", "Geneticist Goggles");
        builder.add("item.avp_human.geneticist_lab_coat", "Lab Coat");
        builder.add("item.avp_human.geneticist_coat_tails", "Lab Coat Tails");
        builder.add("entity.avp_human.colonist.geneticist", "Geneticist");

        // Loot genes are named after their item; they share one description.
        builder.add("gene.avp.loot.desc", "Drops extra of this item when it dies.");
    };

    private static void addGene(
        FabricLanguageProvider.TranslationBuilder translationBuilder,
        Gene gene,
        String name,
        String description
    ) {
        translationBuilder.add(gene.getTranslationKey(), name);
        translationBuilder.add(gene.getDescriptionKey(), description);
    }
}
