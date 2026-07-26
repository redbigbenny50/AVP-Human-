package com.human.common.gameplay.item.old_painless;

import com.human.common.gameplay.item.ItemCooldownUser;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.item.HumanGunItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server-owned heat and lockout state for Old Painless. */
public final class OldPainlessHeat {

    // The minigun fires once per tick. With one point of passive cooling, this is
    // a net three heat/tick: 420 heat reaches overheat in seven seconds.
    public static final int MAX_HEAT = 420;

    private static final int HEAT_PER_SHOT = 4;

    private static final int PASSIVE_COOLING_PER_TICK = 1;

    private static final int OVERHEATED_COOLING_PER_TICK = 4;

    private static final int OVERHEAT_LOCKOUT_TICKS = 100;

    public static boolean isOverheated(ItemStack stack) {
        return stack.getOrDefault(HumanDataComponents.OLD_PAINLESS_OVERHEATED.get(), false);
    }

    public static int getHeat(ItemStack stack) {
        return stack.getOrDefault(HumanDataComponents.OLD_PAINLESS_HEAT.get(), 0);
    }

    public static void addShotHeat(LivingEntity shooter, ItemStack stack) {
        var heat = Math.min(MAX_HEAT, getHeat(stack) + HEAT_PER_SHOT);
        stack.set(HumanDataComponents.OLD_PAINLESS_HEAT.get(), heat);

        if (heat < MAX_HEAT || isOverheated(stack)) {
            return;
        }

        stack.set(HumanDataComponents.OLD_PAINLESS_OVERHEATED.get(), true);
        var cooldowns = ItemCooldownUser.getItemCooldownsOrNull(shooter);
        if (cooldowns != null) {
            cooldowns.addCooldown(HumanGunItems.OLD_PAINLESS.get(), OVERHEAT_LOCKOUT_TICKS);
        }
        if (shooter instanceof Player player) {
            player.displayClientMessage(Component.literal("OLD PAINLESS OVERHEATED - COOLING"), true);
        }
    }

    public static void cool(ItemStack stack) {
        var heat = getHeat(stack);
        if (heat <= 0) {
            stack.remove(HumanDataComponents.OLD_PAINLESS_OVERHEATED.get());
            return;
        }

        var coolingRate = isOverheated(stack) ? OVERHEATED_COOLING_PER_TICK : PASSIVE_COOLING_PER_TICK;
        heat = Math.max(0, heat - coolingRate);
        stack.set(HumanDataComponents.OLD_PAINLESS_HEAT.get(), heat);
        if (heat == 0) {
            stack.remove(HumanDataComponents.OLD_PAINLESS_OVERHEATED.get());
        }
    }

    private OldPainlessHeat() {}
}
