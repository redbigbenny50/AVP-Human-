package com.human.common.gameplay.entity.living.human.colonist;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every colonist profession, from every mod. [stated] Sep 30: avp_human owns the colonist; other mods add jobs.
 * <p>
 * A plain list, not a game registry: a profession is behaviour, not something synced to clients or saved by number.
 * Register during mod setup - the dropship mod's navigator, for example, calls {@code register(NAVIGATOR)} from its own
 * initialiser. avp_human's electrician is registered here, so it always exists.
 * </p>
 */
public final class ColonistProfessions {

    private static final Map<ResourceLocation, ColonistProfession> PROFESSIONS = Collections.synchronizedMap(new LinkedHashMap<>());

    static {
        register(ElectricianProfession.INSTANCE);
    }

    private ColonistProfessions() {}

    /** Adds a profession. Registering the same id twice keeps the first - a mod loaded twice cannot swap it. */
    public static void register(ColonistProfession profession) {
        PROFESSIONS.putIfAbsent(profession.id(), profession);
    }

    public static @Nullable ColonistProfession byId(@Nullable ResourceLocation id) {
        return id == null ? null : PROFESSIONS.get(id);
    }

    /** The profession whose workstation this block is, or null. */
    public static @Nullable ColonistProfession forWorkstation(BlockState state) {
        synchronized (PROFESSIONS) {
            for (var profession : PROFESSIONS.values()) {
                if (profession.isWorkstation(state)) {
                    return profession;
                }
            }
        }

        return null;
    }

    public static Collection<ColonistProfession> all() {
        synchronized (PROFESSIONS) {
            return new ArrayList<>(PROFESSIONS.values());
        }
    }
}
