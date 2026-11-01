package com.human.common.gameplay.gene;

import com.human.common.gameplay.entity.manager.GeneContainer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Advanced genes: a bonus on top of base genes that keep working (team-reviewed list, Oct 25/26).
 * <ul>
 * <li>A <b>MUTATION</b> is active while EVERY base gene in its recipe is at or above {@link #THRESHOLD} of that gene's
 * cap, and switches off as soon as one drops below.</li>
 * <li>An <b>ENGINEERED</b> gene is locked in by the genetics machines whatever the base genes do, and replaces any
 * mutation in its category.</li>
 * <li>A mob holds at most <b>one advanced gene per category</b>. When two mutations qualify, the one whose base genes
 * are stronger wins and the other reads BLOCKED. A gene that explicitly replaces another (Moonwalker replaces Leaper)
 * always beats it.</li>
 * </ul>
 * The threshold is a single dial here: [stated] Oct 30, 60% of each cap (was the proposed 50%).
 */
public final class AdvancedGenes {

    /** [stated] Oct 30: 60% of each recipe gene's cap. */
    public static final double THRESHOLD = 0.6;

    public enum AdvancedCategory {
        COMBAT,
        MOVEMENT,
        BODY
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        BLOCKED
    }

    public record Definition(
        Gene gene,
        AdvancedCategory category,
        List<Gene> recipe,
        @Nullable Gene replaces
    ) {}

    private static final List<Definition> DEFINITIONS = new ArrayList<>();

    static {
        define(Genes.BERSERKER, AdvancedCategory.COMBAT, null, Genes.ATTACK_SPEED, Genes.ATTACK_DAMAGE);
        define(Genes.HEMOPHAGE, AdvancedCategory.COMBAT, null, Genes.REGENERATION, Genes.ATTACK_DAMAGE);
        define(Genes.AMBUSHER, AdvancedCategory.COMBAT, null, Genes.STEALTH, Genes.NIGHT_VISION);
        define(Genes.REACH, AdvancedCategory.COMBAT, null, Genes.KEEN_SENSES, Genes.ATTACK_SPEED);

        define(Genes.LEAPER, AdvancedCategory.MOVEMENT, null, Genes.JUMP_HEIGHT, Genes.FALL_RESISTANCE);
        define(
            Genes.MOONWALKER,
            AdvancedCategory.MOVEMENT,
            Genes.LEAPER,
            Genes.JUMP_HEIGHT,
            Genes.FALL_RESISTANCE,
            Genes.LIGHTWEIGHT
        );
        define(Genes.AMPHIBIOUS, AdvancedCategory.MOVEMENT, null, Genes.SWIM_SPEED, Genes.AIR_CAPACITY);
        define(Genes.PHASE_STEP, AdvancedCategory.MOVEMENT, null, Genes.WARP, Genes.STEALTH);
        define(Genes.CLIMBING, AdvancedCategory.MOVEMENT, null, Genes.LIGHTWEIGHT, Genes.JUMP_HEIGHT);

        define(Genes.PHOTOSYNTHESIS, AdvancedCategory.BODY, null, Genes.REGENERATION, Genes.METABOLISM);
        define(Genes.ECHOLOCATION, AdvancedCategory.BODY, null, Genes.NIGHT_VISION, Genes.KEEN_SENSES);
        define(Genes.VOLATILE_ACID, AdvancedCategory.BODY, null, Genes.ACID_VOLATILITY, Genes.CORROSIVE_DAMAGE);
    }

    private AdvancedGenes() {}

    private static void define(Gene gene, AdvancedCategory category, @Nullable Gene replaces, Gene... recipe) {
        DEFINITIONS.add(new Definition(gene, category, List.of(recipe), replaces));
    }

    public static List<Definition> definitions() {
        return Collections.unmodifiableList(DEFINITIONS);
    }

    public static @Nullable Definition definition(Gene gene) {
        for (var definition : DEFINITIONS) {
            if (definition.gene() == gene) {
                return definition;
            }
        }
        return null;
    }

    /** Result of {@link #evaluate}: every advanced gene's status, and which kind each active one is. */
    public record State(
        Map<Gene, Status> statuses,
        Set<Gene> engineered
    ) {

        public static final State NONE = new State(Map.of(), Set.of());

        public boolean isActive(Gene gene) {
            return statuses.get(gene) == Status.ACTIVE;
        }

        public Status status(Gene gene) {
            return statuses.getOrDefault(gene, Status.INACTIVE);
        }

        public boolean isEngineered(Gene gene) {
            return engineered.contains(gene);
        }

        public boolean anyActive() {
            return statuses.containsValue(Status.ACTIVE);
        }
    }

    /**
     * Works out every advanced gene's status from the container's ACTIVE base genes and its engineered set.
     */
    public static State evaluate(GeneContainer container) {
        var map = container.getActiveGeneMap();
        var engineered = container.getEngineeredGenes();
        var statuses = new LinkedHashMap<Gene, Status>();
        var bestPerCategory = new EnumMap<AdvancedCategory, Definition>(AdvancedCategory.class);
        var bestScore = new EnumMap<AdvancedCategory, Double>(AdvancedCategory.class);
        var qualified = new ArrayList<Definition>();

        // Engineered genes own their category outright.
        for (var definition : DEFINITIONS) {
            if (engineered.contains(definition.gene())) {
                bestPerCategory.put(definition.category(), definition);
                bestScore.put(definition.category(), Double.MAX_VALUE);
            }
        }

        for (var definition : DEFINITIONS) {
            statuses.put(definition.gene(), Status.INACTIVE);

            var score = 0.0;
            var qualifies = true;

            for (var base : definition.recipe()) {
                var cap = Genes.cap(base);
                var fraction = cap > 0 ? map.getValue(base) / cap : 0.0;

                if (fraction < THRESHOLD) {
                    qualifies = false;
                    break;
                }

                score += fraction;
            }

            if (!qualifies || engineered.contains(definition.gene())) {
                continue;
            }

            qualified.add(definition);
            score /= definition.recipe().size();

            var current = bestPerCategory.get(definition.category());
            var currentScore = bestScore.getOrDefault(definition.category(), -1.0);
            var beats = current == null
                || (definition.replaces() != null && definition.replaces() == current.gene() && currentScore != Double.MAX_VALUE)
                || (!(current.replaces() != null && current.replaces() == definition.gene()) && score > currentScore);

            if (beats) {
                bestPerCategory.put(definition.category(), definition);
                bestScore.put(definition.category(), score);
            }
        }

        for (var definition : qualified) {
            statuses.put(definition.gene(), Status.BLOCKED);
        }

        for (var winner : bestPerCategory.values()) {
            statuses.put(winner.gene(), Status.ACTIVE);
        }

        return new State(Collections.unmodifiableMap(statuses), Set.copyOf(engineered));
    }
}
