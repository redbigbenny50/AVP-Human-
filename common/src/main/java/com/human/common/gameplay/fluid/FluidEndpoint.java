package com.human.common.gameplay.fluid;

import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * Something in the world that holds one fluid: a placed canister, a fluid tank, later the machines' tanks.
 * <p>
 * [stated] Sep 29: pipes are dumb and contents live at the ENDPOINTS - this is the endpoint. One NeoForge handler and
 * one Fabric storage serve every endpoint, and so do the bucket and canister interactions, so a new endpoint gets
 * pipes, other mods' pipes and bucket handling by implementing these methods.
 * </p>
 * <h2>PER-FACE RULES ARE ASKED, NOT CAPTURED</h2> {@link #canFill} and {@link #canDrain} are called at the moment of
 * each transfer. NeoForge and other mods cache the handler they are given; if a face's rule were baked into the
 * handler, toggling that face (a tank side switched to output) would not reach a pipe that already cached it.
 * </p>
 */
public interface FluidEndpoint {

    FluidContent getContent();

    int getCapacity();

    /** @return how much of the fluid fits / was put in, in millibuckets */
    int insert(Fluid fluid, int maxAmount, boolean simulate);

    /**
     * @param fluid the fluid wanted, or null for whatever is inside
     * @return how much would come out / came out, in millibuckets
     */
    int extract(@Nullable Fluid fluid, int maxAmount, boolean simulate);

    /**
     * ⚠ TRANSACTIONAL CALLERS ONLY (Fabric): change without saving or syncing; the change may still be rolled back.
     * Follow with {@link #notifyContentChanged()} once it is final.
     */
    void setContentSilently(FluidContent content);

    void notifyContentChanged();

    /** Whether a pipe on this face may put fluid in. Null = no particular face (a query, a probe mod). */
    boolean canFill(@Nullable Direction side);

    /** Whether a pipe on this face may take fluid out. Null = no particular face. */
    boolean canDrain(@Nullable Direction side);

    /** Whether this face is a port at all. A face that can neither fill nor drain exposes nothing. */
    default boolean isPort(@Nullable Direction side) {
        return canFill(side) || canDrain(side);
    }
}
