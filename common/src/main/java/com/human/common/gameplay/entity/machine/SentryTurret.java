package com.human.common.gameplay.entity.machine;

import com.human.common.data.HumanTooltipTranslationKeys;
import com.human.common.property.HumanProperties;
import com.human.common.property.HumanPropertyAccess;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.HumanSoundEvents;
import com.human.common.registry.key.HumanDamageTypeKeys;
import com.human.common.registry.tag.HumanDamageTypesTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The sentry turret.
 * <h2>POWER</h2> [stated] Sep 30: the sentry "will require power" -
 * <ul>
 * <li>It stores its own charge: {@value #CAPACITY} FE, and each SHOT costs {@value #SHOT_COST} FE. Nothing is drawn
 * while it waits, tracks or has no target - the grid's no-idle-draw rule.</li>
 * <li>UNBOUND, it charges itself from a battery pack or nuclear battery in its battery slot (a drained nuclear battery
 * is used up).</li>
 * <li>BOUND to a fuse panel (wiring tool: select the panel, right-click the turret), the panel keeps it topped up and
 * the screen shows its tank instead of the battery slot.</li>
 * <li>Picking it up keeps its charge on the item; the battery in its slot drops.</li>
 * </ul>
 * Power replaces the old redstone requirement: a turret placed before this starts empty and waits for charge.
 */
public class SentryTurret extends Mob implements TraceableEntity, net.minecraft.world.MenuProvider {

    /**
     * Whether a sentry turret stands in this block. [stated] Oct 5: a cable core was placed right inside a turret.
     * Players may walk through pipes and cables, so the placement check - run as the player - saw no collision shape
     * and let them in. Cables and pipes ask this instead, so the walk-through stays.
     */
    public static boolean standsIn(net.minecraft.world.level.Level level, BlockPos pos) {
        var space = new net.minecraft.world.phys.AABB(pos);
        return !level.getEntitiesOfClass(SentryTurret.class, space, SentryTurret::isAlive).isEmpty();
    }

    /** [stated] Sep 30: storage on the turret - one battery pack fills it. */
    public static final long CAPACITY = 100_000L;

    /** [stated] Sep 30: 50 FE a shot. */
    public static final long SHOT_COST = 50L;

    /** The most a fuse panel refills it by in one tick. */
    public static final long PANEL_REFILL_PER_TICK = 2_000L;

    private static final String ENERGY_KEY = "Energy";

    private static final String BATTERY_KEY = "Battery";

    private static final String FUSE_PANEL_KEY = "FusePanel";

    private long energy;

    private @Nullable BlockPos fusePanel;

    /** The battery slot: a battery pack or nuclear battery. */
    private final net.minecraft.world.SimpleContainer battery = new net.minecraft.world.SimpleContainer(1) {

        @Override
        public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof com.human.common.gameplay.item.power.BatteryItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };

    public static float DAMAGE = HumanPropertyAccess.INSTANCE.get(HumanProperties.Blocks.SentryTurret.DAMAGE);

    public static int RANGE = HumanPropertyAccess.INSTANCE.get(HumanProperties.Blocks.SentryTurret.RANGE);

    protected static int FOV = HumanPropertyAccess.INSTANCE.get(HumanProperties.Blocks.SentryTurret.FOV);

    private static final int MAX_TURRET_FIRE_COOLDOWN_IN_TICKS = 2;

    // ------------------------------------------------------------------------------------------------------------
    // [stated] Oct 2 - the sentry overhaul. The turret fires ONLY from its own loadout; the nearby-chest search (an
    // 11x11x11 block-entity scan) is gone.
    // ------------------------------------------------------------------------------------------------------------

    /** 10 s of continuous fire before the turret overheats (20 s with the cooling card), then 5 s to cool down. */
    public static final int OVERHEAT_TICKS = 200;

    public static final int COOLDOWN_TICKS = 100;

    /** A rocket every 50 ticks. */
    public static final int ROCKET_INTERVAL = 50;

    public static final float ROCKET_POWER = 2.0F;

    /** Caseless rounds hit half as hard again. */
    public static final float CASELESS_MULTIPLIER = 1.5F;

    /** ⚡ With nothing targeted, look for something every half second rather than every shot. */
    private static final int TARGET_SEARCH_INTERVAL = 10;

    /** The motion lens: hears movement 16 blocks beyond the turret's range, never past 80. */
    private static final int MOTION_SCAN_INTERVAL = 20;

    private static final int MOTION_EXTRA_RANGE = 16;

    private static final int MOTION_MAX_RANGE = 80;

    private static final String LOADOUT_KEY = "Loadout";

    private static final String FILTER_KEY = "TargetFilter";

    private static final String HEAT_KEY = "Heat";

    private static final String OVERHEAT_KEY = "Overheat";

    private static final String REDSTONE_KEY = "Redstone";

    private static final String BASE_YAW_KEY = "BaseYaw";

    /**
     * Oct 16 - which animation clip clients should play (idle / unpowered / firing); see SentryTurretAnimDispatcher.
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Byte> DATA_ANIM =
        net.minecraft.network.syncher.SynchedEntityData.defineId(
            SentryTurret.class,
            net.minecraft.network.syncher.EntityDataSerializers.BYTE
        );

    /** [stated] Oct 2: the skin in the skin slot, synced so every client draws it - empty for the USCM default. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<String> DATA_SKIN =
        net.minecraft.network.syncher.SynchedEntityData.defineId(
            SentryTurret.class,
            net.minecraft.network.syncher.EntityDataSerializers.STRING
        );

    // ------------------------------------------------------------------------------------------------------------
    // [stated] Oct 2 - the camera lens and the network card.
    // ------------------------------------------------------------------------------------------------------------

    /** Re-register on the network this often while the card is in. */
    private static final int NETWORK_REFRESH_INTERVAL = 40;

    /**
     * The chunks kept running while someone looks through the camera.
     * <p>
     * ⚠ A region ticket's level is 33 minus its radius, and ENTITIES only tick at level 31 or below (vanilla's
     * ChunkLevel: 33 loaded, 32 block-ticking, 31 entity-ticking). Radius 1 would load the chunks but leave the
     * turret's own at 32 - the turret itself would stop ticking for a viewer past their simulation distance. Radius 2:
     * the turret's chunk entity-ticking, the 3x3 round it block-ticking, the 5x5 edge only loaded.
     * </p>
     */
    private static final int CAMERA_TICKET_RADIUS = 2;

    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> CAMERA_TICKET =
        net.minecraft.server.level.TicketType.create(
            "avp_human_sentry_camera",
            java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong)
        );

    /** Who is looking through which turret: player -> turret. Server only. */
    private static final java.util.Map<UUID, UUID> CAMERA_SESSIONS = new java.util.HashMap<>();

    /** The player looking through this turret's camera, or null. Not saved - a session ends with the server. */
    private @Nullable UUID controller;

    private @Nullable net.minecraft.world.level.ChunkPos ticketChunk;

    private float manualYaw;

    private float manualPitch;

    private boolean trigger;

    /** The direction it was placed facing - its cone is centred on this, whoever is aiming it. */
    private float baseYaw = Float.NaN;

    private boolean onNetwork;

    /** [stated] Oct 2: the turret's Redstone side button - ignore, fire only with a signal, or only without one. */
    private com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode redstoneMode =
        com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode.IGNORE;

    /** Ammo, cards, skin, network card and lens - the rules are {@link SentryLoadout}. The battery stays separate. */
    private final net.minecraft.world.SimpleContainer loadout = new net.minecraft.world.SimpleContainer(SentryLoadout.SIZE) {

        @Override
        public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
            return SentryLoadout.mayPlace(this, slot, stack);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            recountLoadout();
        }
    };

    /** What it shoots; unconfigured, monsters only (the filter's own default). Saved on the turret. */
    private com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter targetFilter =
        new com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter();

    // ⚡ The loadout, read once whenever it changes - the turret's every tick only reads these.
    private int range;

    private double coneCosine;

    private int heatLimit;

    private boolean defense;

    private boolean caseless;

    private boolean rockets;

    private boolean emLens;

    private boolean motionLens;

    private int heat;

    private int overheat;

    private void recountLoadout() {
        range = RANGE + (SentryLoadout.hasCard(loadout, com.human.common.gameplay.power.upgrade.UpgradeCard.RANGE)
            ? com.human.common.gameplay.power.upgrade.UpgradeCard.RANGE_BONUS
            : 0);
        var halfCone = FOV + (SentryLoadout.hasCard(loadout, com.human.common.gameplay.power.upgrade.UpgradeCard.WIDE_CONE)
            ? com.human.common.gameplay.power.upgrade.UpgradeCard.CONE_BONUS_DEGREES
            : 0);
        coneCosine = Math.cos(Math.toRadians(Math.min(180, halfCone)));
        heatLimit = (int) (OVERHEAT_TICKS * (SentryLoadout.hasCard(loadout, com.human.common.gameplay.power.upgrade.UpgradeCard.COOLING)
            ? com.human.common.gameplay.power.upgrade.UpgradeCard.COOLING_MULTIPLIER
            : 1.0));
        defense = SentryLoadout.hasCard(loadout, com.human.common.gameplay.power.upgrade.UpgradeCard.DEFENSE);
        caseless = SentryLoadout.hasCard(loadout, com.human.common.gameplay.power.upgrade.UpgradeCard.CASELESS_AMMO);
        rockets = SentryLoadout.hasCard(loadout, com.human.common.gameplay.power.upgrade.UpgradeCard.ROCKET_AMMO);

        // Only the server sets synced data; the client receives it.
        if (!level().isClientSide) {
            var skin = com.human.common.gameplay.item.SkinKitItem.skinOf(loadout.getItem(SentryLoadout.SKIN));
            entityData.set(DATA_SKIN, skin == null ? "" : skin.toString());
        }

        var lens = loadout.getItem(SentryLoadout.SENSOR);
        emLens = lens.is(com.human.common.registry.init.item.HumanItems.EM_SENSOR.get());
        motionLens = lens.is(com.human.common.registry.init.item.HumanItems.MOTION_SENSOR.get());
    }

    /** Rockets with the rocket card, caseless rounds with the caseless card, otherwise medium rounds. */
    public net.minecraft.world.item.Item ammoType() {
        return rockets
            ? com.human.common.registry.init.item.HumanItems.ROCKET.get()
            : caseless
                ? com.human.common.registry.init.item.HumanItems.CASELESS_BULLET.get()
                : com.human.common.registry.init.item.HumanItems.MEDIUM_BULLET.get();
    }

    private static final String FIRE_COOLDOWN_KEY = "FireCooldown";

    private static final String OWNER_KEY = "Owner";

    @Nullable
    private UUID ownerUUID;

    @Nullable
    private Entity cachedOwner;

    private int fireCooldown = 0;

    protected final SentryTurretAnimDispatcher animDispatcher;

    public SentryTurret(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        animDispatcher = new SentryTurretAnimDispatcher(this);
        recountLoadout();
    }

    public void setOwner(@Nullable Entity owner) {
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
            this.cachedOwner = owner;
        }
    }

    @Nullable
    @Override
    public Entity getOwner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        } else if (ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            cachedOwner = serverLevel.getEntity(ownerUUID);
            return cachedOwner;
        } else {
            return null;
        }
    }

    public static AttributeSupplier.Builder createSentryTurretAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 16.0F)
            .add(Attributes.MOVEMENT_SPEED, 0.0F)
            .add(Attributes.KNOCKBACK_RESISTANCE, 100.0D)
            .add(Attributes.FOLLOW_RANGE, RANGE);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            animDispatcher.applyOnClient();
        }

        if (!level().isClientSide) {
            tickPower();

            if (!hasShotPower()) {
                animDispatcher.unpowered();
                return;
            } else if (getTarget() == null) {
                animDispatcher.idle();
            }

            if (Float.isNaN(baseYaw)) {
                baseYaw = getYRot();
            }

            if (tickCount % NETWORK_REFRESH_INTERVAL == 0) {
                updateNetwork();
            }

            // Someone is looking through the camera: they aim and fire, not the turret.
            if (controller != null) {
                tickManual();
                return;
            }

            // ⚡ The neighbour check only runs when the mode cares.
            if (
                redstoneMode != com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode.IGNORE && !redstoneMode.allows(
                    level().hasNeighborSignal(blockPosition())
                )
            ) {
                setTarget(null);
                coolDown();
                animDispatcher.idle();
                return;
            }

            // Overheated: it waits out the cool-down, then starts cold.
            if (overheat > 0) {
                overheat--;
                setTarget(null);
                animDispatcher.idle();
                return;
            }

            if (motionLens && tickCount % MOTION_SCAN_INTERVAL == 0) {
                listenForMotion();
            }

            if (!SentryLoadout.hasAmmo(loadout, ammoType())) {
                setTarget(null);
                coolDown();
                animDispatcher.idle();
                return;
            }

            if (getTarget() != null && !getTarget().isAlive()) {
                setTarget(null);
                animDispatcher.idle();
                return;
            }

            // Heat: builds while it has a target, falls while it has none.
            if (getTarget() != null) {
                if (++heat >= heatLimit) {
                    heat = 0;
                    overheat = COOLDOWN_TICKS;
                    setTarget(null);
                    level().playSound(
                        null,
                        blockPosition(),
                        net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        0.8F,
                        1.2F
                    );
                    return;
                }
            } else {
                coolDown();
            }

            if (fireCooldown > 0) {
                fireCooldown--;
                return;
            }

            targetAndFire();
        }
    }

    private void coolDown() {
        heat = Math.max(0, heat - 2);
    }

    /**
     * [stated] Oct 2 - the motion lens: hears anything it would shoot moving within its range plus 16 blocks, all the
     * way round, and beeps when it is outside what the turret can hit. ⚡ Once a second, not every tick.
     */
    private void listenForMotion() {
        var hearing = Math.min(MOTION_MAX_RANGE, range + MOTION_EXTRA_RANGE);
        var heard = level().getEntitiesOfClass(
            LivingEntity.class,
            getBoundingBox().inflate(hearing),
            entity -> entity != this && entity.isAlive() && entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4
                && isEnemy(entity) && distanceTo(entity) <= hearing
        );

        for (var entity : heard) {
            if (distanceTo(entity) > range || !isFacingTarget(entity)) {
                level().playSound(
                    null,
                    blockPosition(),
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BIT.value(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.6F
                );
                return;
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID(OWNER_KEY)) {
            this.ownerUUID = compound.getUUID(OWNER_KEY);
            this.cachedOwner = null;
        }

        this.fireCooldown = compound.getInt(FIRE_COOLDOWN_KEY);
        this.energy = Math.clamp(compound.getLong(ENERGY_KEY), 0L, CAPACITY);
        this.fusePanel = compound.contains(FUSE_PANEL_KEY) ? BlockPos.of(compound.getLong(FUSE_PANEL_KEY)) : null;
        battery.fromTag(compound.getList(BATTERY_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND), registryAccess());
        loadout.fromTag(compound.getList(LOADOUT_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND), registryAccess());

        if (compound.contains(FILTER_KEY)) {
            targetFilter = com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter.load(
                compound.getCompound(FILTER_KEY)
            );
        }

        heat = compound.getInt(HEAT_KEY);
        redstoneMode = com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode.byOrdinal(compound.getInt(REDSTONE_KEY));
        baseYaw = compound.contains(BASE_YAW_KEY) ? compound.getFloat(BASE_YAW_KEY) : Float.NaN;
        overheat = compound.getInt(OVERHEAT_KEY);
        recountLoadout();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        if (ownerUUID != null) {
            compound.putUUID(OWNER_KEY, ownerUUID);
        }

        compound.putInt(FIRE_COOLDOWN_KEY, fireCooldown);
        compound.putLong(ENERGY_KEY, energy);

        if (fusePanel != null) {
            compound.putLong(FUSE_PANEL_KEY, fusePanel.asLong());
        }

        compound.put(BATTERY_KEY, battery.createTag(registryAccess()));
        compound.put(LOADOUT_KEY, loadout.createTag(registryAccess()));
        compound.put(FILTER_KEY, targetFilter.save());
        compound.putInt(HEAT_KEY, heat);
        compound.putInt(REDSTONE_KEY, redstoneMode.ordinal());

        if (!Float.isNaN(baseYaw)) {
            compound.putFloat(BASE_YAW_KEY, baseYaw);
        }
        compound.putInt(OVERHEAT_KEY, overheat);
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        var stack = HumanBlocks.SENTRY_TURRET.get().asItem().getDefaultInstance();

        // The charge travels with the turret when it is picked up.
        if (energy > 0L) {
            stack.set(com.human.common.registry.init.HumanDataComponents.STORED_POWER.get(), energy);
        }

        return stack;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean canBeAffected(@NotNull MobEffectInstance effectInstance) {
        // Sentry turrets aren't affected by any effects, even positive ones. It doesn't make sense for a turret
        // to have regeneration or absorption as much as it doesn't make sense for them to have nausea or wither.
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is((HumanDamageTypesTags.DOES_NOT_HURT_SENTRY_TURRETS))) {
            return false;
        }

        // Its own rockets.
        if (source.getEntity() == this || source.getDirectEntity() == this) {
            return false;
        }

        // [stated] Oct 2 - the defence card: as tough as obsidian. Immune to explosions, half damage from everything
        // else.
        if (defense) {
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
                return false;
            }

            amount *= (float) com.human.common.gameplay.power.upgrade.UpgradeCard.DEFENSE_DAMAGE_MULTIPLIER;
        }

        return super.hurt(source, amount);
    }

    /**
     * ⚠ An explosion knocks back every entity it does not ignore - isPushable is not consulted - so this, not hurt(),
     * is what stops the turret's own rocket shoving it off its spot. With the defence card it ignores every explosion:
     * no damage and no knockback.
     */
    @Override
    public boolean ignoreExplosion(@NotNull net.minecraft.world.level.Explosion explosion) {
        return explosion.getDirectSourceEntity() == this || explosion.getIndirectSourceEntity() == this || defense;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        // Oct 14 FIX - [stated] Sep 30: "select the panel, right-click the turret" binds it to a fuse panel. The
        // binding
        // lives in WiringToolItem.interactLivingEntity, but vanilla's Player.interactOn asks the ENTITY first and only
        // reaches the held item if the entity did not consume the click (verified in the 1.21.1 bytecode). This method
        // always consumed it - it opened the screen - so the tool never got a turn and no turret could ever be bound.
        // A wiring tool always passes the click on: with a panel selected it connects the turret, with nothing selected
        // it selects the turret for connecting device-first ([stated] Oct 14). The screen opens from any other item.
        if (player.getItemInHand(hand).getItem() instanceof com.human.common.gameplay.item.WiringToolItem) {
            return InteractionResult.PASS;
        }

        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        var isOwner = getOwner() != null && getOwner().is(player);

        // ⚠ PICKING THE TURRET UP NOW NEEDS A SNEAK. It used to be a plain right-click, but that is also the gesture
        // for "what is wrong with this thing", and asking a turret why it is silent should never pocket it mid-fight.
        // Sneaking is the deliberate action, which is the right way round for the destructive one.
        if (isOwner && player.isShiftKeyDown()) {
            // [stated] Oct 2: everything travels inside the turret item, like a shulker box.
            dropTurretItem();
            discard();

            return InteractionResult.SUCCESS;
        }

        // [stated] Sep 30: a screen with the battery slot, or its tank when wired to a fuse panel. The status report
        // moved into it.
        player.openMenu(this);

        // The Targeting page shows what is actually set - sent beside the screen, as the marine's is.
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.human.Human.MOD.networking()
                .sendToClient(
                    serverPlayer,
                    new com.human.common.network.packet.S2CMarineSentryFilterPayload(targetFilter.copy())
                );
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canBeCollidedWith() {
        return isAlive();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    private void dropTurretItem() {
        var turretItem = packIntoItem();
        spawnAtLocation(turretItem, 0.5f);
    }

    /** Slot of the battery in a packed turret item: after the loadout. */
    private static final int PACKED_BATTERY = SentryLoadout.SIZE;

    /**
     * The turret as an item, carrying its charge, loadout, battery and targeting settings.
     * <p>
     * ⚠ No duplication: the contents are MOVED - the turret's own slots are emptied here, before it is removed - so
     * they exist on the item and nowhere else. Placing the item moves them back ({@link #unpackFromItem}).
     * </p>
     */
    private ItemStack packIntoItem() {
        var stack = getPickResult();

        if (stack == null) {
            stack = HumanBlocks.SENTRY_TURRET.get().asItem().getDefaultInstance();
        }

        var contents = new ArrayList<ItemStack>();

        for (int slot = 0; slot < SentryLoadout.SIZE; slot++) {
            contents.add(loadout.removeItemNoUpdate(slot));
        }

        contents.add(battery.removeItemNoUpdate(0));
        stack.set(
            net.minecraft.core.component.DataComponents.CONTAINER,
            net.minecraft.world.item.component.ItemContainerContents.fromItems(contents)
        );

        var settings = new CompoundTag();
        settings.put(FILTER_KEY, targetFilter.save());
        settings.putInt(REDSTONE_KEY, redstoneMode.ordinal());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(settings));

        loadout.setChanged();
        return stack;
    }

    /** Placing a packed turret: its contents and settings move back in. The placed item is used up as normal. */
    public void unpackFromItem(ItemStack stack) {
        var contents = stack.get(net.minecraft.core.component.DataComponents.CONTAINER);

        if (contents != null) {
            var items = net.minecraft.core.NonNullList.withSize(PACKED_BATTERY + 1, ItemStack.EMPTY);
            contents.copyInto(items);

            for (int slot = 0; slot < SentryLoadout.SIZE; slot++) {
                loadout.setItem(slot, items.get(slot));
            }

            battery.setItem(0, items.get(PACKED_BATTERY));
        }

        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);

        if (data != null && data.contains(FILTER_KEY)) {
            var settings = data.copyTag();
            targetFilter = com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter.load(
                settings.getCompound(FILTER_KEY)
            );
            redstoneMode = com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode.byOrdinal(settings.getInt(REDSTONE_KEY));
        }

        recountLoadout();
    }

    /** Destroyed, not picked up: everything in it drops. */
    private void dropLoadout() {
        for (int slot = 0; slot < SentryLoadout.SIZE; slot++) {
            var stack = loadout.removeItemNoUpdate(slot);

            if (!stack.isEmpty()) {
                spawnAtLocation(stack, 0.5F);
            }
        }

        loadout.setChanged();
    }

    public net.minecraft.world.SimpleContainer getLoadout() {
        return loadout;
    }

    public com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter getTargetFilter() {
        return targetFilter;
    }

    public void setTargetFilter(com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter filter) {
        this.targetFilter = filter;
    }

    public com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode mode) {
        redstoneMode = mode;
    }

    /** Whether {@code player} may change this turret's settings - its owner, or anyone if it has none. */
    public boolean canConfigure(Player player) {
        var owner = getOwner();
        return owner == null || owner.is(player);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN, "");
        builder.define(DATA_ANIM, SentryTurretAnimDispatcher.IDLE);
    }

    /** Server: which clip clients should play. Only sent when it changes. */
    void setAnimState(byte state) {
        if (!level().isClientSide && entityData.get(DATA_ANIM) != state) {
            entityData.set(DATA_ANIM, state);
        }
    }

    byte getAnimState() {
        return entityData.get(DATA_ANIM);
    }

    /** The skin to draw, or null for the default. */
    public @Nullable net.minecraft.resources.ResourceLocation getSkin() {
        var skin = entityData.get(DATA_SKIN);
        return skin.isEmpty() ? null : net.minecraft.resources.ResourceLocation.tryParse(skin);
    }

    public boolean hasCamera() {
        return loadout.getItem(SentryLoadout.SENSOR).is(com.human.common.registry.init.item.HumanItems.CAMERA_SENSOR.get());
    }

    public boolean hasNetworkCard() {
        return !loadout.getItem(SentryLoadout.NETWORK).isEmpty();
    }

    /** ⚡ Re-registers only every couple of seconds, and the saved list is only marked dirty when the entry changes. */
    private void updateNetwork() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        var network = SentryNetwork.get(serverLevel.getServer());

        if (!hasNetworkCard()) {
            if (onNetwork) {
                network.remove(getUUID());
                onNetwork = false;
            }

            return;
        }

        var name = hasCustomName()
            ? getCustomName().getString()
            : "Sentry " + getUUID().toString().substring(0, 4).toUpperCase(java.util.Locale.ROOT);
        network.put(
            new SentryNetwork.Entry(getUUID(), ownerUUID, serverLevel.dimension().location().toString(), blockPosition(), name, hasCamera())
        );
        onNetwork = true;
    }

    /**
     * Why {@code player} cannot look through this turret right now, or null if they can: it needs its camera lens, its
     * network card and power, the player must own it and be in its dimension, and it must be within their render
     * distance - their client can only draw chunks it has been sent.
     */
    public @Nullable Component cameraRefusal(net.minecraft.server.level.ServerPlayer player) {
        if (!hasCamera() || !hasNetworkCard()) {
            return Component.translatable("message.avp_human.sentry.camera.needs_parts");
        }

        if (!canConfigure(player)) {
            return Component.translatable("message.avp_human.sentry.camera.not_owner");
        }

        if (!hasShotPower()) {
            return Component.translatable("message.avp_human.sentry.camera.no_power");
        }

        if (player.level() != level()) {
            return Component.translatable("message.avp_human.sentry.camera.too_far");
        }

        var viewChunks = Math.min(player.server.getPlayerList().getViewDistance(), player.requestedViewDistance());
        var playerChunk = player.chunkPosition();
        var turretChunk = chunkPosition();

        if (Math.max(Math.abs(playerChunk.x - turretChunk.x), Math.abs(playerChunk.z - turretChunk.z)) > viewChunks - 1) {
            return Component.translatable("message.avp_human.sentry.camera.too_far");
        }

        return null;
    }

    /** Starts a camera session: loads the chunks round the turret and tells the player's client to look through it. */
    public void startControl(net.minecraft.server.level.ServerPlayer player) {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        // One turret per player, one player per turret.
        var previous = CAMERA_SESSIONS.get(player.getUUID());

        if (previous != null && serverLevel.getEntity(previous) instanceof SentryTurret other) {
            other.stopControl();
        }

        stopControl();

        controller = player.getUUID();
        CAMERA_SESSIONS.put(controller, getUUID());
        manualYaw = getYRot();
        manualPitch = getXRot();
        trigger = false;

        ticketChunk = chunkPosition();
        serverLevel.getChunkSource().addRegionTicket(CAMERA_TICKET, ticketChunk, CAMERA_TICKET_RADIUS, ticketChunk);

        com.human.Human.MOD.networking().sendToClient(player, new com.human.common.network.packet.S2CSentryViewPayload(getId()));
    }

    /** Ends the session, if there is one: releases the chunks, hands the player their own view back, stops firing. */
    public void stopControl() {
        if (controller == null) {
            return;
        }

        var player = level() instanceof net.minecraft.server.level.ServerLevel serverLevel
            ? serverLevel.getServer().getPlayerList().getPlayer(controller)
            : null;

        CAMERA_SESSIONS.remove(controller, getUUID());
        controller = null;
        trigger = false;

        if (ticketChunk != null && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getChunkSource().removeRegionTicket(CAMERA_TICKET, ticketChunk, CAMERA_TICKET_RADIUS, ticketChunk);
            ticketChunk = null;
        }

        if (!Float.isNaN(baseYaw)) {
            setYRot(baseYaw);
            setYHeadRot(baseYaw);
            setXRot(0.0F);
        }

        if (player != null) {
            com.human.Human.MOD.networking()
                .sendToClient(
                    player,
                    new com.human.common.network.packet.S2CSentryViewPayload(com.human.common.network.packet.S2CSentryViewPayload.END)
                );
        }
    }

    /** The controls from the player's client - aim clamped to the turret's cone, pitch to +/-60 degrees. */
    public void applyControl(float yaw, float pitch, boolean trigger) {
        var halfCone = (float) Math.toDegrees(Math.acos(coneCosine));
        var base = Float.isNaN(baseYaw) ? getYRot() : baseYaw;
        var offset = net.minecraft.util.Mth.wrapDegrees(yaw - base);

        manualYaw = base + net.minecraft.util.Mth.clamp(offset, -halfCone, halfCone);
        manualPitch = net.minecraft.util.Mth.clamp(pitch, -60.0F, 60.0F);
        this.trigger = trigger;
    }

    /** The turret this player is looking through, if any. */
    public static @Nullable SentryTurret controlledBy(net.minecraft.server.level.ServerPlayer player) {
        var turret = CAMERA_SESSIONS.get(player.getUUID());
        return turret != null && player.serverLevel().getEntity(turret) instanceof SentryTurret sentry && player.getUUID()
            .equals(sentry.controller) ? sentry : null;
    }

    /** A camera session's tick: stop it if anything is no longer true, otherwise aim and fire as told. */
    private void tickManual() {
        var player = ((net.minecraft.server.level.ServerLevel) level()).getServer().getPlayerList().getPlayer(controller);

        if (player == null || !player.isAlive() || cameraRefusal(player) != null) {
            stopControl();
            return;
        }

        setYRot(manualYaw);
        setYHeadRot(manualYaw);
        setXRot(manualPitch);

        if (overheat > 0) {
            overheat--;
            return;
        }

        if (fireCooldown > 0) {
            fireCooldown--;
        }

        if (!trigger) {
            coolDown();
            return;
        }

        if (++heat >= heatLimit) {
            heat = 0;
            overheat = COOLDOWN_TICKS;
            level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.2F);
            return;
        }

        if (fireCooldown == 0 && SentryLoadout.consumeAmmo(loadout, ammoType())) {
            fireManually();
        }
    }

    /** Fires along the aim: the first thing in the line, within range - a block stops it. */
    private void fireManually() {
        var eye = getEyePosition();
        var look = net.minecraft.world.phys.Vec3.directionFromRotation(manualPitch, manualYaw);
        var end = eye.add(look.scale(range));
        var blockHit = level().clip(
            new net.minecraft.world.level.ClipContext(
                eye,
                end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                this
            )
        );

        if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        var entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
            level(),
            this,
            eye,
            end,
            getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D),
            entity -> entity instanceof LivingEntity living && living.isAlive() && entity != this && !entity.isSpectator()
                && entity != getOwner()
        );

        var at = entityHit != null ? entityHit.getLocation() : end;
        animDispatcher.firing();
        energy -= SHOT_COST;

        if (rockets) {
            level().playSound(
                null,
                blockPosition(),
                net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.BLOCKS,
                1.0F,
                0.8F
            );
            level().explode(
                this,
                damageSources().explosion(this, this),
                null,
                at.x,
                at.y,
                at.z,
                ROCKET_POWER,
                false,
                Level.ExplosionInteraction.NONE
            );
            fireCooldown = ROCKET_INTERVAL;
            return;
        }

        level().playSound(null, blockPosition(), HumanSoundEvents.WEAPON_GENERIC_SHOOT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        fireCooldown = MAX_TURRET_FIRE_COOLDOWN_IN_TICKS;

        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
            target.hurt(damageSources().source(HumanDamageTypeKeys.BULLET, this), caseless ? DAMAGE * CASELESS_MULTIPLIER : DAMAGE);
            target.invulnerableTime = 0;
        }
    }

    public boolean isOverheated() {
        return overheat > 0;
    }

    /** 0..1 - how close to overheating, or how far through the cool-down while overheated. */
    public float getHeatFraction() {
        return overheat > 0 ? overheat / (float) COOLDOWN_TICKS : heat / (float) Math.max(1, heatLimit);
    }

    /**
     * What this turret is waiting on, as lines to show the player, or EMPTY when it is ready to fire.
     * <p>
     * Every condition is reported, not just the first: a turret dropped in a bare field needs power AND ammo, and being
     * told about one at a time means two trips.
     */
    public List<Component> getStatusReport() {
        var report = new ArrayList<Component>();

        if (!hasShotPower()) {
            report.add(Component.translatable("tooltip.avp_human.sentry.needs_power"));
        }

        if (!SentryLoadout.hasAmmo(loadout, ammoType())) {
            report.add(Component.translatable("tooltip.avp_human.sentry.needs_ammo", ammoType().getDescription()));
        }

        if (overheat > 0) {
            report.add(Component.translatable("tooltip.avp_human.sentry.overheated"));
        }

        return report;
    }

    /**
     * Tells a player what the turret is waiting on, or that it is ready. Sent as an action-bar line rather than chat:
     * it is a glance-at-it status, not something worth keeping in the log.
     */
    public void sendStatusReport(Player player) {
        var report = getStatusReport();

        if (report.isEmpty()) {
            player.displayClientMessage(Component.translatable(HumanTooltipTranslationKeys.STATUS_READY), true);

            return;
        }

        var message = Component.empty();

        for (var index = 0; index < report.size(); index++) {
            if (index > 0) {
                message.append(Component.literal("  |  "));
            }

            message.append(report.get(index));
        }

        player.displayClientMessage(message, true);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Power
    // ------------------------------------------------------------------------------------------------------------

    public static final int STATUS_NO_POWER = 1;

    public static final int STATUS_NO_CHEST = 2;

    public static final int STATUS_NO_BULLETS = 4;

    public static final int STATUS_OVERHEATED = 8;

    /**
     * What the turret is waiting on, as flags - for its screen. Searches for a chest, so the menu asks once a second.
     */
    public int getStatusFlags() {
        var flags = hasShotPower() ? 0 : STATUS_NO_POWER;

        if (!SentryLoadout.hasAmmo(loadout, ammoType())) {
            flags |= STATUS_NO_BULLETS;
        }

        if (overheat > 0) {
            flags |= STATUS_OVERHEATED;
        }

        return flags;
    }

    public long getEnergy() {
        return energy;
    }

    public void setEnergy(long energy) {
        this.energy = Math.clamp(energy, 0L, CAPACITY);
    }

    public boolean hasShotPower() {
        return energy >= SHOT_COST;
    }

    public net.minecraft.world.SimpleContainer getBattery() {
        return battery;
    }

    public @Nullable BlockPos getFusePanel() {
        return fusePanel;
    }

    public void setFusePanel(@Nullable BlockPos panel) {
        this.fusePanel = panel == null ? null : panel.immutable();
    }

    /** What a bound fuse panel should send this tick: the room left, capped per tick. Nothing when full. */
    public long getPanelRequest() {
        return fusePanel == null ? 0L : Math.min(PANEL_REFILL_PER_TICK, CAPACITY - energy);
    }

    /** A fuse panel's delivery. @return what the turret took */
    public long receivePanelPower(long amount) {
        var taken = Math.max(0L, Math.min(amount, CAPACITY - energy));

        energy += taken;

        return taken;
    }

    /**
     * Unbound: top up from the battery slot. Bound: check now and then that the panel still lists this turret, and let
     * go of it if the panel was broken or dropped it.
     */
    private void tickPower() {
        var level = level();

        if (fusePanel != null) {
            if (
                level.getGameTime() % 40 == 0 && level.isLoaded(fusePanel)
                    && !(level.getBlockEntity(fusePanel) instanceof com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel
                        && panel.isBoundSentry(getUUID()))
            ) {
                fusePanel = null;
            }

            return;
        }

        var cell = battery.getItem(0);

        if (energy < CAPACITY && cell.getItem() instanceof com.human.common.gameplay.item.power.BatteryItem batteryItem) {
            energy += batteryItem.extract(cell, CAPACITY - energy, false);
            // A drained nuclear battery shrank itself to nothing - the slot now holds an empty stack.
            battery.setChanged();
        }
    }

    private void dropBattery() {
        var cell = battery.removeItemNoUpdate(0);

        if (!cell.isEmpty()) {
            spawnAtLocation(cell, 0.5F);
        }
    }

    /**
     * Destroyed or picked up - not merely unloaded - the turret lets go of its fuse panel, or the panel would keep it
     * in its list, holding a device slot, forever. (The panel cannot prune missing turrets itself: a loaded chunk's
     * entities can load a moment after the chunk, so "not found" does not mean "gone".)
     */
    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide) {
            stopControl();

            // Destroyed or picked up - off the network. (Merely unloaded keeps its entry, so the terminal still lists
            // it.)
            if (reason.shouldDestroy() && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                SentryNetwork.get(serverLevel.getServer()).remove(getUUID());
            }
        }

        if (
            reason.shouldDestroy() && fusePanel != null && level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && serverLevel.isLoaded(fusePanel)
                && serverLevel.getBlockEntity(fusePanel) instanceof com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel
        ) {
            panel.unbindSentry(getUUID());
        }

        super.remove(reason);
    }

    @Override
    public void die(@NotNull DamageSource source) {
        if (!level().isClientSide) {
            dropBattery();
            dropLoadout();
        }

        super.die(source);
    }

    @Override
    public @Nullable net.minecraft.world.inventory.AbstractContainerMenu createMenu(
        int containerId,
        @NotNull net.minecraft.world.entity.player.Inventory inventory,
        @NotNull Player player
    ) {
        return new com.human.common.gameplay.menu.sentry.SentryTurretMenu(containerId, inventory, this);
    }

    private void targetAndFire() {
        var target = getTarget();

        if (target != null && (!target.isAlive() || target.isRemoved())) {
            setTarget(null);
            this.fireCooldown = 0;
            animDispatcher.idle();
            return;
        }

        if (getTarget() == null && tickCount % TARGET_SEARCH_INTERVAL == 0) {
            findTarget();
        }

        if (getTarget() != null) {
            fireAtTarget();
        }
    }

    /**
     * ⚡ The nearest valid target; the line-of-sight check (a raycast) runs last, only for what passed the cheap tests.
     */
    private void findTarget() {
        var candidates = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range), this::canTarget);
        LivingEntity best = null;
        var bestDistance = Double.MAX_VALUE;

        for (var candidate : candidates) {
            var distance = distanceToSqr(candidate);

            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }

        if (best != null) {
            setTarget(best);
        }
    }

    private void fireAtTarget() {
        var target = getTarget();

        if (target == null || !canTarget(target)) {
            setTarget(null);
            return;
        }

        if (!SentryLoadout.consumeAmmo(loadout, ammoType())) {
            setTarget(null);
            return;
        }

        animDispatcher.firing();
        energy -= SHOT_COST;

        if (rockets) {
            // [stated] Oct 2: a rocket every 50 ticks. It breaks no blocks, and the turret is immune to its own blast.
            level().playSound(
                null,
                blockPosition(),
                net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.BLOCKS,
                1.0F,
                0.8F
            );
            level().explode(
                this,
                damageSources().explosion(this, this),
                null,
                target.getX(),
                target.getY(0.5),
                target.getZ(),
                ROCKET_POWER,
                false,
                Level.ExplosionInteraction.NONE
            );
            fireCooldown = ROCKET_INTERVAL;
        } else {
            level().playSound(null, blockPosition(), HumanSoundEvents.WEAPON_GENERIC_SHOOT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            target.hurt(damageSources().source(HumanDamageTypeKeys.BULLET, this), caseless ? DAMAGE * CASELESS_MULTIPLIER : DAMAGE);
            target.invulnerableTime = 0;
            fireCooldown = MAX_TURRET_FIRE_COOLDOWN_IN_TICKS;
        }

        target.setLastHurtMob(this);
    }

    /** Something the turret may shoot right now: an enemy, in range, in its cone, seen. */
    private boolean canTarget(LivingEntity entity) {
        return entity.isAlive()
            && isEnemy(entity)
            && distanceTo(entity) <= range
            && isFacingTarget(entity)
            && getSensing().hasLineOfSight(entity);
    }

    /**
     * Whether the filter says to shoot it - never the owner, their allies or tamed animals, another turret, or (without
     * the EM lens) anything invisible, which is how a cloaked predator stays hidden.
     */
    private boolean isEnemy(LivingEntity entity) {
        // ⚠ Oct 5 - the cloak is not vanilla invisibility (isInvisible stays false), so this check alone never hid a
        // yautja. [stated] cloaking hides it from turrets "unless they have the em lens". The concealment test is the
        // same zero-visibility rule the marines use.
        if (
            entity == this
                || entity instanceof SentryTurret
                || !emLens && (entity.isInvisible() || com.human.common.gameplay.entity.living.human.marine.ai.MarineGOAP.isConcealedFrom(
                    this,
                    entity
                ))
        ) {
            return false;
        }

        var owner = getOwner();

        if (owner != null) {
            if (entity == owner || entity.isAlliedTo(owner)) {
                return false;
            }

            if (entity instanceof net.minecraft.world.entity.OwnableEntity ownable && owner.getUUID().equals(ownable.getOwnerUUID())) {
                return false;
            }
        }

        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        return targetFilter.shouldEngage(entity);
    }

    /** Inside the field of fire: 90 degrees, 180 with the wide-cone card. */
    private boolean isFacingTarget(LivingEntity target) {
        var turretCenter = Vec3.atCenterOf(blockPosition());
        var directionToEntity = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(turretCenter).normalize();

        return directionToEntity.dot(getLookAngle().normalize()) > coneCosine;
    }
}
