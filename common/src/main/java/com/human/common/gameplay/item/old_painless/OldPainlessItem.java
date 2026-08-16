package com.human.common.gameplay.item.old_painless;

import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.GunData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class OldPainlessItem extends GunItem {

    public OldPainlessItem() {
        super(GunData.OLD_PAINLESS);
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity livingEntity, @NotNull ItemStack itemStack, int tickCountdown) {
        if (livingEntity instanceof Player) {
            return;
        }

        var tickProgress = Math.abs(START_TICK_PROGRESS - tickCountdown);

        fire(level, livingEntity, itemStack, tickProgress);
    }

    @Override
    protected void playUseAnimations(Entity shooter, ItemStack itemStack) {}

    @Override
    public void inventoryTick(@NotNull ItemStack itemStack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean selected) {
        super.inventoryTick(itemStack, level, entity, slotId, selected);
        if (!level.isClientSide) {
            OldPainlessHeat.cool(itemStack);
        }
    }

}
