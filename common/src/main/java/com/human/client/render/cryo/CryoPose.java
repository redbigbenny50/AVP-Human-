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
 * none of these clips (anything not an alien, and any caste still without one) keeps its normal idle, frozen in the
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

    /**
     * ⚠⚠ Oct 19 (his report: "Face hugger animation isnt working in cryotank"). The pose was always played on a track
     * called "body" - and the facehugger has none (its tracks are legs / lungs / tail), so the clip went nowhere and it
     * froze in its idle. The chestbursters (head / tail), boiler and ovomorph have no "body" either. Now: "body" where
     * the animator has one, otherwise its FIRST track - a held pose clip moves every bone it names whichever track it
     * is on. Worked out once per entity type.
     */
    private static final Map<EntityType<?>, String> POSE_TRACK = new HashMap<>();

    private static String poseTrack(AzAnimator<Object, Object> animator, Entity entity) {
        return POSE_TRACK.computeIfAbsent(entity.getType(), type -> {
            String first = null;

            for (var track : animator.getAnimationTrackContainer().getAll()) {
                if (TRACK.equals(track.name())) {
                    return TRACK;
                }

                if (first == null) {
                    first = track.name();
                }
            }

            return first == null ? TRACK : first;
        });
    }

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

        iconCommand(poseTrack(typed, entity), clip.get()).dispatchForEntity(entity);

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

    /**
     * [stated] Oct 19 - "i tried to center the pivot best i could for the molt.loop animation that should be the point
     * centered in the cryotank". A cryo_poses.json entry may name a CENTRE BONE: that bone's pivot, where the frozen
     * pose puts it, goes in the middle of the tank instead of the mob's feet-and-box placement. Entity id -> bone.
     */
    private static Map<String, String> centres;

    private CryoPose() {}

    /** Forgets cached choices - after a resource reload, so a changed cryo_poses.json or new clips take effect. */
    public static void clear() {
        CHOSEN.clear();
        POSE_TRACK.clear();
        ICON_CHOSEN.clear();
        ICON_LAYERS_FOUND.clear();
        overrides = null;
        centres = null;
    }

    private static Map<String, String> overrides() {
        if (overrides != null) {
            return overrides;
        }

        var loaded = new HashMap<String, String>();
        var loadedCentres = new HashMap<String, String>();
        var location = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_human", "cryo_poses.json");

        // ⚠ Every pack's copy is read, lowest priority first, so a higher pack can override single entries.
        for (var resource : net.minecraft.client.Minecraft.getInstance().getResourceManager().getResourceStack(location)) {
            try (var reader = resource.openAsReader()) {
                var json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();

                for (var entry : json.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        loaded.put(entry.getKey(), entry.getValue().getAsString());
                    } else if (entry.getValue().isJsonObject()) {
                        // { "clip": "...", "center": "root" } - either part optional; a missing clip keeps the
                        // default choice.
                        var object = entry.getValue().getAsJsonObject();

                        if (object.has("clip") && object.get("clip").isJsonPrimitive()) {
                            loaded.put(entry.getKey(), object.get("clip").getAsString());
                        }

                        if (object.has("center") && object.get("center").isJsonPrimitive()) {
                            loadedCentres.put(entry.getKey(), object.get("center").getAsString());
                        }
                    }
                }
            } catch (Exception exception) {
                // A broken pack file loses its overrides, never the game: the defaults still apply.
                com.human.Human.LOGGER.warn("[Cryo] Could not read {} from a resource pack: {}", location, exception.toString());
            }
        }

        overrides = loaded;
        centres = loadedCentres;
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

        var track = poseTrack(typed, entity);

        COMMANDS.computeIfAbsent(
            track + "|" + clip.get(),
            // Into the pose in one tick, not the track's usual blend: a copy that has just been built would otherwise
            // be
            // seen unfolding out of its bind pose into the curl (Oct 19).
            key -> AzCommand.<Object>idempotent()
                .setTransitionSpeed(AzTarget.track(track), 1.0F)
                .play(AzTarget.track(track), clip.get(), AzPlayBehaviors.HOLD_ON_LAST_FRAME)
                .build()
        ).dispatchForEntity(entity);

        // The bones already hold THIS frame's pose here (the redirect sits after the bone update), so the centre is
        // measured from the pose as drawn. A held pose does not move, so the renderer using it one frame late is exact.
        var centreBone = centreBoneFor(entity);
        CryoDisplay.setCentre(entity, centreBone == null ? null : posedPivot(typed, centreBone));
        CryoDisplay.setBounds(entity, posedBounds(typed));

        return true;
    }

    private static @org.jetbrains.annotations.Nullable String centreBoneFor(Entity entity) {
        overrides();
        return centres.get(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    /**
     * Where the named bone's pivot is in the posed model, in blocks - the same chain of transforms BLib's model
     * renderer applies (RenderUtil: move by the bone's position, to its pivot, rotate Z-Y-X, scale, back off the
     * pivot), parent first. Null if the model has no such bone.
     */
    private static @org.jetbrains.annotations.Nullable org.joml.Vector3f posedPivot(
        AzAnimator<Object, Object> animator,
        String boneName
    ) {
        var bakedModel = animator.context().boneCache().getBakedModel();

        if (bakedModel == null) {
            return null;
        }

        var bone = bakedModel.getBoneOrNull(boneName);

        if (bone == null) {
            return null;
        }

        var chain = new java.util.ArrayDeque<com.blib.api.client.model.v1.AzBone>();

        for (var parent = bone.getParent(); parent != null; parent = parent.getParent()) {
            chain.push(parent);
        }

        var matrix = new org.joml.Matrix4f();

        for (var link : chain) {
            matrix.translate(-link.getPosX() / 16F, link.getPosY() / 16F, link.getPosZ() / 16F)
                .translate(link.getPivotX() / 16F, link.getPivotY() / 16F, link.getPivotZ() / 16F)
                .rotateZYX(link.getRotZ(), link.getRotY(), link.getRotX())
                .scale(link.getScaleX(), link.getScaleY(), link.getScaleZ())
                .translate(-link.getPivotX() / 16F, -link.getPivotY() / 16F, -link.getPivotZ() / 16F);
        }

        return matrix.transformPosition(
            new org.joml.Vector3f(
                (-bone.getPosX() + bone.getPivotX()) / 16F,
                (bone.getPosY() + bone.getPivotY()) / 16F,
                (bone.getPosZ() + bone.getPivotZ()) / 16F
            )
        );
    }

    /**
     * The posed model's box in model space: every cube's eight corners (its own baked rotation included) through its
     * bone's posed transform - the same chain posedPivot uses, walked down the whole bone tree. Hidden bones are
     * skipped. Null if the model is not baked yet or has nothing in it. A few hundred corners for a big caste; only for
     * a specimen being drawn in a tank.
     */
    private static float @org.jetbrains.annotations.Nullable [] posedBounds(AzAnimator<Object, Object> animator) {
        var bakedModel = animator.context().boneCache().getBakedModel();

        if (bakedModel == null) {
            return null;
        }

        var box = new float[] { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };

        for (var bone : bakedModel.getTopLevelBones()) {
            accumulate(bone, new org.joml.Matrix4f(), box);
        }

        return box[0] > box[3] ? null : box;
    }

    private static void accumulate(com.blib.api.client.model.v1.AzBone bone, org.joml.Matrix4f parent, float[] box) {
        if (bone.isHidden()) {
            return;
        }

        var matrix = new org.joml.Matrix4f(parent)
            .translate(-bone.getPosX() / 16F, bone.getPosY() / 16F, bone.getPosZ() / 16F)
            .translate(bone.getPivotX() / 16F, bone.getPivotY() / 16F, bone.getPivotZ() / 16F)
            .rotateZYX(bone.getRotZ(), bone.getRotY(), bone.getRotX())
            .scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ())
            .translate(-bone.getPivotX() / 16F, -bone.getPivotY() / 16F, -bone.getPivotZ() / 16F);

        var corner = new org.joml.Vector3f();

        for (var cube : bone.getCubes()) {
            var bounds = cube.bounds();

            if (bounds.isEmpty()) {
                continue;
            }

            for (var i = 0; i < 8; i++) {
                corner.set(
                    bounds.centerX() + ((i & 1) == 0 ? -bounds.extentX() : bounds.extentX()),
                    bounds.centerY() + ((i & 2) == 0 ? -bounds.extentY() : bounds.extentY()),
                    bounds.centerZ() + ((i & 4) == 0 ? -bounds.extentZ() : bounds.extentZ())
                );

                if (!cube.transform().identity()) {
                    cube.transform().pose().transformPosition(corner);
                }

                matrix.transformPosition(corner);
                box[0] = Math.min(box[0], corner.x);
                box[1] = Math.min(box[1], corner.y);
                box[2] = Math.min(box[2], corner.z);
                box[3] = Math.max(box[3], corner.x);
                box[4] = Math.max(box[4], corner.y);
                box[5] = Math.max(box[5], corner.z);
            }
        }

        for (var child : bone.getChildBones()) {
            accumulate(child, matrix, box);
        }
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
