package com.human.common.gameplay.entity.manager;

import com.blib.api.common.nbt.v1.model.NBTSerializable;
import com.human.common.gameplay.gene.Gene;
import com.human.common.gameplay.gene.GeneModifierKey;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.gene.Genes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class GeneManager implements NBTSerializable {

    private static final String NBT_SAMPLE_COOLDOWN_UNTIL = "geneSampleCooldownUntil";

    /**
     * [stated] Oct 25 - after a syringe draw the donor rests for half the vanilla breeding cooldown (2.5 minutes).
     */
    public static final int SAMPLE_COOLDOWN_TICKS = 3000;

    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> XENOMORPH_TAG = net.minecraft.tags.TagKey
        .create(
            net.minecraft.core.registries.Registries.ENTITY_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_alien", "aliens")
        );

    private final LivingEntity entity;

    /** Game time until which this mob cannot be drawn from again. */
    private long sampleCooldownUntil;

    private static final String NBT_HARVEST_READY = "geneHarvestReady";

    /** Game time each harvest action is ready again, by HarvestGenes.Tool ordinal. */
    private final long[] harvestReadyAt = new long[com.human.common.gameplay.harvest.HarvestGenes.Tool.values().length];

    /** Advanced gene statuses, re-evaluated whenever the active genes change. */
    private com.human.common.gameplay.gene.AdvancedGenes.State advancedState = com.human.common.gameplay.gene.AdvancedGenes.State.NONE;

    private static final net.minecraft.resources.ResourceLocation MOD_REACH_ENTITY = com.human.HumanResources.location(
        "advanced_reach_entity"
    );

    private static final net.minecraft.resources.ResourceLocation MOD_REACH_BLOCK = com.human.HumanResources.location(
        "advanced_reach_block"
    );

    private static final net.minecraft.resources.ResourceLocation MOD_LEAP_JUMP = com.human.HumanResources.location("advanced_leap_jump");

    private static final net.minecraft.resources.ResourceLocation MOD_LEAP_FALL = com.human.HumanResources.location("advanced_leap_fall");

    private static final net.minecraft.resources.ResourceLocation MOD_MOONWALK_GRAVITY = com.human.HumanResources.location(
        "advanced_moonwalk_gravity"
    );

    private static final net.minecraft.resources.ResourceLocation MOD_AMPHIBIOUS_MINING = com.human.HumanResources.location(
        "advanced_amphibious_mining"
    );

    private static final net.minecraft.resources.ResourceLocation MOD_BRITTLE_ARMOR = com.human.HumanResources.location(
        "defect_brittle_armor"
    );

    private final GeneContainer geneContainer;

    /**
     * [stated] Oct 30: a PLAYER's injection is temporary - one active slot, 3 days, no stacking (see
     * {@link com.human.common.gameplay.gene.infusion.PlayerGeneInfusion}). These are the gene modifiers and engineered
     * mutations that slot put on, so exactly those come off again. Empty for every mob.
     */
    private final java.util.List<com.human.common.gameplay.gene.GeneModifierKey> infusionKeys = new java.util.ArrayList<>();

    private final java.util.List<net.minecraft.resources.ResourceLocation> infusionEngineered = new java.util.ArrayList<>();

    /**
     * Game time the infusion runs out - kept here as well as on the effect, so it can follow the player through death.
     */
    private long infusionUntil;

    private static final String NBT_INFUSION_UNTIL = "geneInfusionUntil";

    private static final String NBT_INFUSION_KEYS = "geneInfusionKeys";

    private static final String NBT_INFUSION_ENGINEERED = "geneInfusionEngineered";

    public GeneManager(LivingEntity entity) {
        this.entity = entity;
        this.geneContainer = new GeneContainer();
    }

    public void tick() {
        var activeGeneMap = geneContainer.getActiveGeneMap();

        if (entity.level().isClientSide) {
            return;
        }

        // A player's temporary infusion ends with its effect (time up, milk, or a respawn's fresh body).
        if (!infusionKeys.isEmpty() || !infusionEngineered.isEmpty()) {
            if ((entity.tickCount + entity.getId()) % 20 == 0) {
                com.human.common.gameplay.gene.infusion.PlayerGeneInfusion.checkExpiry(entity, this);
            }
        }

        // Once a second (spread across entities by id), run the genes that act over time.
        if (
            (entity.tickCount + entity.getId()) % 20 == 0
                && (!activeGeneMap.isEmpty() || advancedState.anyActive())
        ) {
            tickTimedGenes();
        }

        if (!activeGeneMap.isDirty()) {
            return;
        }

        // Active genes changed: re-evaluate which advanced genes are on, and apply the stat-type ones.
        advancedState = com.human.common.gameplay.gene.AdvancedGenes.evaluate(geneContainer);
        applyAdvancedAndDefectStats();
        // ...and tell clients which harvest tools this mob now answers to, and the genes drawn on their screens.
        com.human.common.gameplay.harvest.HarvestGenes.refreshSyncedMask(entity, geneContainer);
        com.human.common.registry.init.HumanGeneSync.refresh(entity, geneContainer, advancedState);

        // Do NOT move these to after the gene effects being applied!
        var oldMaxHealth = entity.getMaxHealth();
        var wasFullHealth = entity.getHealth() == oldMaxHealth;

        activeGeneMap.getDirtyKeys()
            .forEach(this::applyGeneEffects);

        var maxHealthAttributeInstance = entity.getAttribute(Attributes.MAX_HEALTH);

        if (maxHealthAttributeInstance != null) {
            var newMax = maxHealthAttributeInstance.getValue();
            var currentHealth = entity.getHealth();

            if (wasFullHealth || currentHealth > newMax) {
                entity.setHealth((float) newMax);
            }
        }

        activeGeneMap.clearDirtyKeys();
    }

    /**
     * Genes that act over time, run once a second:
     * <ul>
     * <li><b>Regeneration</b> - heals its value in health per second.</li>
     * <li><b>Accelerated Growth</b> - a baby grows up faster (+100% at the cap; a weakness slows it).</li>
     * <li><b>Rapid Gestation</b> - the breeding cooldown after mating runs out faster (half the time at the cap).</li>
     * </ul>
     * Xenomorph molts, host embryo time and queen laying are driven from avp_alien.
     */
    private void tickTimedGenes() {
        if (!entity.isAlive()) {
            return;
        }

        var activeGeneMap = geneContainer.getActiveGeneMap();

        // Photosynthesis (advanced, players): open sunlight restores hunger and saturation - never health.
        if (
            advancedState.isActive(Genes.PHOTOSYNTHESIS)
                && entity instanceof net.minecraft.world.entity.player.Player player
                && (entity.tickCount / 20) % 5 == 0
                && inOpenSunlight()
        ) {
            player.getFoodData().eat(1, 0.25F);
        }

        // Photophobia (defect): weakened and slowed in bright light.
        if (
            com.human.common.gameplay.gene.GeneDefects.carries(geneContainer, Genes.PHOTOPHOBIA)
                && entity.level().getMaxLocalRawBrightness(entity.blockPosition()) >= 12
        ) {
            entity.addEffect(
                new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 40, 0, true, false, true)
            );
            entity.addEffect(
                new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    40,
                    0,
                    true,
                    false,
                    true
                )
            );
        }

        // Night Vision (players): at half the cap or more, the vanilla effect - kept topped up, hidden (no particles,
        // no
        // icon). Drinking milk clears it for a second at most. It runs out on its own when the gene is gone.
        if (
            entity instanceof net.minecraft.world.entity.player.Player
                && activeGeneMap.getValue(Genes.NIGHT_VISION) >= NIGHT_VISION_THRESHOLD * Genes.cap(Genes.NIGHT_VISION)
        ) {
            var current = entity.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            if (current == null || (!current.isInfiniteDuration() && current.getDuration() < 220)) {
                entity.addEffect(
                    new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.NIGHT_VISION,
                        400,
                        0,
                        true,
                        false,
                        false
                    )
                );
            }
        }

        // Egg-laying (harvest): lays an egg each time its timer runs out.
        com.human.common.gameplay.harvest.HarvestGenes.tickEggLaying(entity);

        var regeneration = activeGeneMap.getValue(Genes.REGENERATION);
        if (regeneration > 0 && entity.getHealth() < entity.getMaxHealth()) {
            entity.heal((float) regeneration);
        }

        if (entity instanceof net.minecraft.world.entity.AgeableMob ageable) {
            var age = ageable.getAge();

            if (age < 0) {
                // A baby: vanilla ages it 20 ticks a second; add the gene's share on top.
                var growth = activeGeneMap.getValue(Genes.ACCELERATED_GROWTH);
                var extra = (int) Math.round(20.0 * growth);
                if (extra != 0) {
                    ageable.setAge(Math.min(0, age + extra));
                }
            } else if (age > 0) {
                // Breeding cooldown: time * (1 - gene), so the clock runs 1 / (1 - gene) times as fast.
                var gestation = Math.min(activeGeneMap.getValue(Genes.RAPID_GESTATION), 0.9);
                if (gestation > 0) {
                    var extra = (int) Math.round(20.0 * (1.0 / (1.0 - gestation) - 1.0));
                    if (extra > 0) {
                        ageable.setAge(Math.max(0, age - extra));
                    }
                }
            }
        }
    }

    private boolean inOpenSunlight() {
        var level = entity.level();
        var pos = entity.blockPosition().above();
        return level.isDay() && !level.isRainingAt(pos) && level.canSeeSky(pos);
    }

    /**
     * Advanced genes and defects that are plain stat changes, kept as attribute modifiers that come and go with them:
     * <ul>
     * <li><b>Reach</b> - +1.5 block and entity interaction range (players).</li>
     * <li><b>Leaper</b> - higher jump and no fall damage.</li>
     * <li><b>Moonwalker</b> - Leaper's jump and fall immunity plus half gravity (slow fall, floaty jumps).</li>
     * <li><b>Amphibious</b> - no underwater mining penalty (breathing is in MixinLivingEntity_GeneEffects).</li>
     * <li><b>Brittle Armor</b> (defect) - a quarter less armour.</li>
     * </ul>
     */
    private void applyAdvancedAndDefectStats() {
        var reach = advancedState.isActive(Genes.REACH);
        var leaper = advancedState.isActive(Genes.LEAPER);
        var moonwalker = advancedState.isActive(Genes.MOONWALKER);
        var amphibious = advancedState.isActive(Genes.AMPHIBIOUS);
        var brittle = com.human.common.gameplay.gene.GeneDefects.carries(geneContainer, Genes.BRITTLE_ARMOR);

        setModifier(Attributes.ENTITY_INTERACTION_RANGE, MOD_REACH_ENTITY, reach, 1.5, AttributeModifier.Operation.ADD_VALUE);
        setModifier(Attributes.BLOCK_INTERACTION_RANGE, MOD_REACH_BLOCK, reach, 1.5, AttributeModifier.Operation.ADD_VALUE);
        setModifier(Attributes.JUMP_STRENGTH, MOD_LEAP_JUMP, leaper || moonwalker, 0.2, AttributeModifier.Operation.ADD_VALUE);
        setModifier(
            Attributes.FALL_DAMAGE_MULTIPLIER,
            MOD_LEAP_FALL,
            leaper || moonwalker,
            -1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        setModifier(Attributes.GRAVITY, MOD_MOONWALK_GRAVITY, moonwalker, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(Attributes.SUBMERGED_MINING_SPEED, MOD_AMPHIBIOUS_MINING, amphibious, 0.8, AttributeModifier.Operation.ADD_VALUE);
        setModifier(Attributes.ARMOR, MOD_BRITTLE_ARMOR, brittle, -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private void setModifier(
        net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
        net.minecraft.resources.ResourceLocation id,
        boolean present,
        double amount,
        AttributeModifier.Operation operation
    ) {
        var instance = entity.getAttribute(attribute);

        if (instance == null) {
            return;
        }

        if (present) {
            instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, operation));
        } else if (instance.hasModifier(id)) {
            instance.removeModifier(id);
        }
    }

    /** Advanced gene statuses for this mob (ACTIVE / INACTIVE / BLOCKED). */
    public com.human.common.gameplay.gene.AdvancedGenes.State getAdvancedState() {
        return advancedState;
    }

    public boolean hasAdvanced(com.human.common.gameplay.gene.Gene gene) {
        return advancedState.isActive(gene);
    }

    private void applyGeneEffects(GeneModifierKey dirtyGeneModifierKey) {
        var activeGeneMap = geneContainer.getActiveGeneMap();
        var id = dirtyGeneModifierKey.resourceLocation();
        var gene = GeneRegistry.getValueOrNull(id);

        if (gene == null) {
            return;
        }

        switch (gene) {
            case Gene.Attribute attribute -> {
                var attributeInstance = entity.getAttribute(attribute.attributeHolder());

                // ⚠ Xenomorph step height is pinned to the crusher charge and the chrysalis roll (their terrain-vs-wall
                // lines are drawn from it), so the Step Height gene never touches a xeno. It still shows on the reader.
                if (attributeInstance != null && gene == Genes.STEP_HEIGHT && entity.getType().is(XENOMORPH_TAG)) {
                    attributeInstance.removeModifier(id);
                    return;
                }

                if (attributeInstance != null) {
                    var hasGeneModifier = activeGeneMap.hasGeneModifier(dirtyGeneModifierKey);

                    if (hasGeneModifier) {
                        applyGeneAttributeBonus(dirtyGeneModifierKey, attributeInstance);
                    } else {
                        // Container no longer has the gene modifier, so remove the attribute modifier.
                        attributeInstance.removeModifier(id);
                    }
                }
            }
            case Gene.Effect effectGene -> effectGene.onChange().accept(entity);
            case Gene.Simple ignored -> { /* NO-OP */ }
            case Gene.Loot ignored -> { /* NO-OP: acts at death, see LootGenes.dropExtra */ }
        }
    }

    private void applyGeneAttributeBonus(GeneModifierKey geneModifierKey, AttributeInstance attributeInstance) {
        var id = geneModifierKey.resourceLocation();
        var gene = GeneRegistry.getValueOrNull(id);
        var bonusValue = geneContainer.getActiveGeneMap().getValue(gene, geneModifierKey.operation());
        var scale = gene instanceof Gene.Attribute attribute ? attribute.attributeScale() : 1.0;
        var finalBonusValue = scale * switch (geneModifierKey.operation()) {
            case ADDITIVE -> bonusValue;
            case MULTIPLICATIVE -> attributeInstance.getBaseValue() * bonusValue;
        };
        var modifier = new AttributeModifier(id, finalBonusValue, AttributeModifier.Operation.ADD_VALUE);

        attributeInstance.addOrReplacePermanentModifier(modifier);
    }

    public GeneContainer getGeneContainer() {
        return geneContainer;
    }

    /** Night Vision works from half its cap: a spider's weak trace isn't enough to see in the dark. */
    public static final double NIGHT_VISION_THRESHOLD = 0.5;

    /** Ticks left before this mob can be drawn from again (0 = ready). */
    public long getSampleCooldownRemaining() {
        return Math.max(0, sampleCooldownUntil - entity.level().getGameTime());
    }

    public void startSampleCooldown() {
        sampleCooldownUntil = entity.level().getGameTime() + SAMPLE_COOLDOWN_TICKS;
    }

    /** Ticks before this harvest action works again (0 = ready). */
    public long getHarvestCooldownRemaining(com.human.common.gameplay.harvest.HarvestGenes.Tool tool) {
        return Math.max(0, harvestReadyAt[tool.ordinal()] - entity.level().getGameTime());
    }

    public void startHarvestCooldown(com.human.common.gameplay.harvest.HarvestGenes.Tool tool, int ticks) {
        harvestReadyAt[tool.ordinal()] = entity.level().getGameTime() + ticks;
    }

    public java.util.List<com.human.common.gameplay.gene.GeneModifierKey> infusionKeys() {
        return infusionKeys;
    }

    public java.util.List<net.minecraft.resources.ResourceLocation> infusionEngineered() {
        return infusionEngineered;
    }

    public long infusionUntil() {
        return infusionUntil;
    }

    public void setInfusionUntil(long infusionUntil) {
        this.infusionUntil = infusionUntil;
    }

    @Override
    public void load(CompoundTag compoundTag) {
        geneContainer.load(compoundTag);
        infusionKeys.clear();
        infusionEngineered.clear();
        infusionUntil = compoundTag.getLong(NBT_INFUSION_UNTIL);
        for (var element : compoundTag.getList(NBT_INFUSION_KEYS, net.minecraft.nbt.Tag.TAG_STRING)) {
            var parts = element.getAsString().split("\\|");
            var id = parts.length == 2 ? net.minecraft.resources.ResourceLocation.tryParse(parts[0]) : null;
            if (id != null) {
                try {
                    infusionKeys.add(
                        new com.human.common.gameplay.gene.GeneModifierKey(
                            id,
                            com.human.common.gameplay.gene.GeneOperationType.valueOf(parts[1])
                        )
                    );
                } catch (IllegalArgumentException ignored) {}
            }
        }
        for (var element : compoundTag.getList(NBT_INFUSION_ENGINEERED, net.minecraft.nbt.Tag.TAG_STRING)) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(element.getAsString());
            if (id != null) {
                infusionEngineered.add(id);
            }
        }
        sampleCooldownUntil = compoundTag.getLong(NBT_SAMPLE_COOLDOWN_UNTIL);

        var ready = compoundTag.getLongArray(NBT_HARVEST_READY);
        for (var i = 0; i < harvestReadyAt.length; i++) {
            harvestReadyAt[i] = i < ready.length ? ready[i] : 0L;
        }
    }

    @Override
    public void save(CompoundTag compoundTag) {
        geneContainer.save(compoundTag);
        if (infusionUntil > 0) {
            compoundTag.putLong(NBT_INFUSION_UNTIL, infusionUntil);
        }
        if (!infusionKeys.isEmpty()) {
            var keys = new net.minecraft.nbt.ListTag();
            for (var key : infusionKeys) {
                keys.add(net.minecraft.nbt.StringTag.valueOf(key.resourceLocation() + "|" + key.operation().name()));
            }
            compoundTag.put(NBT_INFUSION_KEYS, keys);
        }
        if (!infusionEngineered.isEmpty()) {
            var engineered = new net.minecraft.nbt.ListTag();
            for (var id : infusionEngineered) {
                engineered.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
            }
            compoundTag.put(NBT_INFUSION_ENGINEERED, engineered);
        }
        if (sampleCooldownUntil > 0) {
            compoundTag.putLong(NBT_SAMPLE_COOLDOWN_UNTIL, sampleCooldownUntil);
        }

        for (var readyAt : harvestReadyAt) {
            if (readyAt > 0) {
                compoundTag.putLongArray(NBT_HARVEST_READY, harvestReadyAt.clone());
                break;
            }
        }
    }
}
