package com.human.client.particle;

import com.human.common.gameplay.vapor.VaporKind;
import com.human.common.gameplay.vapor.VaporLook;
import com.human.common.gameplay.vapor.VaporParticleOptions;
import com.human.common.gameplay.vapor.VaporStyle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A vapor puff ([stated] Oct 18 - Gasworks-style gas, the per-vapor mixes): a soft see-through sprite, tinted to the
 * vapor's colour, that swells and thins as it ages and BOUNCES off blocks.
 * <ul>
 * <li>Drawn translucent, so overlapping puffs blend into a mist instead of stacking into solid squares.</li>
 * <li>Six frames per style, chosen by age; it grows from its start size to its end size, and fades out over its last
 * third.</li>
 * <li>It drifts up or down as its vapor does ({@link VaporLook#drift}): cold cryo sinks and pools, hot smoke
 * rises.</li>
 * <li>⚠ Its own collision, not vanilla's: a vanilla particle that is stopped falling is frozen for the rest of its life
 * (Particle.move's private stoppedByCollision), so sinking gas could never roll along the floor. Here a blocked axis
 * bounces back at {@value #BOUNCE} of its speed and the other axes carry on.</li>
 * <li>Infernium and irradium glow (full brightness), so they show in the dark.</li>
 * </ul>
 * ⚡ One small box collision a tick while it moves (what any vanilla particle with physics costs); none once it has
 * settled.
 */
public class VaporParticle extends TextureSheetParticle {

    private static final double BOUNCE = 0.35;

    private static final double FRICTION = 0.94;

    private static final int FULL_BRIGHT = 0xF000F0;

    private static final float MAX_ALPHA = 0.85F;

    private final SpriteSet sprites;

    private final VaporStyle style;

    private final float drift;

    private final boolean glow;

    private final float startSize;

    private final float endSize;

    protected VaporParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xd,
        double yd,
        double zd,
        VaporParticleOptions options,
        SpriteSet sprites
    ) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.style = VaporStyle.byOrdinal(options.style());

        var kind = VaporKind.byOrdinal(options.vapor());
        var look = kind == null ? null : VaporLook.of(kind);
        // The mist's own colour - cryo is cryofluid's pale ice, not its tank's blue (VaporLook).
        var colour = kind == null ? 0xFFFFFF : look.colour(kind);

        this.drift = look == null ? 0.0F : look.drift();
        this.glow = look != null && look.glow();
        this.startSize = options.startSize();
        this.endSize = options.endSize();

        // A little shade variation between puffs, as vanilla's coloured particles have.
        var shade = 0.85F + random.nextFloat() * 0.15F;

        this.rCol = (colour >> 16 & 0xFF) / 255.0F * shade;
        this.gCol = (colour >> 8 & 0xFF) / 255.0F * shade;
        this.bCol = (colour & 0xFF) / 255.0F * shade;
        this.alpha = MAX_ALPHA;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.lifetime = options.lifetime() > 0 ? options.lifetime() : 20 + random.nextInt(14);
        this.quadSize = startSize / 2.0F;
        setSize(0.1F, 0.1F);
        setSprite(sprites.get(style.sprite(0), VaporStyle.SPRITES - 1));
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return glow ? FULL_BRIGHT : super.getLightColor(partialTick);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;

        if (age++ >= lifetime) {
            remove();
            return;
        }

        var t = age / (float) lifetime;
        var frame = Math.min(VaporStyle.FRAMES - 1, (int) (t * VaporStyle.FRAMES));

        setSprite(sprites.get(style.sprite(frame), VaporStyle.SPRITES - 1));
        quadSize = (startSize + (endSize - startSize) * t) / 2.0F;
        alpha = t < 0.66F ? MAX_ALPHA : MAX_ALPHA * Math.max(0.0F, (1.0F - t) / 0.34F);

        // Drift toward the vapor's own rise or sink speed; friction makes `drift` the speed it settles at.
        yd += drift * (1.0 - FRICTION);

        moveAndBounce();

        xd *= FRICTION;
        yd *= FRICTION;
        zd *= FRICTION;
    }

    private void moveAndBounce() {
        if (xd * xd + yd * yd + zd * zd < 1.0E-8) {
            return;
        }

        var wanted = new Vec3(xd, yd, zd);
        var moved = Entity.collideBoundingBox(null, wanted, getBoundingBox(), level, List.of());

        setBoundingBox(getBoundingBox().move(moved));
        setLocationFromBoundingbox();

        if (moved.x != wanted.x) {
            xd = -xd * BOUNCE;
        }

        if (moved.y != wanted.y) {
            yd = -yd * BOUNCE;
        }

        if (moved.z != wanted.z) {
            zd = -zd * BOUNCE;
        }
    }

    public static class Provider implements ParticleProvider<VaporParticleOptions> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
            @NotNull VaporParticleOptions options,
            @NotNull ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd
        ) {
            return new VaporParticle(level, x, y, z, xd, yd, zd, options, sprites);
        }
    }
}
