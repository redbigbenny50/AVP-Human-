package com.human.common.gameplay.machine;

import com.human.common.gameplay.fluid.FluidContainerInteractions;
import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.item.canister.UniversalCanisterItem;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

/**
 * Buckets and universal canisters sitting in a machine SLOT - the fill slot of the press and hydrator, and the
 * freezer's input. {@link FluidContainerInteractions} does the same for a container in a player's hand; this works on
 * the stack alone, since a slot has no player.
 * <p>
 * Every method is pure: it returns what the stack would become and leaves the stack itself untouched, so the caller can
 * check the result fits before committing anything.
 * </p>
 */
public final class MachineFluidItems {

    /** What filling or draining produced: the container as it now is, and how many millibuckets moved. */
    public record Result(
        ItemStack container,
        int moved
    ) {

        static final Result NONE = new Result(ItemStack.EMPTY, 0);

        public boolean isNone() {
            return moved <= 0;
        }
    }

    private MachineFluidItems() {}

    /** Whether this can take fluid FROM a machine: an empty bucket, or a universal canister with room. */
    public static boolean isFillable(ItemStack stack) {
        if (stack.is(Items.BUCKET)) {
            return true;
        }

        return stack.is(HumanItems.CANISTER.get()) && UniversalCanisterItem.getContent(stack).amount() < UniversalCanisterItem.CAPACITY;
    }

    /** The fluid this container holds - a full bucket of any mod, or a universal canister - or EMPTY. */
    public static FluidContent contained(ItemStack stack) {
        if (stack.is(HumanItems.CANISTER.get())) {
            return UniversalCanisterItem.getContent(stack);
        }

        var fluid = FluidContainerInteractions.fluidInBucket(stack.getItem());

        return fluid == null ? FluidContent.EMPTY : FluidContent.of(fluid, FluidContent.BUCKET);
    }

    /**
     * Fills one container from the fluid on offer.
     * <ul>
     * <li>Empty bucket: needs a whole bucket of a fluid that HAS a bucket item.</li>
     * <li>Canister: takes as much as fits, if it is empty or already holds this fluid.</li>
     * </ul>
     */
    public static Result fill(ItemStack container, FluidContent available) {
        if (container.isEmpty() || available.isEmpty()) {
            return Result.NONE;
        }

        if (container.is(Items.BUCKET)) {
            var bucket = available.fluid().getBucket();

            if (available.amount() < FluidContent.BUCKET || bucket == Items.AIR) {
                return Result.NONE;
            }

            return new Result(new ItemStack(bucket), FluidContent.BUCKET);
        }

        if (container.is(HumanItems.CANISTER.get())) {
            var held = UniversalCanisterItem.getContent(container);

            if (!held.accepts(available.fluid())) {
                return Result.NONE;
            }

            var moved = Math.min(available.amount(), UniversalCanisterItem.CAPACITY - held.amount());

            if (moved <= 0) {
                return Result.NONE;
            }

            var filled = container.copyWithCount(1);

            UniversalCanisterItem.setContent(filled, FluidContent.of(available.fluid(), held.amount() + moved));

            return new Result(filled, moved);
        }

        return Result.NONE;
    }

    /**
     * Pours from one container, up to {@code room} millibuckets. A bucket pours all or nothing; a canister pours what
     * fits and keeps the rest.
     */
    public static Result drain(ItemStack container, Fluid fluid, int room) {
        var held = contained(container);

        if (held.isEmpty() || !held.fluid().isSame(fluid) || room <= 0) {
            return Result.NONE;
        }

        if (container.is(HumanItems.CANISTER.get())) {
            var moved = Math.min(room, held.amount());
            var drained = container.copyWithCount(1);

            UniversalCanisterItem.setContent(drained, held.withAmount(held.amount() - moved));

            return new Result(drained, moved);
        }

        if (room < FluidContent.BUCKET) {
            return Result.NONE;
        }

        return new Result(new ItemStack(Items.BUCKET), FluidContent.BUCKET);
    }

    /** Whether a canister has been poured out completely - an empty bucket always has. */
    public static boolean isEmptied(ItemStack container) {
        return contained(container).isEmpty();
    }
}
