package com.human.common.gameplay.entity.living.human.colonist;

import com.human.common.gameplay.entity.living.human.AbstractHuman;
import com.human.common.gameplay.entity.living.human.HumanFeatureManager;
import com.human.common.gameplay.util.EyeColorGenerator;
import com.human.common.gameplay.util.HairColorGenerator;
import com.human.common.gameplay.util.SkinColorGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The colonist: an ordinary human settler who takes a job from a workstation. [stated] Sep 30: the colonist mob belongs
 * to avp_human; the dropship mod was written before avp_human had one, and its navigator becomes a profession
 * registered here ({@link ColonistProfessions}). Ported from the dropship's colonist, with the navigator-specific parts
 * moved out into the profession API.
 * <h2>THE JOB</h2>
 * <ul>
 * <li>A colonist without a job looks for a free workstation of ANY registered profession within
 * {@value #WORKSTATION_SEARCH_RADIUS} blocks every few seconds, claims it, and puts on that profession's outfit.</li>
 * <li>It walks back when it wanders more than 6 blocks from the station.</li>
 * <li>It loses the job - and the outfit - when the station is broken, or claimed by another colonist.</li>
 * <li>Right-click trades with it through vanilla's merchant screen. It restocks while at its station.</li>
 * </ul>
 * <h2>APPEARANCE</h2> The same tinted skin, eyes, hair and beard as the marines ({@link AbstractHuman}), with the
 * colonist's own model, skin and clothes ({@link ColonistFeatureManager}).
 */
public class ColonistEntity extends AbstractHuman implements Merchant {

    public static final int WORKSTATION_SEARCH_RADIUS = 8;

    private static final double RETURN_DISTANCE_SQR = 36.0D;

    private static final String NBT_PROFESSION = "ColonistProfession";

    private static final String NBT_WORKSTATION = "ColonistWorkstation";

    private static final String NBT_OFFERS = "ColonistOffers";

    private static final String NBT_PROFESSION_DATA = "ColonistProfessionData";

    private static final String NBT_LAST_RESTOCK = "ColonistLastRestock";

    private final ColonistFeatureManager colonistFeatureManager;

    private @Nullable Player tradingPlayer;

    private @Nullable MerchantOffers offers;

    private @Nullable ResourceLocation professionId;

    private @Nullable BlockPos workstationPos;

    private CompoundTag professionData = new CompoundTag();

    private long lastRestock;

    private int workstationScanCooldown;

    private int workstationReturnCooldown;

    public ColonistEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.colonistFeatureManager = new ColonistFeatureManager(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.26D)
            .add(Attributes.FOLLOW_RANGE, 20.0D)
            .add(Attributes.ARMOR, 0.0D)
            .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        @NotNull ServerLevelAccessor level,
        @NotNull DifficultyInstance difficulty,
        @NotNull MobSpawnType spawnType,
        @Nullable SpawnGroupData spawnGroupData
    ) {
        randomizeAppearance();
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    private void randomizeAppearance() {
        var random = getRandom();

        this.isMale.set(random.nextBoolean());

        var male = this.isMale.get();

        if (male) {
            this.setBeardVariant(random.nextInt(3));
        } else {
            this.beardVariant.set(0);
        }

        this.eyeColor.set(EyeColorGenerator.random(random));
        this.hairColor.set(HairColorGenerator.random(random));
        this.hairVariant.set(random.nextInt(male ? 5 : 6));
        this.skinColor.set(SkinColorGenerator.random(random));
        this.resetTicksUntilBored();
    }

    @Override
    public HumanFeatureManager getHumanFeatureManager() {
        return colonistFeatureManager;
    }

    @Override
    public void runAttackAnimations() {
        // Colonists do not fight.
    }

    // ------------------------------------------------------------------------------------------------------------
    // The job
    // ------------------------------------------------------------------------------------------------------------

    public @Nullable ColonistProfession getProfession() {
        return ColonistProfessions.byId(professionId);
    }

    public @Nullable BlockPos getWorkstationPos() {
        return workstationPos;
    }

    /** Per-colonist state a profession keeps (a trade tab, sold counters) - saved with the colonist. */
    public CompoundTag getProfessionData() {
        return professionData;
    }

    /** Throws the offers away so the profession builds them again - e.g. after a trade tab changes. */
    public void rebuildOffers() {
        this.offers = null;
    }

    @Override
    public void tick() {
        super.tick();

        if (level() instanceof ServerLevel serverLevel) {
            if (--workstationScanCooldown <= 0) {
                workstationScanCooldown = 80 + getRandom().nextInt(40);
                updateProfession(serverLevel);
            }

            tickReturnToWorkstation(serverLevel);
        }
    }

    private void updateProfession(ServerLevel level) {
        var profession = getProfession();

        if (profession != null) {
            // The station's chunk unloaded (the colonist wandered to a chunk edge): keep the job until it can be
            // checked,
            // rather than load that chunk - or worse, judge an unloaded station "gone" and strip the job.
            if (workstationPos != null && !level.isLoaded(workstationPos)) {
                return;
            }

            if (
                workstationPos == null || !profession.isWorkstation(level.getBlockState(workstationPos))
                    || isClaimedByAnother(level, workstationPos, this)
            ) {
                clearProfession();
                return;
            }

            tryRestock(level, profession);
            return;
        }

        // An unknown saved profession (its mod removed) is simply no job: look for a new one.
        professionId = null;

        var found = findFreeWorkstation(level);

        if (found != null) {
            var newProfession = ColonistProfessions.forWorkstation(level.getBlockState(found));

            if (newProfession != null) {
                setProfession(newProfession, found);
            }
        }
    }

    private void setProfession(ColonistProfession profession, BlockPos workstation) {
        this.professionId = profession.id();
        this.workstationPos = workstation.immutable();
        this.professionData = new CompoundTag();
        this.offers = null;
        this.lastRestock = level().getGameTime();

        for (var entry : profession.outfit().entrySet()) {
            setItemSlot(entry.getKey(), entry.getValue().copy());
            setDropChance(entry.getKey(), 0.0F);
        }
    }

    private void clearProfession() {
        var profession = getProfession();

        if (profession != null) {
            // Take off only what the job put on - anything else it wears stays.
            for (var entry : profession.outfit().entrySet()) {
                if (ItemStack.isSameItem(getItemBySlot(entry.getKey()), entry.getValue())) {
                    setItemSlot(entry.getKey(), ItemStack.EMPTY);
                }
            }
        }

        // Losing the job mid-trade closes the trade screen (ServerPlayer's closeContainer is the public one).
        if (tradingPlayer instanceof ServerPlayer serverPlayer) {
            serverPlayer.closeContainer();
        }

        setTradingPlayer(null);

        this.professionId = null;
        this.workstationPos = null;
        this.professionData = new CompoundTag();
        this.offers = null;
    }

    private void tryRestock(ServerLevel level, ColonistProfession profession) {
        if (offers == null || workstationPos == null || level.getGameTime() - lastRestock < profession.restockIntervalTicks()) {
            return;
        }

        if (distanceToSqr(workstationPos.getX() + 0.5D, workstationPos.getY() + 0.5D, workstationPos.getZ() + 0.5D) > RETURN_DISTANCE_SQR) {
            return;
        }

        lastRestock = level.getGameTime();

        for (var offer : offers) {
            offer.resetUses();
        }
    }

    private @Nullable BlockPos findFreeWorkstation(ServerLevel level) {
        var center = blockPosition();
        var cursor = new BlockPos.MutableBlockPos();

        for (var y = -2; y <= 2; y++) {
            for (var x = -WORKSTATION_SEARCH_RADIUS; x <= WORKSTATION_SEARCH_RADIUS; x++) {
                for (var z = -WORKSTATION_SEARCH_RADIUS; z <= WORKSTATION_SEARCH_RADIUS; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);

                    // Never loads a chunk: a colonist at a chunk edge only looks at what is loaded.
                    if (
                        level.isLoaded(cursor) && ColonistProfessions.forWorkstation(level.getBlockState(cursor)) != null
                            && !isClaimedByAnother(level, cursor, this)
                    ) {
                        return cursor.immutable();
                    }
                }
            }
        }

        return null;
    }

    private static boolean isClaimedByAnother(ServerLevel level, BlockPos pos, @Nullable ColonistEntity self) {
        for (var colonist : level.getEntitiesOfClass(ColonistEntity.class, new AABB(pos).inflate(32.0D, 8.0D, 32.0D))) {
            if (colonist != self && colonist.isAlive() && pos.equals(colonist.workstationPos) && colonist.professionId != null) {
                return true;
            }
        }

        return false;
    }

    private void tickReturnToWorkstation(ServerLevel level) {
        if (workstationPos == null || getProfession() == null || --workstationReturnCooldown > 0) {
            return;
        }

        workstationReturnCooldown = 60 + getRandom().nextInt(60);

        var distance = distanceToSqr(workstationPos.getX() + 0.5D, workstationPos.getY() + 0.5D, workstationPos.getZ() + 0.5D);

        if (distance > RETURN_DISTANCE_SQR) {
            getNavigation().moveTo(workstationPos.getX() + 0.5D, workstationPos.getY(), workstationPos.getZ() + 0.5D, 0.7D);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Trading
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        var profession = getProfession();

        if (profession == null || !isAlive() || tradingPlayer != null && tradingPlayer != player) {
            return super.mobInteract(player, hand);
        }

        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer) || getOffers().isEmpty()) {
            return InteractionResult.CONSUME;
        }

        // ⚠ From the dropship's colonist, kept as-is: set the trading player AFTER openMenu returns. openMenu closes
        // the
        // player's previous container first, and if that was this colonist's merchant menu, closing it calls
        // setTradingPlayer(null) - which would undo a trading player set beforehand and close the new screen next tick.
        serverPlayer.openMenu(
            new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new MerchantMenu(containerId, inventory, this),
                profession.title()
            )
        ).ifPresent(containerId -> {
            setTradingPlayer(serverPlayer);
            serverPlayer.connection.send(
                new ClientboundMerchantOffersPacket(containerId, getOffers(), 1, getVillagerXp(), showProgressBar(), canRestock())
            );
        });

        return InteractionResult.CONSUME;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Override
    public @Nullable Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public @NotNull MerchantOffers getOffers() {
        if (offers == null) {
            var profession = getProfession();

            offers = profession == null ? new MerchantOffers() : profession.createOffers(this);
        }

        return offers;
    }

    @Override
    public void overrideOffers(@NotNull MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(@NotNull MerchantOffer offer) {
        offer.increaseUses();

        var profession = getProfession();

        if (profession != null) {
            profession.onTrade(this, offer);
        }
    }

    @Override
    public void notifyTradeUpdated(@NotNull ItemStack stack) {}

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {}

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public @NotNull SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide;
    }

    /** Shown in the trade screen: out-of-stock trades say they will come back. */
    @Override
    public boolean canRestock() {
        return getProfession() != null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Save
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        if (professionId != null) {
            compound.putString(NBT_PROFESSION, professionId.toString());
        }

        if (workstationPos != null) {
            compound.putLong(NBT_WORKSTATION, workstationPos.asLong());
        }

        compound.put(NBT_PROFESSION_DATA, professionData.copy());
        compound.putLong(NBT_LAST_RESTOCK, lastRestock);

        if (offers != null) {
            MerchantOffers.CODEC.encodeStart(registryAccess().createSerializationContext(NbtOps.INSTANCE), offers)
                .result()
                .ifPresent(tag -> compound.put(NBT_OFFERS, tag));
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        professionId = compound.contains(NBT_PROFESSION) ? ResourceLocation.tryParse(compound.getString(NBT_PROFESSION)) : null;
        workstationPos = compound.contains(NBT_WORKSTATION) ? BlockPos.of(compound.getLong(NBT_WORKSTATION)) : null;
        professionData = compound.getCompound(NBT_PROFESSION_DATA).copy();
        lastRestock = compound.getLong(NBT_LAST_RESTOCK);
        offers = null;

        if (compound.contains(NBT_OFFERS)) {
            MerchantOffers.CODEC.parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), compound.get(NBT_OFFERS))
                .result()
                .ifPresent(saved -> offers = saved);
        }
    }

    @Override
    public @NotNull Component getTypeName() {
        var profession = getProfession();

        return profession != null ? profession.title() : super.getTypeName();
    }
}
