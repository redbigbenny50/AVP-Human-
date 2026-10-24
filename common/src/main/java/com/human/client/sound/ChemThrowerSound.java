package com.human.client.sound;

import com.human.common.registry.init.HumanSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * [stated] Oct 19 - the chem thrower's spray, three sounds cut from one steam-leak recording: "one that fades in when
 * it starts being used. one thats a loop as its consistently being used and one that fades out when you let go".
 * <p>
 * Driven by the blast packets the server already sends every tick a chem thrower fires (to the shooter and everyone
 * watching), so it is heard the same for a player or a marine and on every client:
 * <ol>
 * <li>The first blast plays the START (a 0.6-second rise) and, under it, this LOOP - silent until the start has risen,
 * then blended up to full over a few ticks.</li>
 * <li>Each further blast keeps it going.</li>
 * <li>When the blasts stop for {@value #RELEASE_TICKS} ticks - the trigger let go, the tank empty, the gun put away -
 * the loop stops and the STOP (a 0.8-second fall) plays. Let go during the rise and the rise is cut too.</li>
 * </ol>
 * All three follow the shooter. One loop per shooter at a time; the shooter dying or unloading ends it the same way.
 * </p>
 */
public class ChemThrowerSound extends AbstractTickableSoundInstance {

    /** The start clip's rise. The loop comes in under its end. */
    private static final int START_TICKS = 12;

    /** The loop is blended up over this many ticks as the start finishes. */
    private static final int BLEND_TICKS = 3;

    /** No blast for this long counts as letting go - a little slack for a late packet. */
    private static final int RELEASE_TICKS = 4;

    private static final float VOLUME = 1.0F;

    private static final Map<Integer, ChemThrowerSound> ACTIVE = new HashMap<>();

    private final Entity shooter;

    private final SoundInstance start;

    private long lastBlast;

    private int age;

    private ChemThrowerSound(Entity shooter, SoundInstance start, long now) {
        super(HumanSoundEvents.WEAPON_CHEM_THROWER_LOOP.get(), shooter.getSoundSource(), SoundInstance.createUnseededRandom());
        this.shooter = shooter;
        this.start = start;
        this.lastBlast = now;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        follow();
    }

    /** One tick of a blast from {@code shooterId}, seen on this client. */
    public static void onBlast(int shooterId) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;

        if (level == null) {
            return;
        }

        var now = level.getGameTime();
        var playing = ACTIVE.get(shooterId);

        if (playing != null && !playing.isStopped()) {
            playing.lastBlast = now;
            return;
        }

        var shooter = level.getEntity(shooterId);

        if (shooter == null) {
            return;
        }

        var start = oneShot(HumanSoundEvents.WEAPON_CHEM_THROWER_START.get(), shooter);
        var loop = new ChemThrowerSound(shooter, start, now);

        ACTIVE.put(shooterId, loop);
        minecraft.getSoundManager().play(start);
        minecraft.getSoundManager().play(loop);
    }

    @Override
    public void tick() {
        var level = Minecraft.getInstance().level;

        if (level == null || shooter.isRemoved() || shooter.level() != level) {
            finish(false);
            return;
        }

        if (level.getGameTime() - lastBlast > RELEASE_TICKS) {
            finish(true);
            return;
        }

        age++;
        this.volume = VOLUME * Mth.clamp((age - (START_TICKS - BLEND_TICKS)) / (float) BLEND_TICKS, 0.0F, 1.0F);
        follow();
    }

    /** Silent at first on purpose: without this the sound engine would skip a sound that starts at volume 0. */
    @Override
    public boolean canStartSilent() {
        return true;
    }

    /**
     * ⚠⚠ Oct 19 crash: this runs inside {@link #tick}, and the sound engine calls tick() while it is walking its own
     * list of ticking sounds. Starting a sound from here (the fade-out is entity-bound, so it ticks too) added to that
     * list mid-walk and crashed the game with a ConcurrentModificationException. So only this loop stops itself here;
     * the start cut-off and the fade-out are handed to the client to run straight after the sound engine's tick.
     */
    private void finish(boolean fadeOut) {
        stop();
        ACTIVE.remove(shooter.getId(), this);

        var minecraft = Minecraft.getInstance();
        var playFadeOut = fadeOut && !shooter.isRemoved();

        minecraft.tell(() -> {
            minecraft.getSoundManager().stop(start);

            if (playFadeOut && !shooter.isRemoved()) {
                minecraft.getSoundManager().play(oneShot(HumanSoundEvents.WEAPON_CHEM_THROWER_STOP.get(), shooter));
            }
        });
    }

    private void follow() {
        this.x = shooter.getX();
        this.y = shooter.getEyeY() - 0.3D;
        this.z = shooter.getZ();
    }

    private static SoundInstance oneShot(SoundEvent sound, Entity shooter) {
        return new EntityBoundSoundInstance(sound, shooter.getSoundSource(), VOLUME, 1.0F, shooter, shooter.getRandom().nextLong());
    }
}
