package com.human.client.render;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * The world-render camera state, captured each frame so HUD code can turn world positions into screen positions.
 * <p>
 * ⚠⚠ THE MATRICES MUST BE CAPTURED, NOT REBUILT. The obvious approach - read the FOV option and construct a projection
 * - is wrong in several ways at once: {@code GameRenderer.getFov} is PRIVATE, it folds in the sprint and speed FOV
 * effects, the underwater multiplier, the death-animation squeeze, and a NeoForge hook any mod can alter. A
 * hand-rebuilt projection is right at rest and drifts the moment the player sprints, which shows up as targeting boxes
 * sitting slightly off their targets exactly when things are moving.
 * </p>
 * <p>
 * ⚠ ALSO NOT AVAILABLE FROM {@code RenderSystem} AT HUD TIME. By the time the Gui renders, the projection has been
 * swapped for the GUI's orthographic one, so reading it there gives a 2D matrix that projects nothing.
 * </p>
 * <p>
 * ⚠ POSITIONS ARE CAMERA-RELATIVE. The frustum matrix carries rotation only; the translation is the camera position
 * subtracted before multiplying. Passing an absolute world position produces coordinates thousands of blocks off.
 * </p>
 */
public final class CameraMatrices {

    private static final Matrix4f frustum = new Matrix4f();

    private static final Matrix4f projection = new Matrix4f();

    private static Vec3 cameraPosition = Vec3.ZERO;

    private static boolean valid;

    public static void capture(Vec3 position, Matrix4f frustumMatrix, Matrix4f projectionMatrix) {
        cameraPosition = position;
        frustum.set(frustumMatrix);
        projection.set(projectionMatrix);
        valid = true;
    }

    public static void invalidate() {
        valid = false;
    }

    public static boolean isValid() {
        return valid;
    }

    public static Vec3 cameraPosition() {
        return cameraPosition;
    }

    /**
     * Projects a world position to GUI-scaled screen coordinates, or null if it is behind the camera.
     * <p>
     * ⚠ THE {@code w <= 0} TEST IS NOT OPTIONAL. Without it, a position behind the player divides by a negative w and
     * lands on screen MIRRORED - so a xenomorph directly behind you would draw a targeting box in front of you, and the
     * maths gives no other clue that anything is wrong.
     * </p>
     */
    public static float @Nullable [] project(Vec3 worldPosition, int guiWidth, int guiHeight) {
        if (!valid) {
            return null;
        }

        var vector = new Vector4f(
            (float) (worldPosition.x - cameraPosition.x),
            (float) (worldPosition.y - cameraPosition.y),
            (float) (worldPosition.z - cameraPosition.z),
            1.0F
        );

        vector.mul(frustum);
        vector.mul(projection);

        if (vector.w <= 0.0F) {
            return null;
        }

        var normalisedX = vector.x / vector.w;
        var normalisedY = vector.y / vector.w;

        return new float[] {
            (normalisedX * 0.5F + 0.5F) * guiWidth,
            (1.0F - (normalisedY * 0.5F + 0.5F)) * guiHeight
        };
    }

    private CameraMatrices() {
        throw new UnsupportedOperationException();
    }
}
