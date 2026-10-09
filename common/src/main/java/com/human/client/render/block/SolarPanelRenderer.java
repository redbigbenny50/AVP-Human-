package com.human.client.render.block;

import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.blib.api.client.render.v1.layer.AzAutoGlowingLayer;
import com.human.HumanResources;
import com.human.client.animation.block.SolarPanelAnimator;
import com.human.common.gameplay.block.entity.power.impl.SolarPanelBlockEntity;
import com.human.common.gameplay.block.power.PowerMode;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The solar panel: one texture per power light, an emissive layer for the lit LED, and a panel that TRACKS THE SUN.
 * <p>
 * ⭐ SUN TRACKING IS DRIVEN BY WORLD TIME, NOT BY PLAYING THE CLIP. [stated] "suntrack plays the duration of the sun
 * moving across the sky this should follow whatever the day cycle is and the movement of the sun. i know this can vary
 * server to server and skipping time so you can speed up or slow down the animation in relation to the sun movement." A
 * clip played at a speed has to be re-timed on every /time set, every sleep, every day-length mod and every relog; a
 * tilt computed from the clock is simply always right. The clip's two keyframes (0 at sunrise, 115 at sunset, from
 * solar_panel.animation.json) are the endpoints; everything in between is interpolated, with partial ticks so it is
 * smooth. The night reset (115 back to 0 over one second) and the hold at 0 until sunrise are the same clock.
 * </p>
 * <p>
 * ⚠ The baked model's bones are SHARED by every panel drawn with this renderer (the Old Painless lesson), which is fine
 * here because the tilt is set immediately before each block's own render pass from that block's own level.
 * </p>
 */
public class SolarPanelRenderer extends AzBlockEntityRenderer<SolarPanelBlockEntity> {

    public static final String NAME = "solar_panel";

    private static final String PANEL_BONE = "gPanel";

    /** From the animation file: sunrise 0, sunset 115 degrees about Z. */
    private static final float SUNRISE_ANGLE_DEG = 0.0F;

    private static final float SUNSET_ANGLE_DEG = 115.0F;

    /**
     * Flip to -1 if the panel is found to track AWAY from the sun in play. Z rotation tilts the panel in the X (east-
     * west) plane; which way is "toward the rising sun" depends on how the geo was authored and cannot be read here.
     */
    private static final float DIRECTION = 1.0F;

    private static final long DAY_LENGTH = 24000L;

    private static final long SUNRISE = 23000L;

    private static final long SUNSET = 13000L;

    /** Length of a day in ticks measured from sunrise (23000) to sunset (13000) going round the clock. */
    private static final long DAYLIGHT_TICKS = (SUNSET + DAY_LENGTH - SUNRISE) % DAY_LENGTH; // 14000

    private static final float NIGHT_RESET_TICKS = 20.0F;

    private static final ResourceLocation MODEL_LOCATION = HumanResources.blockGeoModelLocation(NAME);

    private static final ResourceLocation TEXTURE_OFF = HumanResources.blockTextureLocation(NAME);

    private static final ResourceLocation TEXTURE_LOW = HumanResources.blockTextureLocation(NAME + "_low");

    private static final ResourceLocation TEXTURE_MED = HumanResources.blockTextureLocation(NAME + "_med");

    private static final ResourceLocation TEXTURE_MAX = HumanResources.blockTextureLocation(NAME + "_max");

    public SolarPanelRenderer() {
        super(
            AzBlockEntityRendererConfig.<SolarPanelBlockEntity>builder($ -> MODEL_LOCATION, SolarPanelRenderer::textureFor)
                .setAnimatorProvider(SolarPanelAnimator::new)
                .setPrerenderEntry(context -> {
                    var panel = context.bakedModel().getBoneOrNull(PANEL_BONE);

                    if (panel != null) {
                        panel.setRotZ(DIRECTION * (float) Math.toRadians(panelAngleDegrees(context.partialTick())));
                    }

                    return context;
                })
                .addRenderLayer(new AzAutoGlowingLayer<>())
                .build()
        );
    }

    /** The lit LED texture for the block's current light; each has a _glowmask beside it holding only the lit pixel. */
    private static ResourceLocation textureFor(SolarPanelBlockEntity blockEntity) {
        var state = blockEntity.getBlockState();

        if (!state.hasProperty(PowerMode.PROPERTY)) {
            return TEXTURE_OFF;
        }

        return switch (state.getValue(PowerMode.PROPERTY)) {
            case OFF -> TEXTURE_OFF;
            case LOW -> TEXTURE_LOW;
            case MED -> TEXTURE_MED;
            case MAX -> TEXTURE_MAX;
        };
    }

    /**
     * Where the panel should be pointing right now, in degrees from the sunrise pose.
     * <p>
     * Daylight (23000 round to 13000): sunrise angle to sunset angle, linearly with the clock. First second of night:
     * back from the sunset angle to the sunrise pose (the nightreset clip). Rest of the night: held at the sunrise pose
     * (the holdreset clip).
     * </p>
     */
    private static float panelAngleDegrees(float partialTick) {
        var level = Minecraft.getInstance().level;

        if (level == null) {
            return SUNRISE_ANGLE_DEG;
        }

        var time = (level.getDayTime() % DAY_LENGTH + partialTick + DAY_LENGTH) % DAY_LENGTH;
        var sinceSunrise = (time - SUNRISE + DAY_LENGTH) % DAY_LENGTH;

        if (sinceSunrise < DAYLIGHT_TICKS) {
            return Mth.lerp((float) (sinceSunrise / DAYLIGHT_TICKS), SUNRISE_ANGLE_DEG, SUNSET_ANGLE_DEG);
        }

        var sinceSunset = (float) (sinceSunrise - DAYLIGHT_TICKS);

        if (sinceSunset < NIGHT_RESET_TICKS) {
            return Mth.lerp(sinceSunset / NIGHT_RESET_TICKS, SUNSET_ANGLE_DEG, SUNRISE_ANGLE_DEG);
        }

        return SUNRISE_ANGLE_DEG;
    }
}
