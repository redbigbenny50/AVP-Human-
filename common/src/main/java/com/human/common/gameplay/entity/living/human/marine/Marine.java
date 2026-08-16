package com.human.common.gameplay.entity.living.human.marine;

import com.blib.api.common.codec.v1.BLibCodecs;
import com.blib.api.common.entity.v1.BiomeSenseCache;
import com.blib.api.common.entity.v1.EntitySenseCache;
import com.blib.api.common.entity.v1.EntitySenseCacheUser;
import com.blib.api.common.entity.v1.PlayerStatConstants;
import com.blib.api.common.goap.v1.GOAPUser;
import com.blib.api.common.inventory.v1.BLibInventory;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.blib.api.common.item.v1.ItemUtil;
import com.human.Human;
import com.human.common.data.HumanAdvancements;
import com.human.common.gameplay.entity.living.human.AbstractHuman;
import com.human.common.gameplay.entity.living.human.marine.ai.MarineGOAP;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.strategy.FRIStrategySet;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.WeaponStrategySet;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.strategy.ArmorStrategySet;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategySet;
import com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter;
import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.ItemCooldownUser;
import com.human.common.gameplay.level.patrol.decorator.gear.MarineGearDecorator;
import com.human.common.gameplay.menu.marine.MarineInventoryContainer;
import com.human.common.gameplay.menu.marine.MarineInventoryMenu;
import com.human.common.network.packet.S2CMarineSentryFilterPayload;
import com.human.common.registry.init.HumanDataComponents;
import com.just.ai.goap.Agent;
import com.just.ai.goap.graph.Graph;
import com.just.codec.impl.Codecs;
import com.just.core.functional.option.Option;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class Marine extends AbstractHuman implements BLibInventoryHolder, EntitySenseCacheUser, GOAPUser<Marine>, ItemCooldownUser {

    private static final String NBT_INVENTORY = "inventory";

    private static final String NBT_LEADER_UUID = "leaderUUID";

    private static final String NBT_RESUPPLY_TICK = "resupplyTick";

    private static final String NBT_MODE = "mode";

    private static final String NBT_SENTRY_POS = "sentryPos";

    private static final String NBT_SENTRY_FILTER = "sentryFilter";

    public static AttributeSupplier.Builder createMarineAttributes() {
        var builder = Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE);

        builder.add(Attributes.ARMOR, 2.0F);
        builder.add(Attributes.ARMOR_TOUGHNESS, 0f);
        builder.add(Attributes.ATTACK_DAMAGE, PlayerStatConstants.BASE_HEALTH * 0.1F);
        builder.add(Attributes.FOLLOW_RANGE, 20F);
        builder.add(Attributes.KNOCKBACK_RESISTANCE, 0f);
        builder.add(Attributes.MAX_HEALTH, PlayerStatConstants.BASE_HEALTH);
        builder.add(Attributes.MOVEMENT_SPEED, PlayerStatConstants.BASE_SPRINT_JUMP_SPEED);

        return builder;
    }

    private final MarineAnimationDispatcher animationDispatcher;

    private final BiomeSenseCache biomeSenseCache;

    private final EntitySenseCache entitySenseCache;

    private final BLibInventory inventory;

    private final ItemCooldowns itemCooldowns;

    private Option<UUID> leaderUUIDOption;

    private MarineMode mode;

    /**
     * How many players currently have this marine's inventory screen open. Not persisted: a marine that was being
     * rummaged through when the world unloaded is nobody's business by the time it loads again.
     */
    private int containerOpenCount;

    private final MarineResupply resupply = new MarineResupply();

    /**
     * Where a sentry was posted. Only meaningful while the mode is SENTRY, and kept rather than cleared when the mode
     * changes so that toggling sentry off and on again returns the marine to the same post.
     */
    private Option<BlockPos> sentryPosOption = Option.none();

    private SentryTargetFilter sentryFilter = new SentryTargetFilter();

    public Marine(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.animationDispatcher = new MarineAnimationDispatcher(this);
        // Replaces the vanilla control so a fighting withdrawal can strafe at a chosen speed instead of the
        // hardcoded quarter speed. Behaves identically for every other kind of movement.
        this.moveControl = new MarineMoveControl(this);
        this.biomeSenseCache = BiomeSenseCache.builder(this)
            .withScanRadius(4)
            .withRefreshPolicy(cache -> cache.getEntity().tickCount - cache.getLastSenseTick() > 40)
            .build();
        this.entitySenseCache = EntitySenseCache.builder(this)
            .withScanRadius(40)
            .withRefreshPolicy(cache -> cache.getEntity().tickCount - cache.getLastSenseTick() > 40)
            .build();
        // Sized from the menu constant so the container and the screen can never disagree about the pack size.
        this.inventory = new BLibInventory(MarineInventoryMenu.MARINE_SLOT_COUNT);
        this.itemCooldowns = new ItemCooldowns();
        this.leaderUUIDOption = Option.none();
        this.mode = MarineMode.FOLLOW;
    }

    @Override
    public Agent.Builder<Marine> blib$applyGOAPAgentProperties(Agent.Builder<Marine> agentBuilder) {
        return MarineGOAP.applyAgentProperties(agentBuilder);
    }

    @Override
    public @Nullable Graph<Marine> blib$getGOAPGraphOrNull() {
        return MarineGOAP.GRAPH;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            // Re-asserted every tick from the open count rather than toggled on open and close. The count is then
            // the only state that matters, and no dropped connection, crash or missed close callback can strand a
            // marine with its brain switched off.
            var agent = blib$getGOAPAgentOrNull();

            if (agent != null) {
                agent.setEnabled(containerOpenCount <= 0);
            }

            if (containerOpenCount > 0) {
                getNavigation().stop();
            }
        }

        super.tick();

        if (!level().isClientSide) {
            reassertAimAfterMovement();

            itemCooldowns.tick();
            resupply.tick(this);

            // Tick items in hand. This prevents gun muzzle flashes from getting stuck when held by marines.
            var mainHandItemStack = getMainHandItem();
            mainHandItemStack.inventoryTick(level(), this, EquipmentSlot.MAINHAND.getIndex(), true);
            var offHandItemStack = getOffhandItem();
            offHandItemStack.inventoryTick(level(), this, EquipmentSlot.OFFHAND.getIndex(), true);
        }
    }

    /**
     * Puts the marine back on its target after the movement code has finished turning it away.
     * <p>
     * ⚠⚠ THE AIM IS SET EARLY IN THE TICK AND OVERWRITTEN LATE. The combat strategy calls {@code lookAt} while the GOAP
     * agent runs, at the very top of the tick - but {@code MoveControl}'s MOVE_TO branch, which runs afterwards inside
     * {@code serverAiStep}, does {@code setYRot(rotlerp(getYRot(), waypointYaw, 90))} every single tick. It turns the
     * marine to face wherever it is WALKING, by up to ninety degrees at a time, and it always gets the last word.
     * <p>
     * A marine that shoots while walking therefore ends up facing its path, not its enemy - and if the path leads away
     * from what it is shooting at, it faces the exact opposite direction. Both the held weapon and the shot follow
     * {@code yRot}, so the gun points backwards AND the bullets go backwards with it.
     * <p>
     * Running here is the whole fix: {@code super.tick()} has already returned, so the movement code has had its turn
     * and this is the last word instead. Only while actually engaging, so walking, idling and patrolling still turn the
     * marine to face where it is going.
     */
    private void reassertAimAfterMovement() {
        if (!isAggressive()) {
            return;
        }

        var target = getTarget();

        if (target == null || !target.isAlive()) {
            return;
        }

        lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

        // The arms are posed from the head-to-body difference, so squaring the body is what actually puts the rifle on
        // the target rather than out to one side.
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    /**
     * Keeps a hired marine in the world.
     * <p>
     * Without this vanilla despawning DELETES them - beyond 128 blocks, and also through the idle-too-long roll at
     * closer range - and a fresh unhired marine spawns later, which reads as the contract having been forgotten. The
     * contract itself was always saved correctly; the marine holding it simply stopped existing.
     * <p>
     * Read from the leader rather than latched with {@code setPersistenceRequired()}, which is deliberately one-way in
     * vanilla and has no matching clear. Answering the question from current state means ending a contract releases the
     * marine to ordinary despawning again with nothing to reset - so a player who hires and dismisses dozens over a
     * playthrough is not quietly filling their world with permanent ones.
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !hasLeader();
    }

    @Override
    public int getMaxFallDistance() {
        return inventory.hasItem(Items.WATER_BUCKET)
            ? 1024
            : super.getMaxFallDistance();
    }

    @Override
    public void runAttackAnimations() {
        animationDispatcher.rightShoot();
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        Arrays.stream(inventory.getSerializedItemStacks())
            .filter(itemStack -> !itemStack.has(HumanDataComponents.MARINE_OWNED.get()))
            .map(itemStack -> ItemUtil.drop(this, itemStack, true, false))
            .flatMap(Option::toStream)
            .forEach(itemEntity -> level().addFreshEntity(itemEntity));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        @NotNull ServerLevelAccessor level,
        @NotNull DifficultyInstance difficulty,
        @NotNull MobSpawnType spawnType,
        @Nullable SpawnGroupData spawnGroupData
    ) {
        switch (spawnType) {
            case BUCKET,
                COMMAND,
                DISPENSER,
                MOB_SUMMONED,
                NATURAL,
                SPAWN_EGG,
                SPAWNER,
                TRIGGERED,
                TRIAL_SPAWNER -> MarineGearDecorator.INSTANCE.decorate(level(), this);
            case BREEDING,
                CHUNK_GENERATION,
                CONVERSION,
                EVENT,
                JOCKEY,
                PATROL,
                REINFORCEMENT,
                STRUCTURE -> { /* NO-OP */ }
        }

        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand interactionHand) {
        var itemStack = player.getItemInHand(interactionHand);

        // ⚠⚠ TEMPORARY DIAGNOSTIC — [stated] the inventory GUI does not open on right-click, hired or not, and the
        // server log shows NO exception, so the interaction path is simply not reaching openInventoryMenu. Everything
        // upstream reads correct: nothing overrides mobInteract, the only Entity#interact mixin fires solely for the
        // syringe and gene reader, the menu type and screen are both registered, and the slot counts match.
        // ⭐ READ IT AS: if NO line appears at all, mobInteract is never called and the problem is upstream of this
        // class. If it appears with leaderMatches=false, the leader is not being held (or not the player clicking).
        // If leaderMatches=true and empty=true but no "opening menu" line follows, openInventoryMenu itself is the
        // fault. ⏭ DELETE THIS BLOCK AND THE ONE IN openInventoryMenu once the report is closed.
        Human.LOGGER.info(
            "[marinegui] interact side={} player={} leader={} leaderMatches={} hand={} item={} empty={}",
            level().isClientSide ? "CLIENT" : "SERVER",
            player.getUUID(),
            getLeaderUUID().isSome() ? getLeaderUUID().unwrap() : "none",
            getLeaderUUID().isSomeAnd(uuid -> Objects.equals(uuid, player.getUUID())),
            interactionHand,
            itemStack.getItem(),
            itemStack.isEmpty()
        );

        if (itemStack.getItem() == Items.DIAMOND && !hasLeader()) {
            itemStack.consume(1, player);
            setLeader(player);

            if (!level().isClientSide) {
                HumanAdvancements.HIRE_MARINE.grant((ServerPlayer) player);
            }

            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Only leaders can give marines items.
        if (getLeaderUUID().isSomeAnd(uuid -> Objects.equals(uuid, player.getUUID()))) {
            if (
                itemStack.getItem() == Items.BONE
                    || itemStack.getItem() == Items.TORCH
                    || itemStack.getItem() == Items.TOTEM_OF_UNDYING
                    || itemStack.getItem() == Items.WATER_BUCKET
                    || ArmorStrategySet.INSTANCE.isAnyValidFor(itemStack)
                    || FRIStrategySet.INSTANCE.isAnyValidFor(itemStack)
                    || HealingStrategySet.INSTANCE.isAnyValidFor(itemStack)
                    || WeaponStrategySet.INSTANCE.isAnyValidFor(itemStack)
            ) {
                if (!level().isClientSide) {
                    var item = new ItemStack(itemStack.getItem(), 1);
                    item.applyComponents(itemStack.getComponents());
                    itemStack.consume(1, player);
                    inventory.addItemStack(item);
                }

                return InteractionResult.sidedSuccess(level().isClientSide);
            }

            if (itemStack.isEmpty()) {
                // Used to toggle follow/hold. That command now lives in the screen alongside room for more, which
                // frees the empty hand for the gesture players already expect to open a container.
                if (player instanceof ServerPlayer serverPlayer) {
                    openInventoryMenu(serverPlayer);
                }

                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }

        if (itemStack.getItem() instanceof GunItem) {
            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, interactionHand);
    }

    @Override
    public BLibInventory getInventory() {
        return inventory;
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);

        if (compoundTag.contains(NBT_INVENTORY)) {
            BLibInventory.CODEC.decode(BLibCodecs.Schema.NBT, compoundTag.get(NBT_INVENTORY))
                .inspectErr(tag -> Human.LOGGER.error("Failed to load tag '{}'. Tag: {}", NBT_INVENTORY, tag))
                .ifOk(loadedInventory -> Arrays.stream(loadedInventory.getSerializedItemStacks()).forEach(inventory::addItemStack));
        }

        // ⚠ The mode was never persisted before sentry existed, so a marine on HOLD quietly went back to FOLLOW every
        // time the world reloaded. MarineMode.CODEC had been written for this and never wired up.
        if (compoundTag.contains(NBT_MODE)) {
            MarineMode.CODEC.decode(BLibCodecs.Schema.NBT, compoundTag.get(NBT_MODE))
                .inspectErr(tag -> Human.LOGGER.error("Failed to load tag '{}'. Tag: {}", NBT_MODE, tag))
                .ifOk(loadedMode -> this.mode = loadedMode);
        }

        if (compoundTag.contains(NBT_SENTRY_FILTER)) {
            this.sentryFilter = SentryTargetFilter.load(compoundTag.getCompound(NBT_SENTRY_FILTER));
        }

        if (compoundTag.contains(NBT_SENTRY_POS)) {
            this.sentryPosOption = Option.some(NbtUtils.readBlockPos(compoundTag, NBT_SENTRY_POS).orElse(blockPosition()));
        }

        if (compoundTag.contains(NBT_RESUPPLY_TICK)) {
            resupply.setResupplyTick(tickCount + compoundTag.getInt(NBT_RESUPPLY_TICK));
        }

        if (compoundTag.contains(NBT_LEADER_UUID)) {
            Codecs.UUID.decode(BLibCodecs.Schema.NBT, compoundTag.get(NBT_LEADER_UUID))
                .ifOk(uuid -> this.leaderUUIDOption = Option.ofNullable(uuid));
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.put(NBT_INVENTORY, BLibInventory.CODEC.encode(BLibCodecs.Schema.NBT, inventory));
        compoundTag.put(NBT_MODE, MarineMode.CODEC.encode(BLibCodecs.Schema.NBT, mode));
        sentryPosOption.ifSome(sentryPos -> compoundTag.put(NBT_SENTRY_POS, NbtUtils.writeBlockPos(sentryPos)));
        compoundTag.put(NBT_SENTRY_FILTER, sentryFilter.save());

        // Stored as ticks REMAINING rather than the absolute tick it lands on, because tickCount restarts at zero
        // when the entity is reloaded - an absolute deadline would already be in the past and resupply instantly.
        if (resupply.getResupplyTick() != MarineResupply.NO_RESUPPLY_PENDING) {
            compoundTag.putInt(NBT_RESUPPLY_TICK, Math.max(0, resupply.getResupplyTick() - tickCount));
        }
        leaderUUIDOption.ifSome(leaderUUID -> compoundTag.put(NBT_LEADER_UUID, Codecs.UUID.encode(BLibCodecs.Schema.NBT, leaderUUID)));
    }

    public BiomeSenseCache getBiomeSenseCache() {
        return biomeSenseCache;
    }

    @Override
    public EntitySenseCache getEntitySenseCache() {
        return entitySenseCache;
    }

    @Override
    public ItemCooldowns getItemCooldowns() {
        return itemCooldowns;
    }

    public Option<UUID> getLeaderUUID() {
        return leaderUUIDOption;
    }

    public void openInventoryMenu(ServerPlayer serverPlayer) {
        // ⚠ TEMPORARY DIAGNOSTIC — see mobInteract. If this line prints but no screen appears, the failure is in the
        // menu/screen pair or the payload, not in the interaction gate.
        Human.LOGGER.info("[marinegui] opening menu for {}", serverPlayer.getGameProfile().getName());
        setContainerOpen(true);
        // Pushed as the screen opens so the sentry page shows what is actually set. The filter is too big for a
        // DataSlot - those go over the wire as shorts - so it travels as its own payload.
        Human.MOD.networking().sendToClient(serverPlayer, new S2CMarineSentryFilterPayload(sentryFilter.copy()));
        serverPlayer.openMenu(
            new SimpleMenuProvider(
                (containerId, playerInventory, $1) -> new MarineInventoryMenu(
                    containerId,
                    playerInventory,
                    new MarineInventoryContainer(this),
                    this
                ),
                getDisplayName()
            )
        );
    }

    public void setContainerOpen(boolean open) {
        containerOpenCount = open
            ? containerOpenCount + 1
            : Math.max(0, containerOpenCount - 1);
    }

    public boolean isContainerOpen() {
        return containerOpenCount > 0;
    }

    public boolean isLeader(Player player) {
        return getLeaderUUID().isSomeAnd(uuid -> Objects.equals(uuid, player.getUUID()));
    }

    public void setMode(MarineMode mode) {
        this.mode = mode;
    }

    public SentryTargetFilter getSentryFilter() {
        return sentryFilter;
    }

    public void setSentryFilter(SentryTargetFilter sentryFilter) {
        this.sentryFilter = sentryFilter;
    }

    public boolean isSentry() {
        return mode == MarineMode.SENTRY && sentryPosOption.isSome();
    }

    public Option<BlockPos> getSentryPos() {
        return sentryPosOption;
    }

    /**
     * Posts the marine where it currently stands. Called when sentry is switched on, so the player positions a sentry
     * by walking it there rather than by picking a block.
     */
    public void beginSentry() {
        this.sentryPosOption = Option.some(blockPosition());
        setMode(MarineMode.SENTRY);
    }

    /**
     * Stands the marine down. The post is deliberately KEPT: switching sentry back on without moving should return it
     * to the same spot, and re-posting only happens where it is standing at that moment anyway.
     */
    public void endSentry() {
        setMode(MarineMode.FOLLOW);
    }

    public MarineMode getMode() {
        return mode;
    }

    public Option<Entity> getLeader() {
        // TODO: Switch this from 'andThen' to 'map' once Just fixes null not being a valid return choice.
        return getLeaderUUID().andThen(
            uuid -> Option.ofNullable(level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(uuid) : null)
        );
    }

    public boolean hasLeader() {
        return leaderUUIDOption.isSome();
    }

    public void setLeader(Entity entity) {
        setLeaderUUID(entity.getUUID());
    }

    public void setLeaderUUID(UUID leaderUUID) {
        this.leaderUUIDOption = Option.some(leaderUUID);
    }

    public void removeLeader() {
        this.leaderUUIDOption = Option.none();
    }

    /**
     * Ends the contract: the marine is no longer hired and goes back to being an ordinary patrol spawn.
     * <p>
     * Persistence is released along with the leader, deliberately. A dismissed marine that stayed persistent would
     * linger forever, and a player who hired and dismissed a few dozen over a playthrough would be quietly filling
     * their world with them.
     */
    public void endContract() {
        removeLeader();
        setMode(MarineMode.FOLLOW);
        // Forgotten here, unlike endSentry: a dismissed marine has no reason to remember a post it was set by someone
        // who is no longer its leader.
        this.sentryPosOption = Option.none();
    }

    /**
     * Whether the pack holds anything the PLAYER put there. Issued kit is flagged {@code MARINE_OWNED} and walks off
     * with the marine; anything else is the player's and would be lost with it.
     */
    public boolean carriesPlayerItems() {
        for (var slotIndex = 0; slotIndex < inventory.getSize(); slotIndex++) {
            var itemStack = inventory.getItemStack(slotIndex);

            if (!itemStack.isEmpty() && !itemStack.getOrDefault(HumanDataComponents.MARINE_OWNED.get(), false)) {
                return true;
            }
        }

        return false;
    }
}
