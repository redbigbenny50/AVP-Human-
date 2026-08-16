package com.human.common.gameplay.item.gun;

import com.blib.api.common.enchantment.v1.EnchantmentUtil;
import com.blib.api.common.entity.v1.BLibEntityPredicates;
import com.blib.api.common.server.v1.ServerScheduler;
import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.animation.GunAnimationEvents;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.item.HumanBlockItems;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;

import java.time.Duration;
import java.util.Objects;

public class GunReloading {

    private static final String RELOAD_TOO_EARLY_KEY = "message.avp.reload_too_early";

    /** A magazine may be swapped once it is below this fraction of its capacity. */
    private static final float RELOAD_THRESHOLD = 0.10F;

    public static void reload(Player player) {
        if (player == null) {
            // Player is null, nothing we can do beyond this point.
            return;
        }

        var level = player.level();
        var itemStack = getHeldGunStack(player);
        var item = itemStack.getItem();

        if (!(item instanceof GunItem gunItem)) {
            return;
        }

        var gunConfig = gunItem.getGunConfig();
        var maximumAmmunition = gunConfig.maximumAmmunition();

        int currentAmmunition = itemStack.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0);

        if (currentAmmunition >= maximumAmmunition) {
            // Gun is already max ammo, no need to continue trying to reload.
            return;
        }

        if (!canReloadYet(gunConfig, currentAmmunition, maximumAmmunition)) {
            player.displayClientMessage(Component.translatable(RELOAD_TOO_EARLY_KEY), true);

            return;
        }

        var ammunitionItemSupplier = gunConfig.ammunitionItemSupplier();

        if (ammunitionItemSupplier == null) {
            return;
        }

        var ammunitionItem = ammunitionItemSupplier.get();
        var reloadAmount = gunConfig.reloadAmount();
        var neededAmmunition = (int) Math.ceil((maximumAmmunition - currentAmmunition) / ((float) reloadAmount));

        var isPlayerImmortal = BLibEntityPredicates.isInvulnerable(player);
        // Result is how much we DIDN'T consume.
        var result = isPlayerImmortal
            // If the player is immortal, then assume they can get a full reload.
            ? ItemConsumptionResult.Full.INSTANCE
            // Otherwise, the player needs to use actual ammunition.
            : consumeItemAmountFromInventory(player, ammunitionItem, neededAmmunition, true);

        var ammunitionToRestore = switch (result) {
            // We successfully consumed all ammunition we needed, so this is just an identity assignment.
            case ItemConsumptionResult.Full full -> neededAmmunition;
            // We failed to consume any amount of ammunition, so we can't restore any ammunition.
            case ItemConsumptionResult.None none -> 0;
            // We failed to consume all the necessary ammunition.
            // The difference here is needed - failedToConsumeCount = successfullyConsumed.
            // Ex. we needed 500, we consumed and got back 230 remaining, 500 - 230 = 270, 270 is what we can restore.
            case ItemConsumptionResult.Partial partial -> neededAmmunition - partial.remainingAmount;
        };

        if (ammunitionToRestore == 0) {
            // There's no ammunition to restore, so why would we continue? Return.
            return;
        }

        GunAnimationEvents.trigger(itemStack, GunAnimationEvents.RELOAD);

        var fireModeConfig = gunConfig.getDefaultFireMode();
        var reloadStartSoundEvent = fireModeConfig.reloadStartSoundEvent();

        if (reloadStartSoundEvent != null) {
            level.playSound(null, player.blockPosition(), reloadStartSoundEvent.get(), SoundSource.PLAYERS);
        }

        // NOT capped at maximumAmmunition. A whole-unit reload consumes the drum outright, so capping would silently
        // bin whatever was still in the magazine and punish reloading a moment early. Carrying it over is the reward
        // for good discipline, and it is not free ammunition - it is only the rounds the player already had.
        // Round-fed guns cannot overshoot anyway: their neededAmmunition is computed to land exactly on the maximum.
        itemStack.set(
            HumanDataComponents.AMMUNITION.get(),
            currentAmmunition + (ammunitionToRestore * reloadAmount)
        );

