package com.human.common.gameplay.harvest;

import com.blib.api.common.data_sync.v1.model.DataUser;
import com.human.common.gameplay.entity.manager.GeneContainer;
import com.human.common.gameplay.gene.Gene;
import com.human.common.gameplay.gene.Genes;
import com.human.common.model.GeneCarrier;
import com.human.common.network.packet.C2SHarvestPickPayload;
import com.human.common.network.packet.S2COpenHarvestWheelPayload;
import com.human.common.registry.init.HumanHarvest;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Harvest genes (team-reviewed design, Oct 25/26). Any mob can carry them, xenos included ([stated] "yes because its
 * hilarious").
 * <ul>
 * <li><b>Action genes</b> say HOW: Milkable (bucket), Bowl-feeding (bowl), Shearable (shears), Egg-laying (on its
 * own).</li>
 * <li><b>Substance genes</b> say WHAT comes out of a bucket: Milk, Slime, Honey, Ink. Milkable with no substance gives
 * plain milk. Stew is the bowl's substance.</li>
 * <li>Several bucket substances on one mob: using the bucket opens a selection wheel to pick one.</li>
 * <li><b>Results:</b> milk stays vanilla's milk bucket; honey fills avp_human's honey bucket (our honey fluid); a
 * substance with no fluid comes out as a substance bucket (slime places a slime block, ink can't be placed; both give 9
 * items back in a crafting grid); stew needs a bowl.</li>
 * <li><b>Cooldown:</b> each action gene has its own: 2 minutes at the lowest dose, shrinking to 30 seconds at the cap.
 * Egg-laying uses the same timer as its laying interval.</li>
 * <li>Where vanilla already does exactly the same thing (milking a cow or goat for milk, shearing a sheep, a bowl on a
 * mooshroom for stew, a chicken laying) vanilla is left alone.</li>
 * </ul>
 */
public final class HarvestGenes {

    public static final int COOLDOWN_BASE_TICKS = 2 * 60 * 20;

    public static final int COOLDOWN_FLOOR_TICKS = 30 * 20;

    /** How far the player may be from the mob when the wheel's pick arrives. */
    private static final double PICK_RANGE_SQR = 8.0 * 8.0;

    public enum Tool {

        BUCKET(1, Genes.MILKABLE),
        BOWL(2, Genes.BOWL_FEEDING),
        SHEARS(4, Genes.SHEARABLE),
        EGGS(8, Genes.EGG_LAYING);

        public final int bit;

        public final Gene gene;

        Tool(int bit, Gene gene) {
            this.bit = bit;
            this.gene = gene;
        }

        public static @Nullable Tool of(ItemStack held) {
            if (held.is(Items.BUCKET)) {
                return BUCKET;
            }
            if (held.is(Items.BOWL)) {
                return BOWL;
            }
            if (held.is(Items.SHEARS)) {
                return SHEARS;
            }
            return null;
        }
    }

    /** What a bucket can draw. Order is the wheel's order. */
    public enum Substance {

        MILK(Genes.MILK),
        SLIME(Genes.SLIME),
        HONEY(Genes.HONEY),
        INK(Genes.INK);

        public final Gene gene;

        Substance(Gene gene) {
            this.gene = gene;
        }

        public static @Nullable Substance byOrdinal(int ordinal) {
            var values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
        }

        /** What a bucket comes back as. */
        public ItemStack result() {
            return switch (this) {
                case MILK -> new ItemStack(Items.MILK_BUCKET);
                case HONEY -> new ItemStack(com.human.common.registry.init.item.HumanItems.HONEY_BUCKET.get());
                case SLIME -> SubstanceBucketItem.of(Items.SLIME_BALL);
                case INK -> SubstanceBucketItem.of(Items.INK_SAC);
            };
        }
    }

    private HarvestGenes() {}

    // ---------------------------------------------------------------- what a mob carries

    public static boolean carries(GeneContainer container, Gene gene) {
        return container.getActiveGeneMap().getValue(gene) > 0;
    }

    /** The bucket substances this mob carries, in wheel order. */
    public static List<Substance> substances(GeneContainer container) {
        var list = new ArrayList<Substance>();
        for (var substance : Substance.values()) {
            if (carries(container, substance.gene)) {
                list.add(substance);
            }
        }
        return list;
    }

    /**
     * The tools this mob answers to through its genes, as {@link Tool} bits - leaving out what vanilla already does the
     * same way, so a bred cow with Milkable + Milk is still milked by vanilla.
     */
    public static int toolMask(LivingEntity entity, GeneContainer container) {
        var mask = 0;

        if (carries(container, Genes.MILKABLE)) {
            var substances = substances(container);
            var plainMilk = substances.isEmpty() || (substances.size() == 1 && substances.getFirst() == Substance.MILK);
            if (!(plainMilk && (entity instanceof Cow || entity instanceof Goat))) {
                mask |= Tool.BUCKET.bit;
            }
        }

        if (carries(container, Genes.BOWL_FEEDING) && !(entity instanceof MushroomCow)) {
            mask |= Tool.BOWL.bit;
        }

        if (carries(container, Genes.SHEARABLE) && !(entity instanceof Sheep)) {
            mask |= Tool.SHEARS.bit;
        }

        return mask;
    }

    /** Keeps the synced tool flag in step with the genes. Called by GeneManager whenever the active genes change. */
    public static void refreshSyncedMask(LivingEntity entity, GeneContainer container) {
        if (!(entity instanceof DataUser user)) {
            return;
        }

        var mask = toolMask(entity, container);
        var key = HumanHarvest.ENTITY_HARVEST_TOOLS.get();

        if (!java.util.Objects.equals(user.getDataContainer().get(key), mask)) {
            user.getDataContainer().set(key, mask);
        }
    }

    /** The synced flag, readable on either side. */
    public static int syncedMask(LivingEntity entity) {
        if (!(entity instanceof DataUser user)) {
            return 0;
        }
        var value = user.getDataContainer().get(HumanHarvest.ENTITY_HARVEST_TOOLS.get());
        return value == null ? 0 : value;
    }

    /**
     * Cooldown for an action gene at this concentration: 2 minutes at the lowest dose down to 30 seconds at the cap.
     */
    public static int cooldownTicks(GeneContainer container, Gene actionGene) {
        var fraction = Math.clamp(container.getActiveGeneMap().getValue(actionGene) / Genes.cap(actionGene), 0.0, 1.0);
        return (int) Math.round(COOLDOWN_BASE_TICKS + (COOLDOWN_FLOOR_TICKS - COOLDOWN_BASE_TICKS) * fraction);
    }

    // ---------------------------------------------------------------- using a tool on a mob (server)

    /**
     * Server side of a click with {@code tool}. Returns true when the click was the harvest's (whether or not anything
     * came out), so the caller can claim it.
     */
    public static boolean interact(Player player, InteractionHand hand, LivingEntity target, Tool tool) {
        if (!(target instanceof GeneCarrier carrier)) {
            return false;
        }

        var manager = carrier.getOrCreateGeneManager();
        var container = manager.getGeneContainer();

        if ((toolMask(target, container) & tool.bit) == 0) {
            return false;
        }

        var remaining = manager.getHarvestCooldownRemaining(tool);
        if (remaining > 0) {
            // TODO: Use proper translatable here.
            player.displayClientMessage(Component.literal("Not ready to harvest yet (" + ((remaining + 19) / 20) + "s)."), true);
            return true;
        }

        if (tool == Tool.BUCKET) {
            var substances = substances(container);

            if (substances.size() > 1 && player instanceof ServerPlayer serverPlayer) {
                var bits = 0;
                for (var substance : substances) {
                    bits |= 1 << substance.ordinal();
                }
                com.human.Human.MOD.networking()
                    .sendToClient(serverPlayer, new S2COpenHarvestWheelPayload(target.getId(), bits, hand.ordinal()));
                return true;
            }

            harvestBucket(player, hand, target, substances.isEmpty() ? Substance.MILK : substances.getFirst());
            return true;
        }

        if (tool == Tool.BOWL) {
            give(player, hand, new ItemStack(Items.MUSHROOM_STEW));
            target.playSound(SoundEvents.MOOSHROOM_MILK, 1.0F, 1.0F);
            manager.startHarvestCooldown(tool, cooldownTicks(container, tool.gene));
            return true;
        }

        if (tool == Tool.SHEARS) {
            var wool = 1 + target.getRandom().nextInt(2);
            target.spawnAtLocation(new ItemStack(Items.WHITE_WOOL, wool), 1.0F);
            target.level().playSound(null, target, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getItemInHand(hand).hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            manager.startHarvestCooldown(tool, cooldownTicks(container, tool.gene));
            return true;
        }

        return false;
    }

    private static void harvestBucket(Player player, InteractionHand hand, LivingEntity target, Substance substance) {
        var manager = ((GeneCarrier) target).getOrCreateGeneManager();
        give(player, hand, substance.result());
        target.playSound(substance == Substance.HONEY ? SoundEvents.BEEHIVE_DRIP : SoundEvents.COW_MILK, 1.0F, 1.0F);
        manager.startHarvestCooldown(Tool.BUCKET, cooldownTicks(manager.getGeneContainer(), Tool.BUCKET.gene));
    }

    private static void give(Player player, InteractionHand hand, ItemStack result) {
        var held = player.getItemInHand(hand);
        player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
    }

    /** The wheel's pick. ⚠ Re-checks everything: the player, the bucket, the mob, the substance and the cooldown. */
    public static void handlePick(C2SHarvestPickPayload payload, Player player) {
        var substance = Substance.byOrdinal(payload.substance());
        var hand = payload.hand() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        if (substance == null || !player.getItemInHand(hand).is(Items.BUCKET)) {
            return;
        }

        if (!(player.level().getEntity(payload.entityId()) instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }

        if (player.distanceToSqr(target) > PICK_RANGE_SQR || !(target instanceof GeneCarrier carrier)) {
            return;
        }

        var manager = carrier.getOrCreateGeneManager();
        var container = manager.getGeneContainer();

        if (
            (toolMask(target, container) & Tool.BUCKET.bit) == 0
                || !carries(container, substance.gene)
                || manager.getHarvestCooldownRemaining(Tool.BUCKET) > 0
        ) {
            return;
        }

        harvestBucket(player, hand, target, substance);
    }

    // ---------------------------------------------------------------- egg-laying (once a second, from GeneManager)

    /** Lays an egg when its timer is up. Chickens are left to vanilla. */
    public static void tickEggLaying(LivingEntity entity) {
        if (entity instanceof Chicken || !(entity instanceof GeneCarrier carrier) || entity.isBaby()) {
            return;
        }

        var manager = carrier.getOrCreateGeneManager();
        var container = manager.getGeneContainer();

        if (!carries(container, Genes.EGG_LAYING) || manager.getHarvestCooldownRemaining(Tool.EGGS) > 0) {
            return;
        }

        entity.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (entity.getRandom().nextFloat() - entity.getRandom().nextFloat()) * 0.2F + 1.0F);
        entity.spawnAtLocation(Items.EGG);
        manager.startHarvestCooldown(Tool.EGGS, cooldownTicks(container, Genes.EGG_LAYING));
    }
}
