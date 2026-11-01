package com.human.common.registry.init;

import com.blib.api.common.data_sync.v1.model.DataSyncKey;
import com.blib.api.common.data_sync.v1.model.DataUser;
import com.blib.api.common.registry.v1.BLibBuiltInRegistries;
import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.HumanResources;
import com.human.common.gameplay.entity.manager.GeneContainer;
import com.human.common.gameplay.gene.AdvancedGenes;
import com.human.common.gameplay.gene.Genes;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * The few gene values a CLIENT needs, packed into one synced number per mob (genetics sync pass, Oct 28). Genes live on
 * the server; these are the effects drawn or simulated on a player's own screen:
 * <ul>
 * <li><b>Cold-Blooded</b> (any mob) - the predator's thermal view reads it cooler.</li>
 * <li><b>Stealth</b> (your own player) - silent footsteps at 100% on your own screen too.</li>
 * <li><b>Climbing</b> (players) - wall climbing is player movement, which runs on the client.</li>
 * <li><b>Echolocation</b> (players) - outlines of nearby creatures, drawn on the client.</li>
 * </ul>
 * Layout: bits 0-6 cold-blooded %, 7-13 stealth %, 14 climbing, 15 echolocation. Night Vision needs none of this - it
 * is the vanilla effect, applied by the server.
 * <p>
 * The key's id is fixed ({@value #KEY_ID}) so other mods - avp_predator's thermal view - read it through BLib's
 * registry without depending on avp_human.
 * </p>
 */
public final class HumanGeneSync {

    public static final String KEY_ID = "avp_human:entity_gene_flags";

    private static final BLibRegistry<DataSyncKey<?>> SYNC_KEYS = Human.MOD.registries().create(BLibBuiltInRegistries.DATA_SYNC_KEYS);

    public static final BLibHolder<DataSyncKey<Integer>> ENTITY_GENE_FLAGS = SYNC_KEYS.createHolder(
        "entity_gene_flags",
        () -> new DataSyncKey.Builder<Integer>(HumanResources.location("entity_gene_flags"))
            .networkSynchronized(StreamCodecs.INT)
            .build(0)
    );

    private static final int CLIMBING_BIT = 1 << 14;

    private static final int ECHOLOCATION_BIT = 1 << 15;

    private HumanGeneSync() {}

    public static void initialize() {
        SYNC_KEYS.registerAll();
    }

    /** Recomputes the packed flags from the mob's genes; only writes when they changed. Server side. */
    public static void refresh(LivingEntity entity, GeneContainer container, AdvancedGenes.State advanced) {
        var map = container.getActiveGeneMap();
        var flags = percent(map.getValue(Genes.COLD_BLOODED)) | percent(map.getValue(Genes.STEALTH)) << 7;

        if (advanced.isActive(Genes.CLIMBING)) {
            flags |= CLIMBING_BIT;
        }
        if (advanced.isActive(Genes.ECHOLOCATION)) {
            flags |= ECHOLOCATION_BIT;
        }

        var key = ENTITY_GENE_FLAGS.get();
        var data = ((DataUser) entity).getDataContainer();

        if (!java.util.Objects.equals(data.get(key), flags)) {
            data.set(key, flags);
        }
    }

    /** The packed flags, readable on either side. */
    public static int flags(Entity entity) {
        var value = ((DataUser) entity).getDataContainer().get(ENTITY_GENE_FLAGS.get());
        return value == null ? 0 : value;
    }

    public static double coldBlooded(int flags) {
        return (flags & 0x7F) / 100.0;
    }

    public static double stealth(int flags) {
        return ((flags >> 7) & 0x7F) / 100.0;
    }

    public static boolean climbing(int flags) {
        return (flags & CLIMBING_BIT) != 0;
    }

    public static boolean echolocation(int flags) {
        return (flags & ECHOLOCATION_BIT) != 0;
    }

    private static int percent(double value) {
        return (int) Math.clamp(Math.round(value * 100.0), 0, 100);
    }
}
