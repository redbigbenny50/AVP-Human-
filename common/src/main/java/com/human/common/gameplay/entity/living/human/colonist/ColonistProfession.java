package com.human.common.gameplay.entity.living.human.colonist;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * A colonist's job. [stated] Sep 30: the colonist mob belongs to avp_human; professions can come from any mod - the
 * electrician is avp_human's, the navigator is the dropship mod's. A mod registers its own with
 * {@link ColonistProfessions#register} during its setup, and the colonist does the rest: claiming a free workstation
 * within 8 blocks, wearing the outfit, walking back to the station, trading, and losing the job when the station is
 * broken or claimed by another colonist.
 * <h2>WHAT A PROFESSION DECIDES</h2> Only what differs between jobs: which block is its workstation, what it wears, its
 * trade screen's title, and its trades. Per-colonist state a profession wants to keep (a trade tab, sold-out counters)
 * goes in {@link ColonistEntity#getProfessionData()}, which is saved with the colonist.
 */
public interface ColonistProfession {

    /** Saved on the colonist - never change it once released, or colonists in existing worlds lose the job. */
    ResourceLocation id();

    boolean isWorkstation(BlockState state);

    /** The trade screen's title, e.g. "Electrician". */
    Component title();

    /** Worn while the colonist holds the job - put on when it starts, taken off when it ends. Never dropped. */
    Map<EquipmentSlot, ItemStack> outfit();

    /** The colonist's trades. Called when the job starts and when the colonist's offers are rebuilt. */
    MerchantOffers createOffers(ColonistEntity colonist);

    /** A trade was made. Uses are already counted; override to keep your own tallies in the profession data. */
    default void onTrade(ColonistEntity colonist, MerchantOffer offer) {}

    /** How often, in ticks, the colonist restocks - while at its workstation. Vanilla villagers restock twice a day. */
    default int restockIntervalTicks() {
        return 12000;
    }
}
