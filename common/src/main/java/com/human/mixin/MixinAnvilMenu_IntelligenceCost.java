package com.human.mixin;

import com.human.common.gameplay.gene.PlayerIntelligence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * [stated] Oct 30: the Intelligence gene lowers an anvil job's level cost (see PlayerIntelligence). Applied after
 * vanilla works the cost out - so "Too Expensive!" is still vanilla's call - and on the cost the menu shows and
 * charges, so what the player sees is what they pay. Never below 1 (vanilla offers no result at 0).
 */
@Mixin(AnvilMenu.class)
public abstract class MixinAnvilMenu_IntelligenceCost extends ItemCombinerMenu {

    @Shadow
    @Final
    private DataSlot cost;

    protected MixinAnvilMenu_IntelligenceCost(MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(type, containerId, inventory, access);
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void avp_human$intelligenceAnvilCost(CallbackInfo callback) {
        var current = cost.get();
        var cut = PlayerIntelligence.cutAnvilCost(player, current);
        if (cut != current) {
            cost.set(cut);
        }
    }
}
