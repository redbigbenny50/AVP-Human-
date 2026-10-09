package com.human.common.gameplay.entity.living.human.marine;

import com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.impl.GunStrategy;
import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.GunConfig;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.item.HumanItems;
import com.human.common.registry.tag.HumanItemTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Keeps a marine's guns fed: reloads empty guns from the pack, and restocks a marine that has shot itself dry.
 * <p>
 * Resupply is the offscreen fiction of a squad drawing from its own stores, so it only runs when the marine is
 * genuinely empty and only after a wait. The timer starts when the marine runs dry rather than running continuously, so
 * minutes of fighting do not bank a resupply that lands the instant the last round is spent.
 * </p>
 * <p>
 * [stated] Oct 5, after a camp of marines was found standing among zombies holding empty guns: "marines at their camp
 * or any structure they spawn in will get ammo back faster", "maybe 5 mins is too long then for them maybe set it to 3
 * mins and give them 1 more stack of ammo too", "dry marine always has something to fight with knife", and "if they
 * have no knife for some reason which should be impossible they would fall back to their camp".
 * </p>
 * <p>
 * 🚨 Oct 5 - THE STALL THIS CLASS NOW BREAKS. A marine only reloaded the gun IN USE, and only once it fell below 10% -
 * because an EMPTY gun is not a valid weapon (GunStrategy), so it is put away and never reloaded. For a small magazine
 * (the shotgun) 10% is less than one shell, so the reload could only start at zero, which is exactly when the gun stops
 * counting; a reload cut short by a plan change did the same to any gun. The marine in the report held three empty guns
 * and carried TWELVE shotgun shells - and because it carried ammunition it was not "out", so the resupply never came
 * either. Empty guns are now reloaded from the pack on their own ({@link #tickStowedReload}).
 * </p>
 */
public class MarineResupply {

    /** Wait between running dry and the restock arriving, away from home: three minutes. */
    public static final int RESUPPLY_DELAY_IN_TICKS = 3600;

    /** The same wait for a marine at the camp or structure it spawned in: one minute. */
    public static final int CAMP_RESUPPLY_DELAY_IN_TICKS = 1200;

    /** How close to its home a marine must be to draw from the camp's stores, in blocks. */
    public static final double CAMP_RESUPPLY_RADIUS = 24.0;

    /**
     * Spare ammunition, at spawn and on every resupply: at least this many reloads' worth...
     */
    private static final int SPARE_RELOADS = 3;

    /**
     * ...and at least this many FULL STACKS of the ammunition item, whichever is more. [stated] Oct 5: "give them 2
     * full stacks of ammo they wouldnt need a 3rd stack then". ⚠ The reloads floor is why it is "whichever is more":
     * two stacks of caseless rounds (128) is less than one pulse-rifle reload's worth beyond the first (99 a magazine),
     * and a drum stacks to one - two stacks alone would have cut those guns below what they carried before.
     */
    private static final int SPARE_STACKS = 2;

    /**
     * [stated] Oct 5: "make rockets and sniper rounds only 1 stack so 64". Rockets are a blast stronger than TNT and
     * sniper rounds are one-shot heavy rounds; two stacks of either was far more than any other gun carries.
     */
    private static final int HEAVY_ORDNANCE_STACKS = 1;

    /** Oct 5 audit - quiet time before a tactical reload, in ticks: three seconds without a target. */
    private static final int TACTICAL_RELOAD_QUIET_TICKS = 60;

    /** Oct 5 audit - a round-fed gun below this fraction is topped up once things are quiet. */
    private static final float TACTICAL_RELOAD_FRACTION = 0.5F;

    /**
     * How often the checks run. Ammunition state cannot change without the marine firing, so sampling every second is
     * ample and keeps a scan of the pack off the per-tick path - it is 108 slots now that marines carry a full pack.
     */
    private static final int CHECK_INTERVAL_IN_TICKS = 20;

    /** Value of {@link #resupplyTick} when the marine is not waiting on a resupply. */
    public static final int NO_RESUPPLY_PENDING = Integer.MIN_VALUE;

    /** Tick at which the pending resupply lands, or {@link #NO_RESUPPLY_PENDING}. */
    private int resupplyTick = NO_RESUPPLY_PENDING;

    /** Tick at which the pending stowed reload completes, or {@link #NO_RESUPPLY_PENDING}. */
    private int stowedReloadTick = NO_RESUPPLY_PENDING;

    /** Out of ammunition AND without a melee weapon - refreshed with the checks. */
    private boolean disarmed;

    /** Out of ammunition - refreshed with the checks. */
    private boolean outOfAmmunition;

    /** The ammunition items this marine's guns take - refreshed with the checks. */
    private Set<Item> usableAmmunition = Set.of();

    /** A fresh state; nothing pending. */
    public MarineResupply() {}

    /**
     * Server side, every tick, from {@code Marine.tick}.
     *
     * @param marine the marine this state belongs to
     */
    public void tick(Marine marine) {
        if (marine.tickCount % CHECK_INTERVAL_IN_TICKS != 0) {
            return;
        }

        tickStowedReload(marine);

        outOfAmmunition = isOutOfAmmunition(marine);
        disarmed = outOfAmmunition && !hasMeleeWeapon(marine);
        usableAmmunition = collectUsableAmmunition(marine);

        if (!outOfAmmunition) {
            // Picked something up, was handed a drum, or is carrying a gun that was never empty. Any of those cancels
            // a pending resupply: the timer is for a marine that has nothing, not one that is merely low.
            resupplyTick = NO_RESUPPLY_PENDING;

            return;
        }

        var delay = marine.isNearCampHome(CAMP_RESUPPLY_RADIUS) ? CAMP_RESUPPLY_DELAY_IN_TICKS : RESUPPLY_DELAY_IN_TICKS;

        // Start the wait - or SHORTEN it: a marine that walks back into camp mid-wait gets the camp's minute from
        // there, and a wait saved under the old five-minute rule is cut to the current one on load.
        if (resupplyTick == NO_RESUPPLY_PENDING || resupplyTick - marine.tickCount > delay) {
            resupplyTick = marine.tickCount + delay;

            return;
        }

        if (marine.tickCount >= resupplyTick) {
            resupplyTick = NO_RESUPPLY_PENDING;

            resupply(marine);
            disarmed = false;
            outOfAmmunition = false;
        }
    }

    /**
     * {@return whether the marine has no usable gun and no melee weapon} - it has nothing to fight with, so it falls
     * back to its camp (IdleSensors.IS_AWAY_FROM_HOME) instead of standing among its attackers.
     */
    public boolean isDisarmed() {
        return disarmed;
    }

    /** {@return whether every gun is empty with nothing to reload from, as of the last check} */
    public boolean isOutOfAmmunitionCached() {
        return outOfAmmunition;
    }

    /** {@return the ammunition items this marine's guns take, as of the last check (about once a second)} */
    public Set<Item> getUsableAmmunition() {
        return usableAmmunition;
    }

    /**
     * Whether any gun the marine carries takes {@code item} - checked fresh, for a player handing ammunition over.
     *
     * @param marine the marine
     * @param item   the offered item
     * @return true when one of its guns fires it
     */
    public static boolean usesAmmunition(Marine marine, Item item) {
        return collectUsableAmmunition(marine).contains(item);
    }

    /**
     * Whether {@code itemStack} is gun ammunition of any kind. Client-safe (no inventory needed), so the client can
     * swing the hand for a hand-over the server then decides.
     *
     * @param itemStack the stack
     * @return true for rounds, shells, rockets, fuel and drums
     */
    public static boolean isGunAmmunition(ItemStack itemStack) {
        return itemStack.is(HumanItemTags.AMMO_ITEMS)
            || itemStack.is(HumanItems.DRUM_CARTRIDGE.get())
            || itemStack.is(HumanItems.DRUM_CANNISTER.get());
    }

    /**
     * How many ammunition ITEMS a gun's spares are: the larger of {@value #SPARE_RELOADS} reloads' worth and
     * {@value #SPARE_STACKS} full stacks. Used at spawn (MarineGearDecoratorUtil) and by every resupply.
     *
     * @param gunConfig the gun
     * @return item count, or 0 when the gun takes no ammunition item
     */
    public static int spareAmmunitionItems(GunConfig gunConfig) {
        var ammunitionItemSupplier = gunConfig.ammunitionItemSupplier();

        if (ammunitionItemSupplier == null) {
            return 0;
        }

        var reloadAmount = Math.max(1, gunConfig.reloadAmount());
        // Items per reload, exactly as GunReloading works it out: a drum is one item, loose rounds are many.
        var itemsPerReload = (int) Math.ceil(gunConfig.maximumAmmunition() / (float) reloadAmount);
        var ammunitionItem = ammunitionItemSupplier.get().asItem();
        var stackSize = new ItemStack(ammunitionItem).getMaxStackSize();
        var stacks = ammunitionItem == HumanItems.ROCKET.get() || ammunitionItem == HumanItems.HEAVY_BULLET.get()
            ? HEAVY_ORDNANCE_STACKS
            : SPARE_STACKS;

        return Math.max(itemsPerReload * SPARE_RELOADS, stackSize * stacks);
    }

    /**
     * Whether the pack has room for at least one more {@code item}: an empty slot, or a stack of it that is not full.
     * Lets the ammunition pickup skip stacks it cannot take, instead of walking to them and failing forever.
     *
     * @param marine the marine
     * @param item   the item
     * @return true when at least one fits
     */
    public static boolean canPackTake(Marine marine, Item item) {
        var inventory = marine.getInventory();

        for (var slotIndex = 0; slotIndex < inventory.getSize(); slotIndex++) {
            if (inventory.isSlotEmpty(slotIndex)) {
                return true;
            }

            var stack = inventory.getItemStack(slotIndex);

            if (stack.is(item) && stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }

        return false;
    }

    private static Set<Item> collectUsableAmmunition(Marine marine) {
        var items = new HashSet<Item>();

        forEachCarriedStack(marine, itemStack -> {
            if (itemStack.getItem() instanceof GunItem gunItem) {
                var ammunitionItemSupplier = gunItem.getGunConfig().ammunitionItemSupplier();

                if (ammunitionItemSupplier != null) {
                    items.add(ammunitionItemSupplier.get().asItem());
                }
            }
        });

        return items.isEmpty() ? Set.of() : Set.copyOf(items);
    }

    /**
     * Reloads EMPTY guns from the pack, taking the gun's own reload time.
     * <p>
     * ⚠ Empty only. A gun with rounds left is still a valid weapon and the combat strategy reloads it in use at its
     * threshold; this is for the gun the strategy can no longer see. The gun being fired right now is skipped so a
     * reload never lands mid-use.
     * </p>
     */
    private void tickStowedReload(Marine marine) {
        var gunStack = findEmptyReloadableGun(marine);

        if (gunStack == null) {
            stowedReloadTick = NO_RESUPPLY_PENDING;

            return;
        }

        if (stowedReloadTick == NO_RESUPPLY_PENDING) {
            var reloadTime = ((GunItem) gunStack.getItem()).getGunConfig().reloadTimeInTicks();
            stowedReloadTick = marine.tickCount + Math.max(CHECK_INTERVAL_IN_TICKS, reloadTime);

            return;
        }

        if (marine.tickCount < stowedReloadTick) {
            return;
        }

        stowedReloadTick = NO_RESUPPLY_PENDING;

        forEachCarriedStack(marine, itemStack -> {
            if (isEmptyAndReloadable(marine, itemStack)) {
                GunStrategy.reloadFromPack(marine, (GunItem) itemStack.getItem(), itemStack);
            }
        });
    }

    private static @Nullable ItemStack findEmptyReloadableGun(Marine marine) {
        ItemStack[] found = { null };

        forEachCarriedStack(marine, itemStack -> {
            if (found[0] == null && isEmptyAndReloadable(marine, itemStack)) {
                found[0] = itemStack;
            }
        });

        return found[0];
    }

    /**
     * Whether this gun should be loaded from the pack now: it is EMPTY, or - ⚠ Oct 5 audit, the tactical reload - it is
     * a round-fed gun under half full and the marine has had no target for three seconds. A trained soldier reloads
     * between fights instead of walking into the next one with one shell in a six-shell shotgun. Drum and magazine guns
     * are only reloaded empty: their reload swaps a whole drum, so topping up would waste one.
     */
    private static boolean isEmptyAndReloadable(Marine marine, ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof GunItem gunItem)) {
            return false;
        }

        var gunConfig = gunItem.getGunConfig();
        var ammunition = itemStack.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0);

        if (ammunition > 0) {
            var tacticalReload = marine.ticksSinceLastTarget() >= TACTICAL_RELOAD_QUIET_TICKS
                && Math.max(1, gunConfig.reloadAmount()) == 1
                && ammunition < gunConfig.maximumAmmunition() * TACTICAL_RELOAD_FRACTION;

            if (!tacticalReload) {
                return false;
            }
        }

        if (marine.isUsingItem() && marine.getUseItem() == itemStack) {
            return false;
        }

        var ammunitionItemSupplier = gunItem.getGunConfig().ammunitionItemSupplier();

        return ammunitionItemSupplier != null && marine.getInventory().hasItem(ammunitionItemSupplier.get().asItem());
    }

    /**
     * Whether every gun the marine carries is empty and there is nothing left to reload from.
     * <p>
     * Both halves matter. A marine holding an empty rifle and a full drum is not out - it is one reload away, and the
     * reload will happen on its own. Only when neither the guns nor the pack hold anything is the marine actually
     * reduced to its knife. ⚠ Oct 5 - the hands count too; a gun in hand is not in the pack.
     * </p>
     *
     * @param marine the marine to check
     * @return true when no gun it carries can fire or be reloaded
     */
    public static boolean isOutOfAmmunition(Marine marine) {
        var carriesAGun = new boolean[] { false };
        var canFire = new boolean[] { false };

        forEachCarriedStack(marine, itemStack -> {
            if (canFire[0] || !(itemStack.getItem() instanceof GunItem gunItem)) {
                return;
            }

            carriesAGun[0] = true;

            if (itemStack.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0) > 0) {
                canFire[0] = true;
                return;
            }

            var ammunitionItemSupplier = gunItem.getGunConfig().ammunitionItemSupplier();

            if (ammunitionItemSupplier != null && marine.getInventory().hasItem(ammunitionItemSupplier.get().asItem())) {
                canFire[0] = true;
            }
        });

        // A knife-only marine was never issued a gun and has nothing to resupply.
        return carriesAGun[0] && !canFire[0];
    }

    /**
     * Whether the marine carries anything the melee strategy will fight with - the same test MeleeWeaponStrategy uses:
     * swords (the tactical knife is one), axes, or a mace.
     */
    private static boolean hasMeleeWeapon(Marine marine) {
        var found = new boolean[] { false };

        forEachCarriedStack(marine, itemStack -> {
            if (
                itemStack.is(ItemTags.SWORDS) || itemStack.is(ItemTags.AXES) || itemStack.is(Items.MACE)
            ) {
                found[0] = true;
            }
        });

        return found[0];
    }

    /** Both hands, then every pack slot. */
    private static void forEachCarriedStack(Marine marine, Consumer<ItemStack> visitor) {
        visitor.accept(marine.getMainHandItem());
        visitor.accept(marine.getOffhandItem());

        var inventory = marine.getInventory();

        for (var slotIndex = 0; slotIndex < inventory.getSize(); slotIndex++) {
            visitor.accept(inventory.getItemStack(slotIndex));
        }
    }

    /**
     * Delivers spare ammunition for every gun the marine carries.
     */
    private static void resupply(Marine marine) {
        forEachCarriedStack(marine, itemStack -> {
            if (!(itemStack.getItem() instanceof GunItem gunItem)) {
                return;
            }

            var gunConfig = gunItem.getGunConfig();
            var ammunitionItemSupplier = gunConfig.ammunitionItemSupplier();

            if (ammunitionItemSupplier == null) {
                return;
            }

            var resupplyItems = spareAmmunitionItems(gunConfig);

            if (resupplyItems <= 0) {
                return;
            }

            var resupplyStack = new ItemStack(ammunitionItemSupplier.get(), resupplyItems);

            // Marine-owned, matching the issued spares - so a resupplied marine is not a renewable source of drums for
            // a player who parks one somewhere safe and comes back every few minutes.
            resupplyStack.set(HumanDataComponents.MARINE_OWNED.get(), true);

            marine.getInventory().addItemStack(resupplyStack);
        });
    }

    /** {@return the tick the pending resupply lands, or {@link #NO_RESUPPLY_PENDING}} */
    public int getResupplyTick() {
        return resupplyTick;
    }

    /**
     * Restores a saved pending resupply.
     *
     * @param resupplyTick the tick it lands, or {@link #NO_RESUPPLY_PENDING}
     */
    public void setResupplyTick(int resupplyTick) {
        this.resupplyTick = resupplyTick;
    }
}
