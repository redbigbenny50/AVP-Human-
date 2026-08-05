package com.human.common.gameplay.item;

import com.blib.api.common.dismemberment.v1.hitbox.LimbHitPrediction;
import com.blib.api.common.tooltip.v1.TooltipUtil;
import com.human.common.gameplay.item.gun.GunConfig;
import com.human.common.gameplay.item.gun.animation.GunAnimationEvents;
import com.human.common.gameplay.item.gun.pipeline.GunShootContext;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.item.HumanGunItems;
import com.human.compatibility.HumanCommonItemTags;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GunItem extends Item {

    protected static final int START_TICK_PROGRESS = Integer.MAX_VALUE;

    private final GunConfig gunConfig;

    public GunItem(GunConfig gunConfig) {
        super(
            new Item.Properties().stacksTo(1)
                .component(HumanDataComponents.IS_FIRING.get(), false)
                .component(HumanDataComponents.MUZZLE_FLASH_DURATION_IN_TICKS.get(), 0)
                .component(HumanDataComponents.GUN_ANIMATION_ID.get(), 0)
                .component(HumanDataComponents.GUN_ANIMATION_TYPE.get(), GunAnimationEvents.NONE)
                .durability(gunConfig.durability())
                .attributes(createAttributes())
        );
        this.gunConfig = gunConfig;
    }

    private static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_ID, (float) -0.1, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .build();
    }

    protected void playUseAnimations(Entity shooter, ItemStack itemStack) {
        GunAnimationEvents.trigger(itemStack, GunAnimationEvents.SHOOT);
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, ItemStack repairIngredient) {
        return repairIngredient.is(HumanCommonItemTags.INGOTS_STEEL);
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public void releaseUsing(@NotNull ItemStack itemStack, @NotNull Level level, @NotNull LivingEntity livingEntity, int i) {
        var wasFiring = itemStack.getOrDefault(HumanDataComponents.IS_FIRING.get(), false);
        var fireModeConfig = gunConfig.getDefaultFireMode();
        var shootFinishSoundEvent = fireModeConfig.shootFinishSoundEvent();

        if (wasFiring && shootFinishSoundEvent != null) {
            level.playSound(null, livingEntity.blockPosition(), shootFinishSoundEvent.get(), SoundSource.PLAYERS);
        }

        itemStack.set(HumanDataComponents.IS_FIRING.get(), false);

        super.releaseUsing(itemStack, level, livingEntity, i);
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity livingEntity, @NotNull ItemStack itemStack, int tickCountdown) {
        if (livingEntity instanceof Player) {
            return;
        }

        // Lack of server/client side check here is deliberate.

        var tickProgress = Math.abs(START_TICK_PROGRESS - tickCountdown);

        fire(level, livingEntity, itemStack, tickProgress);
    }

    public void fire(@NotNull Level level, @NotNull LivingEntity livingEntity, @NotNull ItemStack itemStack, int tickProgress) {
        fire(level, livingEntity, itemStack, tickProgress, null);
    }

    public void fire(
        @NotNull Level level,
        @NotNull LivingEntity livingEntity,
        @NotNull ItemStack itemStack,
        int tickProgress,
        LimbHitPrediction prediction
    ) {
        GunShootContext.create(livingEntity, itemStack, tickProgress, prediction)
            .map(GunShootContext::shoot)
            .ifSome(result -> {
                switch (result) {
                    // No side effects to run for these results at the time of writing.
                    case COOLDOWN, DELAYED, FAILURE, RELOADING -> { /* NO-OP */ }
                    case SHOT -> {
                        playUseAnimations(livingEntity, itemStack);

                        itemStack.set(HumanDataComponents.IS_FIRING.get(), true);
                        itemStack.set(HumanDataComponents.MUZZLE_FLASH_DURATION_IN_TICKS.get(), 5);
                    }
                }
            });
    }

    @Override
    public int getUseDuration(@NotNull ItemStack itemStack, @NotNull LivingEntity livingEntity) {
        return START_TICK_PROGRESS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level level,
        @NotNull Player player,
        @NotNull InteractionHand interactionHand
    ) {
        return ItemUtils.startUsingInstantly(level, player, interactionHand);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack itemStack, @NotNull Level level, @NotNull Entity entity, int i, boolean bl) {
        removeBlockedEnchantments(itemStack);

        var muzzleFlashDurationInTicks = itemStack.getOrDefault(HumanDataComponents.MUZZLE_FLASH_DURATION_IN_TICKS.get(), 0);
        var newMuzzleFlashDurationInTicks = Math.max(muzzleFlashDurationInTicks - 1, 0);

        itemStack.set(HumanDataComponents.MUZZLE_FLASH_DURATION_IN_TICKS.get(), newMuzzleFlashDurationInTicks);

        if (newMuzzleFlashDurationInTicks == 0) {
            itemStack.set(HumanDataComponents.IS_FIRING.get(), false);
        }

        super.inventoryTick(itemStack, level, entity, i, bl);
    }

    private static void removeBlockedEnchantments(ItemStack itemStack) {
        var enchantments = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        if (enchantments.isEmpty()) {
            return;
        }

        var mutableEnchantments = new ItemEnchantments.Mutable(enchantments);
        mutableEnchantments.removeIf(enchantment -> enchantment.is(Enchantments.INFINITY) || enchantment.is(Enchantments.MENDING));
        var sanitizedEnchantments = mutableEnchantments.toImmutable();

        if (!sanitizedEnchantments.equals(enchantments)) {
            itemStack.set(DataComponents.ENCHANTMENTS, sanitizedEnchantments);
        }
    }

    public GunConfig getGunConfig() {
        return gunConfig;
    }

    public static boolean isBlockedGunEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.INFINITY) || enchantment.is(Enchantments.MENDING);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack itemStack,
        @NotNull TooltipContext tooltipContext,
        @NotNull List<Component> list,
        @NotNull TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(itemStack, tooltipContext, list, tooltipFlag);

        int currentAmmunition = itemStack.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0);
        var fireMode = gunConfig.getDefaultFireMode();
        var itemSupplier = gunConfig.ammunitionItemSupplier();

        // TODO:
        // TooltipUtils.appendLabel(
        // list,
        // "tooltip.avp.fire_mode",
        // Component.literal(fireMode.identifier() + " (" + fireMode.ammunitionData().consumedAmmunition() + " / Shot)")
        // );

        if (itemSupplier != null) {
            var item = itemSupplier.get();

            TooltipUtil.appendLabel(
                list,
                "tooltip.avp.ammunition_type",
                Component.translatable(item.asItem().getDescriptionId())
            );
        }

        // TODO: Don't hardcode old painless here.
        if (this != HumanGunItems.OLD_PAINLESS.get()) {
            TooltipUtil.appendLabel(
                list,
                "tooltip.avp.ammunition",
                Component.literal(currentAmmunition + " / " + gunConfig.maximumAmmunition())
            );
        }

        TooltipUtil.appendLabel(
            list,
            "tooltip.avp.damage",
            Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(fireMode.damage()))
        );
        TooltipUtil.appendLabel(
            list,
            "tooltip.avp.knockback",
            Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(fireMode.knockback()))
        );
        TooltipUtil.appendLabel(
            list,
            "tooltip.avp.fire_rate",
            Component.literal(
                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(fireMode.cooldownInTicks() / 20D) + " / Sec"
            )
        );
    }
}
