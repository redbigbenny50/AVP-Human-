package com.human.common.gameplay.item.gun.attack.hitscan;

import com.blib.api.common.dismemberment.v1.hitbox.LimbHitboxRegistry;
import com.human.common.gameplay.item.gun.GunConfig;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

import java.util.Set;

/** Server-authoritative firearm resistance rules for armoured AVP head hitboxes. */
final class ArmoredHeadResistance {

    private static final Set<ResourceLocation> SMALL_AND_MEDIUM_PROOF_HEADS = Set.of(
        avpLimb("crusher_head"),
        avpLimb("empress_head"),
        avpLimb("queen_head"),
        avpLimb("chrysalis_head"),
        avpLimb("harbinger_head")
    );

    static boolean blocks(GunConfig gun, LimbHitboxRegistry.Hit hit) {
        return SMALL_AND_MEDIUM_PROOF_HEADS.contains(hit.volume().limbId()) && isSmallOrMediumRound(gun);
    }

    private static boolean isSmallOrMediumRound(GunConfig gun) {
        var supplier = gun.ammunitionItemSupplier();
        if (supplier == null) {
            return false;
        }
        var ammunition = supplier.get();
        return sameItem(ammunition, HumanItems.SMALL_BULLET.get()) || sameItem(ammunition, HumanItems.MEDIUM_BULLET.get());
    }

    private static boolean sameItem(ItemLike first, ItemLike second) {
        return first.asItem() == second.asItem();
    }

    private static ResourceLocation avpLimb(String path) {
        return ResourceLocation.fromNamespaceAndPath("avp_alien", path);
    }

    private ArmoredHeadResistance() {}
}