        if (!isPlayerImmortal) {
            var reloadTimeModifier = EnchantmentUtil.getLevel(level, itemStack, Enchantments.QUICK_CHARGE) * 0.2;
            var reloadTimeInTicks = (int) (gunConfig.reloadTimeInTicks() * (1 - reloadTimeModifier));

            player.getCooldowns().addCooldown(itemStack.getItem(), reloadTimeInTicks);

            ServerScheduler.schedule(() -> {
                var reloadFinishSoundEvent = fireModeConfig.reloadFinishSoundEvent();

                if (reloadFinishSoundEvent != null) {
                    var itemInHand = getHeldGunStack(player);

                    if (Objects.equals(itemStack, itemInHand)) {
                        level.playSound(null, player.blockPosition(), reloadFinishSoundEvent.get(), SoundSource.PLAYERS);
                    }
                }
            }, Duration.ofMillis(reloadTimeInTicks * 50L));
        }
    }

    private static ItemStack getHeldGunStack(Player player) {
        var usedItemHand = player.getUsedItemHand();
        var itemStack = player.getItemInHand(usedItemHand);

        if (itemStack.getItem() instanceof GunItem) {
            return itemStack;
        }

        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    public static ItemConsumptionResult consumeItemAmountFromInventory(
        Player player,
        ItemLike ammunitionItem,
        int amountToConsume,
        boolean consume
    ) {
        var result = consumeItemAmountFromInventoryNoSync(player, ammunitionItem, amountToConsume, consume);

        if (result == ItemConsumptionResult.None.INSTANCE) {
            return result;
        }

        // Result must be partial or full, in either case it has changed and needs to be updated to the client.
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return result;
    }

    private static ItemConsumptionResult consumeItemAmountFromInventoryNoSync(
        Player player,
        ItemLike ammunitionItem,
        int amountToConsume,
        boolean consume
    ) {
        var playerInventory = player.getInventory();
        var remainingAmountToConsume = amountToConsume;

        // We iterate over ammo chests first, since we want to consume from them before the player's bare inventory.
        for (var playerItemStack : playerInventory.items) {
            if (!playerItemStack.is(HumanBlockItems.AMMO_CHEST.get())) {
                // Skip non-ammo chests.
                continue;
            }

            // Try and consume some amount from the ammo chest.
            var result = consumeFromAmmoChestItem(playerItemStack, remainingAmountToConsume, ammunitionItem, consume);

            switch (result) {
                case ItemConsumptionResult.Full full -> {
                    // We consumed all items successfully, so we can exit the function.
                    return full;
                }
                case ItemConsumptionResult.Partial partial -> {
                    // We were able to consume some of the items, but not all.
                    remainingAmountToConsume = partial.remainingAmount;
                }
                case ItemConsumptionResult.None none -> {
                    // We weren't able to consume any items and need to continue searching, so this is a NO-OP.
                }
            }
        }

        for (var playerItemStack : playerInventory.items) {
            if (!playerItemStack.is(ammunitionItem.asItem())) {
                // Skip non-ammunition items.
                continue;
            }

            var consumeCount = Math.min(playerItemStack.getCount(), remainingAmountToConsume);

            if (consume) {
                playerItemStack.shrink(consumeCount);
            }

            remainingAmountToConsume -= consumeCount;

            if (remainingAmountToConsume == 0) {
                return ItemConsumptionResult.Full.INSTANCE;
            }
        }

        if (remainingAmountToConsume == amountToConsume) {
            return ItemConsumptionResult.None.INSTANCE;
        }

        return new ItemConsumptionResult.Partial(remainingAmountToConsume);
    }

    private static ItemConsumptionResult consumeFromAmmoChestItem(
        ItemStack ammoChestStack,
        int amountToConsume,
        ItemLike ammunitionItem,
        boolean consume
    ) {
        var container = ammoChestStack.get(net.minecraft.core.component.DataComponents.CONTAINER);

        if (container == null || amountToConsume <= 0) {
            // Can't consume or nothing to consume.
            return ItemConsumptionResult.None.INSTANCE;
        }

        var remainingAmountToConsume = amountToConsume;

        for (var itemStack : container.nonEmptyItems()) {
            if (!itemStack.is(ammunitionItem.asItem())) {
                // Skip non-ammunition items.
                continue;
            }

            // Item is our target consumable by this point.
            var consumeCount = Math.min(itemStack.getCount(), remainingAmountToConsume);

            if (consume) {
                itemStack.shrink(consumeCount);
            }

            remainingAmountToConsume -= consumeCount;

            if (remainingAmountToConsume == 0) {
                // We've consumed all the items we needed to, so return.
                return ItemConsumptionResult.Full.INSTANCE;
            }
        }

        // If we reached this point, that means we didn't fully consume the desired amount.
        return new ItemConsumptionResult.Partial(remainingAmountToConsume);
    }

    public sealed interface ItemConsumptionResult {

        enum None implements ItemConsumptionResult {
            INSTANCE
        }

        enum Full implements ItemConsumptionResult {
            INSTANCE
        }

        record Partial(int remainingAmount) implements ItemConsumptionResult {}
    }

    /**
     * Whether the magazine is empty enough to be worth swapping.
     * <p>
     * This only applies to guns reloaded a WHOLE UNIT at a time — a drum, a fuel tank — where one reload consumes one
     * item no matter how much was left. Without a gate, tapping reload after a short burst throws away almost an entire
     * drum for a handful of rounds. Round-fed weapons are untouched: they consume exactly what they load, so topping up
     * early costs nothing and refusing it would be pure friction.
     * <p>
     * The threshold is deliberately not zero. Players reload in the lull rather than at the click of an empty gun, and
     * the leftover rounds are carried over rather than binned, so reloading early is rewarded instead of punished.
     */
    private static boolean canReloadYet(GunConfig gunConfig, int currentAmmunition, int maximumAmmunition) {
        if (gunConfig.reloadAmount() < maximumAmmunition) {
            return true;
        }

        return currentAmmunition < maximumAmmunition * RELOAD_THRESHOLD;
    }
}
