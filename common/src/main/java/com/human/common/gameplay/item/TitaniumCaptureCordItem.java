package com.human.common.gameplay.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The titanium capture cord. [stated] Oct 1: like a lead but less likely to break, and it works on ALL mobs - hostile
 * ones, villagers, anything a lead refuses. It is how specimens are walked to a cryotank.
 * <p>
 * [stated] Oct 3 - the clean route: it holds with its own {@link CaptureCordTether}, not vanilla's leash, so vanilla's
 * rope is never drawn beside it and a snapped cord drops the cord, never a lead.
 * </p>
 */
public class TitaniumCaptureCordItem extends Item {

    /** Saved on a held mob. */
    public static final String CORD_TAG = "avp_human.capture_cord";

    /** How far the cord stretches before it snaps. */
    public static final double SNAP_DISTANCE = 16.0;

    public TitaniumCaptureCordItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(
        @NotNull ItemStack stack,
        @NotNull Player player,
        @NotNull LivingEntity target,
        @NotNull InteractionHand hand
    ) {
        // Already on a cord or a lead: leave it be. (Right-clicking the one you hold lets it go - see the Mob mixin.)
        if (!(target instanceof Mob mob) || !mob.isAlive() || mob.isLeashed() || CaptureCordTether.isHeld(mob)) {
            return InteractionResult.PASS;
        }

        if (!player.level().isClientSide) {
            // ⚠ Any mob: canBeLeashed() refuses every Enemy and villagers, which is exactly what the cord exists to get
            // past.
            CaptureCordTether.attach(mob, player);
            stack.consume(1, player);
        }

        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
