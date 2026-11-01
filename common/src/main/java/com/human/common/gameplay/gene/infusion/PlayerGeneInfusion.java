package com.human.common.gameplay.gene.infusion;

import com.human.common.gameplay.entity.manager.GeneManager;
import com.human.common.gameplay.gene.AdvancedGenes;
import com.human.common.gameplay.gene.GeneBonusDataEntry;
import com.human.common.gameplay.gene.GeneModifierKey;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.gene.Genes;
import com.human.common.model.GeneCarrier;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.HumanGameRules;
import com.human.common.registry.init.HumanMobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * A player injecting themselves ([stated] Oct 30).
 * <ul>
 * <li><b>Temporary.</b> "like a 3 day mc potion": the genes last {@link #DURATION_TICKS} (3 Minecraft days), shown as
 * the Gene Infusion effect with its timer. Permanent options come later.</li>
 * <li><b>One slot, no stacking.</b> "think of it like a active genes slot": a new injection REPLACES the slot - the old
 * genes come off, the new ones go on, and the timer starts again.</li>
 * <li><b>Every gene in the syringe.</b> A spliced syringe gives its five, a draw from a mob gives everything it held
 * (it can hold more). Genes marked for mobs only are skipped - they do nothing on a player.</li>
 * <li><b>Mutations: yes.</b> An engineered mutation in the syringe is on for the slot's time; and the slot's genes
 * count toward natural mutations at the usual 60% of each recipe gene's cap.</li>
 * <li><b>No integrity cost.</b> A temporary dose leaves nothing behind, so integrity (and the defect risk that comes
 * with it) stays a mob matter; the cleanser stays a mob tool.</li>
 * <li><b>Survives death.</b> [stated] Oct 30: "respawn with the genes still active. It can be another reason to be
 * different from potions." The respawned player gets the same genes and the time that was left ({@link #carryOver},
 * from the vanilla respawn copy).</li>
 * <li><b>Ends cleanly.</b> When its time is up, or on a PURE CLEANSE shot ({@link #cleanseSelf}: an empty, cleansed
 * syringe used on yourself), exactly what the slot put on comes off. [stated] Oct 30: milk does NOT end it - the clock
 * is the slot's own end time, so if milk (or anything) strips the effect, the icon is put back with the time that was
 * left. Checked once a second.</li>
 * <li><b>Game rule</b> {@code humanPlayerGeneInjection}, on by default.</li>
 * </ul>
 * <p>
 * A player has no species genes of their own, so the slot is the only thing in their active gene map; it still records
 * precisely which modifiers and mutations it added and removes only those.
 * </p>
 */
public final class PlayerGeneInfusion {

    /** 3 Minecraft days. */
    public static final int DURATION_TICKS = 3 * 24000;

    private PlayerGeneInfusion() {}

    /** Right-click in the air with a filled syringe in INJECT mode. True when the syringe was used. */
    public static boolean injectSelf(ServerPlayer player, ItemStack syringe) {
        if (!player.serverLevel().getGameRules().getBoolean(HumanGameRules.PLAYER_GENE_INJECTION)) {
            player.displayClientMessage(Component.literal("Self-injection is turned off on this world."), true);
            return false;
        }
        var contents = syringe.get(HumanDataComponents.SYRINGE_CONTENTS.get());
        if (contents == null || contents.geneBonusDataEntries().isEmpty()) {
            if (com.human.common.gameplay.gene.geneticist.GeneCleanser.bonusOf(syringe) > 0.0D) {
                return cleanseSelf(player);
            }
            player.displayClientMessage(Component.literal("Syringe is empty, extract a sample first."), true);
            return false;
        }

        var manager = ((GeneCarrier) player).getOrCreateGeneManager();
        clear(manager);

        var container = manager.getGeneContainer();
        var active = container.getActiveGeneMap();
        var applied = 0;
        for (GeneBonusDataEntry entry : contents.geneBonusDataEntries()) {
            var gene = GeneRegistry.getValueOrNull(entry.id());
            if (gene == null || gene == Genes.GENETIC_INTEGRITY) {
                continue;
            }
            var spec = Genes.specs().get(gene);
            if (spec != null && spec.who() == Genes.Who.MOBS) {
                continue; // does nothing on a player
            }
            if (AdvancedGenes.definition(gene) != null) {
                container.lockEngineered(gene);
                manager.infusionEngineered().add(gene.id());
            } else {
                var key = new GeneModifierKey(entry.id(), entry.operation());
                active.add(key, entry.value());
                if (!manager.infusionKeys().contains(key)) {
                    manager.infusionKeys().add(key);
                }
            }
            applied++;
        }

        manager.setInfusionUntil(player.level().getGameTime() + DURATION_TICKS);
        player.addEffect(new MobEffectInstance(HumanMobEffects.getGeneInfusionHolder(), DURATION_TICKS, 0, false, false, true));
        // [stated] Nov 1: injecting teaches nothing - only the Gene Analyzer teaches genes.
        player.level().playSound(null, player.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.6F, 1.6F);
        player.displayClientMessage(
            Component.literal(applied == 1 ? "Infused with 1 gene for 3 days." : "Infused with " + applied + " genes for 3 days."),
            true
        );
        return true;
    }

    /**
     * [stated] Oct 30: "a pure cleanse injection can remove it" - an empty syringe from the Geneticist Station's
     * cleanser, used on yourself, ends the infusion at once. True (syringe used) only when there was an infusion to
     * end; any player can do this, even with self-injection turned off, so nobody is left stuck with one.
     */
    public static boolean cleanseSelf(ServerPlayer player) {
        var manager = ((GeneCarrier) player).getOrCreateGeneManager();
        if (manager.infusionKeys().isEmpty() && manager.infusionEngineered().isEmpty()) {
            player.displayClientMessage(Component.literal("No gene infusion to cleanse."), true);
            return false;
        }
        clear(manager);
        player.removeEffect(HumanMobEffects.getGeneInfusionHolder());
        player.level().playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.5F, 1.8F);
        player.displayClientMessage(Component.literal("Gene infusion cleansed."), true);
        return true;
    }

    /**
     * Once a second while a slot is filled. The slot's own end time decides: past it, the genes come off (and the icon
     * with them); before it, a missing icon - stripped by milk or anything else - is put back with the time left.
     */
    public static void checkExpiry(LivingEntity entity, GeneManager manager) {
        var remaining = manager.infusionUntil() - entity.level().getGameTime();
        var holder = HumanMobEffects.getGeneInfusionHolder();
        if (manager.infusionUntil() <= 0 || remaining <= 0) {
            clear(manager);
            entity.removeEffect(holder);
            return;
        }
        if (!entity.hasEffect(holder)) {
            entity.addEffect(new MobEffectInstance(holder, (int) Math.min(Integer.MAX_VALUE, remaining), 0, false, false, true));
        }
    }

    /**
     * The vanilla respawn copy ({@code ServerPlayer.restoreFrom}): the new body gets the old body's infusion - the same
     * gene values and engineered mutations - with the time that was left. Nothing to do once it has run out.
     */
    public static void carryOver(ServerPlayer from, ServerPlayer to) {
        var old = ((GeneCarrier) from).getOrCreateGeneManager();
        if (old.infusionKeys().isEmpty() && old.infusionEngineered().isEmpty()) {
            return;
        }
        var remaining = old.infusionUntil() - from.level().getGameTime();
        if (remaining <= 0) {
            return;
        }
        var manager = ((GeneCarrier) to).getOrCreateGeneManager();
        clear(manager);
        var oldActive = old.getGeneContainer().getActiveGeneMap();
        var container = manager.getGeneContainer();
        for (var key : old.infusionKeys()) {
            var value = oldActive.getBackingMap().get(key);
            if (value != null) {
                container.getActiveGeneMap().add(key, value);
                manager.infusionKeys().add(key);
            }
        }
        for (var id : old.infusionEngineered()) {
            var gene = GeneRegistry.getValueOrNull(id);
            if (gene != null) {
                container.lockEngineered(gene);
                manager.infusionEngineered().add(id);
            }
        }
        manager.setInfusionUntil(old.infusionUntil());
        to.addEffect(
            new MobEffectInstance(
                HumanMobEffects.getGeneInfusionHolder(),
                (int) Math.min(Integer.MAX_VALUE, remaining),
                0,
                false,
                false,
                true
            )
        );
    }

    /** Takes off exactly what the slot put on. */
    public static void clear(GeneManager manager) {
        var container = manager.getGeneContainer();
        for (var key : manager.infusionKeys()) {
            container.getActiveGeneMap().remove(key);
        }
        for (var id : manager.infusionEngineered()) {
            var gene = GeneRegistry.getValueOrNull(id);
            if (gene != null) {
                container.unlockEngineered(gene);
            }
        }
        manager.infusionKeys().clear();
        manager.infusionEngineered().clear();
        manager.setInfusionUntil(0L);
    }
}
