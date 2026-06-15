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
import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.ItemCooldownUser;
import com.human.common.gameplay.level.patrol.decorator.gear.MarineGearDecorator;
import com.human.common.registry.init.HumanDataComponents;
import com.just.ai.goap.Agent;
import com.just.ai.goap.graph.Graph;
import com.just.codec.impl.Codecs;
import com.just.core.functional.option.Option;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

    public Marine(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.animationDispatcher = new MarineAnimationDispatcher(this);
        this.biomeSenseCache = BiomeSenseCache.builder(this)
            .withScanRadius(4)
            .withRefreshPolicy(cache -> cache.getEntity().tickCount - cache.getLastSenseTick() > 40)
            .build();
        this.entitySenseCache = EntitySenseCache.builder(this)
            .withScanRadius(40)
            .withRefreshPolicy(cache -> cache.getEntity().tickCount - cache.getLastSenseTick() > 40)
            .build();
        this.inventory = new BLibInventory(27);
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
        super.tick();

        if (!level().isClientSide) {
            itemCooldowns.tick();

            // Tick items in hand. This prevents gun muzzle flashes from getting stuck when held by marines.
            var mainHandItemStack = getMainHandItem();
            mainHandItemStack.inventoryTick(level(), this, EquipmentSlot.MAINHAND.getIndex(), true);
            var offHandItemStack = getOffhandItem();
            offHandItemStack.inventoryTick(level(), this, EquipmentSlot.OFFHAND.getIndex(), true);
        }
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
                if (!level().isClientSide) {
                    this.mode = mode == MarineMode.FOLLOW
                        ? MarineMode.HOLD
                        : MarineMode.FOLLOW;
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

        if (compoundTag.contains(NBT_LEADER_UUID)) {
            Codecs.UUID.decode(BLibCodecs.Schema.NBT, compoundTag.get(NBT_LEADER_UUID))
                .ifOk(uuid -> this.leaderUUIDOption = Option.ofNullable(uuid));
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.put(NBT_INVENTORY, BLibInventory.CODEC.encode(BLibCodecs.Schema.NBT, inventory));
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
}
