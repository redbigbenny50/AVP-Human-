package com.human.client.render.cryo;

import com.blib.api.client.animation.v1.animator.AzAnimator;
import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The pose a frozen specimen is held in. [stated] Oct 2: adult xenomorphs curl into their {@code molt.loop},
 * facehuggers lie in their dead pose, and bursters and adolescents use their own molt loop.
 * <p>
 * The clip is chosen by what the specimen's OWN animation file contains, first match wins - so every strain of a caste
 * works without a list of entity ids, and a new caste with a {@code molt.loop} gets the pose for free. A specimen with
 * none of these clips (chestbursters, spitters, the queen, anything not an alien) keeps its normal idle, frozen in the
 * tank.
 * </p>
 * <p>
 * The clip plays once and holds its last frame, so the specimen settles into the pose and stays perfectly still. ⚠ It
 * is dispatched on the {@code body} track, which every avp_alien animator registers.
 * </p>
 */
public final class CryoPose {

    /** Adults, bursters / facehuggers / the three adolescent kinds - drone first, as the commonest outcome. */
    private static final String[] CANDIDATES = {
        "molt.loop",
        "animation.dead",
        "molt.drone.loop",
        "molt.predalien.loop",
        "molt.praetorian.loop"
    };

    private static final String TRACK = "body";

    private static final Map<EntityType<?>, Optional<String>> CHOSEN = new HashMap<>();

    private static final Map<String, AzCommand<Object>> COMMANDS = new HashMap<>();

    /** Icons: the idle, frozen this many ticks in - long enough for the blend from the bind pose to finish. */
    private static final double ICON_FREEZE_TICK = 10.0;

    /**
     * First match wins. [stated] Oct 2: the facehugger's idle leaves its tail straight, so its tail-sway clip comes
     * before its idle - frozen half a second in, the tail is curled up behind it.
     */
    private static final String[] ICON_CANDIDATES = { "idle", "animation.tailsway", "animation.idle", "animation.eggclosehold" };

    private static final Map<EntityType<?>, Optional<String>> ICON_CHOSEN = new HashMap<>();

    private static final Map<String, AzCommand<Object>> ICON_COMMANDS = new HashMap<>();

    /**
     * Extra layers frozen alongside the body, on their own tracks, for mobs whose parts are not in any body clip.
     * [stated] Oct 2: the harbinger's back whips stuck straight out - its whips run on their own "leftwhip" /
     * "rightwhip" tracks, so they get their resting idle sway too. A mob without the clip is untouched.
     */
    private static final String[][] ICON_LAYERS = {
        { "leftwhip", "idle.leftwhip" },
        { "rightwhip", "idle.rightwhip" }
    };

    private static final Map<EntityType<?>, java.util.List<String[]>> ICON_LAYERS_FOUND = new HashMap<>();

    /**
     * The screen icon's pose: [stated] Oct 2 the idle, baked to ONE frame - its tail curls in the idle, where the bind
     * pose leaves it straight out. A mob with no idle clip keeps whatever it does normally.
     */
    private static AzCommand<Object> iconCommand(String track, String clip) {
        return ICON_COMMANDS.computeIfAbsent(
            track + "|" + clip,
            key -> AzCommand.<Object>idempotent()
                .setFreezeTickOffset(AzTarget.track(track), ICON_FREEZE_TICK)
                .play(AzTarget.track(track), clip, AzPlayBehaviors.FREEZE_ON_FRAME)
                .build()
        );
    }

    public static boolean applyIcon(AzAnimator<?, ?> animator, Entity entity) {
        @SuppressWarnings("unchecked")
        var typed = (AzAnimator<Object, Object>) animator;

        var clip = ICON_CHOSEN.computeIfAbsent(entity.getType(), type -> {
            for (var name : ICON_CANDIDATES) {
                if (typed.getAnimation(entity, name) != null) {
                    return Optional.of(name);
                }
            }

            return Optional.empty();
        });

        if (clip.isEmpty()) {
            return false;
        }

        iconCommand(TRACK, clip.get()).dispatchForEntity(entity);

        var layers = ICON_LAYERS_FOUND.computeIfAbsent(entity.getType(), type -> {
            var found = new java.util.ArrayList<String[]>();

            for (var layer : ICON_LAYERS) {
                if (typed.getAnimation(entity, layer[1]) != null) {
                    found.add(layer);
                }
            }

            return found;
        });

        for (var layer : layers) {
            iconCommand(layer[0], layer[1]).dispatchForEntity(entity);
        }

        return true;
    }

    private static Map<String, String> overrides;

    private CryoPose() {}

    /** Forgets cached choices - after a resource reload, so a changed cryo_poses.json or new clips take effect. */
    public static void clear() {
        CHOSEN.clear();
        ICON_CHOSEN.clear();
        ICON_LAYERS_FOUND.clear();
        overrides = null;
    }

    private static Map<String, String> overrides() {
        if (overrides != null) {
            return overrides;
        }

        var loaded = new HashMap<String, String>();
        var location = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_human", "cryo_poses.json");

        // ⚠ Every pack's copy is read, lowest priority first, so a higher pack can override single entries.
        for (var resource : net.minecraft.client.Minecraft.getInstance().getResourceManager().getResourceStack(location)) {
            try (var reader = resource.openAsReader()) {
                var json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();

                for (var entry : json.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        loaded.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            } catch (Exception exception) {
                // A broken pack file loses its overrides, never the game: the defaults still apply.
                com.human.Human.LOGGER.warn("[Cryo] Could not read {} from a resource pack: {}", location, exception.toString());
            }
        }

        overrides = loaded;
        return overrides;
    }

    /**
     * Called in place of the animator's own per-frame logic for a cryo display copy.
     *
     * @return false when this specimen has no frozen pose, so the caller lets its normal animation run instead
     */
    public static boolean apply(AzAnimator<?, ?> animator, Entity entity) {
        @SuppressWarnings("unchecked")
        var typed = (AzAnimator<Object, Object>) animator;

        var clip = CHOSEN.computeIfAbsent(entity.getType(), type -> choose(typed, entity));

        if (clip.isEmpty()) {
            return false;
        }

        COMMANDS.computeIfAbsent(
            clip.get(),
            name -> AzCommand.<Object>idempotent().play(AzTarget.track(TRACK), name, AzPlayBehaviors.HOLD_ON_LAST_FRAME).build()
        ).dispatchForEntity(entity);

        return true;
    }

    private static Optional<String> choose(AzAnimator<Object, Object> animator, Entity entity) {
        // A resource pack can name the exact clip for any mob in assets/avp_human/cryo_poses.json:
        // { "avp_alien:queen": "molt.loop.empress", "avp_alien:spitter": "molt.emerge" } - checked before the defaults.
        var override = overrides().get(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());

        if (override != null && animator.getAnimation(entity, override) != null) {
            return Optional.of(override);
        }

        for (var name : CANDIDATES) {
            if (animator.getAnimation(entity, name) != null) {
                return Optional.of(name);
            }
        }

        return Optional.empty();
    }
}
