package com.human.mixin;

import com.human.common.registry.init.HumanVillagerProfessions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;

/**
 * [stated] Oct 13: the commissary offers EVERY trade of each level it reaches, not vanilla's two at random. Its master
 * level holds the sentry turret (which has no recipe) and the USCM faction license, and neither may depend on luck.
 * <p>
 * Only the commissary changes; every other villager keeps vanilla's two. A commissary already in a world gets the
 * trades it is missing for the levels it has reached when it loads - its saved trades, and how often they have been
 * used, are left alone.
 * <p>
 * [stated] Oct 20 FIX - the trade list filled up with sentry turrets. The sentry turret is an AzureLib identity item:
 * BLib stamps a RANDOM {@code AZ_ID} on every new turret stack. The "is this trade already there?" check compared the
 * whole stack including components, so the saved turret (one id) never matched a freshly made one (another id), and
 * every time the commissary loaded from disk it added ANOTHER turret trade. The check now compares ITEMS only (what is
 * sold and what it costs), which is all that identifies a trade here, and a load also removes duplicates a commissary
 * already collected - the first copy, with its use count, is the one kept.
 */
@Mixin(Villager.class)
public abstract class MixinVillager_CommissaryTrades extends AbstractVillager {

    @Shadow
    public abstract VillagerData getVillagerData();

    protected MixinVillager_CommissaryTrades(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    /** Vanilla asks for 2 offers per level; the picker stops when a level runs out, so this gives them all. */
    @ModifyArg(
        method = "updateTrades",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/npc/Villager;addOffersFromItemListings(Lnet/minecraft/world/item/trading/MerchantOffers;[Lnet/minecraft/world/entity/npc/VillagerTrades$ItemListing;I)V"
        ),
        index = 2
    )
    private int avp_human$everyCommissaryTrade(int count) {
        return avp_human$isCommissary() ? Integer.MAX_VALUE : count;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void avp_human$addMissingCommissaryTrades(CompoundTag compound, CallbackInfo ci) {
        if (offers == null || level().isClientSide || !avp_human$isCommissary()) {
            return;
        }

        // Repair first: drop the duplicate trades the old check let pile up (keeps the first, which holds the uses).
        var seen = new HashSet<List<Object>>();
        offers.removeIf(saved -> !seen.add(avp_human$key(saved)));

        var data = getVillagerData();
        var byLevel = VillagerTrades.TRADES.get(data.getProfession());

        if (byLevel == null) {
            return;
        }

        for (int tradeLevel = 1; tradeLevel <= data.getLevel(); tradeLevel++) {
            var listings = byLevel.get(tradeLevel);

            if (listings == null) {
                continue;
            }

            for (var listing : listings) {
                var offer = listing.getOffer(this, random);

                if (offer != null && seen.add(avp_human$key(offer))) {
                    offers.add(offer);
                }
            }
        }
    }

    @Unique
    private boolean avp_human$isCommissary() {
        return getVillagerData().getProfession() == HumanVillagerProfessions.COMMISSARY.get();
    }

    /**
     * What identifies a commissary trade: the item it sells and the item it costs. Deliberately NOT the stacks'
     * components - an identity item (the sentry turret, any gun) gets a new random id every time a stack is made.
     */
    @Unique
    private static List<Object> avp_human$key(MerchantOffer offer) {
        return List.of(offer.getResult().getItem(), offer.getItemCostA().item().value());
    }
}
