package com.human.common.registry.tag;

import com.human.Human;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class HumanEntityTypeTags {

    public static final TagKey<EntityType<?>> HATED_BY_MARINES = create("hated_by_marines");

    /**
     * Oct 5 - creatures marines never hunt on sight even though the generic hostile rule would catch them (see
     * MarineHuntRules). Ships with the yautja; a datapack adds any third-party mob a pack wants left alone. It does not
     * stop a marine defending itself or its squad.
     */
    public static final TagKey<EntityType<?>> IGNORED_BY_MARINES = create("ignored_by_marines");

    public static final TagKey<EntityType<?>> RADIATION_RESISTANT = create("radiation_resistant");

    /** Mobs cryofluid never freezes. Includes vanilla's freeze-immune mobs. */
    public static final TagKey<EntityType<?>> CRYOFLUID_IMMUNE = create("cryofluid_immune");

    /**
     * Never frozen SOLID (FrozenSolid), though still chilled by freezing. [stated] "dragon and wither no warden and
     * elder guardian yes". Datapackable.
     */
    public static final TagKey<EntityType<?>> FREEZE_SOLID_IMMUNE = create("freeze_solid_immune");

    /**
     * Mobs a cryotank refuses. Datapackable: add any boss or special mob a pack does not want kept on ice. Ships with
     * vanilla's bosses.
     */
    public static final TagKey<EntityType<?>> CRYOTANK_BLACKLIST = create("cryotank_blacklist");

    /**
     * What a smart weapon's tracker will acquire.
     * <p>
     * ⚠ A TAG, NOT AN INSTANCEOF LIST. It lets a xenomorph or a predator be targetable without avp_human taking a hard
     * dependency on either mod - the same reason the music-disc fragments go through a tag. The targeting code falls
     * back to {@code Enemy} so vanilla hostiles work even with no data loaded at all, which means this tag only ever
     * has to name the things that are NOT already hostile mobs.
     * </p>
     */
    public static final TagKey<EntityType<?>> SMART_TARGETABLE = create("smart_targetable");

    /**
     * Things a smart weapon will NOT acquire, whatever else says otherwise. Checked after everything, so it wins over
     * both the targetable tag and the hostile-mob fallback.
     * <p>
     * ⚠ A SECOND TAG, BECAUSE TAGS CANNOT SUBTRACT. SMART_TARGETABLE is seeded from the marine hate list, and there is
     * no way to say "all of that except one entry" inside a tag file - the only way to carve something out is a
     * separate list that the code consults afterwards.
     * </p>
     * <p>
     * ⭐ ENDERMEN ARE THE FIRST ENTRY. They teleport the instant they are looked at, so a tracker locks, loses them,
     * re-acquires whatever is behind them and generally flickers - and they are not what a marine is hunting anyway.
     * </p>
     */
    public static final TagKey<EntityType<?>> SMART_TARGETING_IGNORED = create("smart_targeting_ignored");

    private static TagKey<EntityType<?>> create(String name) {
        return Human.MOD.resources().createTagKey(Registries.ENTITY_TYPE, name);
    }
}
