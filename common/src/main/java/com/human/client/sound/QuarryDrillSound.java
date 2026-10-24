package com.human.client.sound;

import com.human.common.gameplay.quarry.QuarryTerminalBlockEntity;
import com.human.common.registry.init.HumanSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The quarry's two working loops, each placed on its part of the machine and moved with it every tick:
 * <ul>
 * <li><b>BIT</b> - [stated] Oct 19 "this plays at the bit end so its distance increases as the bit moves down": at the
 * bottom of the cutter. As the bit sinks, a player at the rim hears it fall away into the pit.</li>
 * <li><b>HEAD</b> - [stated] Oct 19 "for the drill head at the surface while it runs": at the drill head on its gantry,
 * which stays at the top while the cutter goes down, and follows the head as it travels between columns.</li>
 * <li><b>ARMS</b> - [stated] Oct 19 "a sound for the arms moving for the quarry": the gantry's servos, at the head
 * (where the X and Z arms meet), only while the head is actually travelling to the next chunk - from the moment the
 * cutter is up and the carriage sets off until it arrives. A recording made deeper, louder and shorter.</li>
 * </ul>
 * Both play while the terminal says WORKING (the state that spins the cutter and throws the dig debris) and stop
 * themselves when the quarry stops, is broken, or the player walks away. Each clip is a ten-second cut of the supplied
 * recording with its ends cross-faded, so the loops have no seam.
 * <p>
 * Heavy machinery: played at volume {@value #VOLUME}, which Minecraft hears out to about {@code 16 x volume} blocks.
 * </p>
 */
public class QuarryDrillSound extends AbstractTickableSoundInstance {

    public enum Part {
        BIT,
        HEAD,
        ARMS
    }

    private static final float VOLUME = 2.0F;

    /** Beyond this the loop is stopped outright rather than left playing silently. */
    private static final double RANGE = 40.0D;

    private static final Map<QuarryTerminalBlockEntity, Map<Part, QuarryDrillSound>> PLAYING = new WeakHashMap<>();

    private final QuarryTerminalBlockEntity terminal;

    private final Part part;

    private QuarryDrillSound(QuarryTerminalBlockEntity terminal, Part part) {
        super(
            switch (part) {
                case BIT -> HumanSoundEvents.QUARRY_DRILL.get();
                case HEAD -> HumanSoundEvents.QUARRY_DRILL_HEAD.get();
                case ARMS -> HumanSoundEvents.QUARRY_ARMS.get();
            },
            SoundSource.BLOCKS,
            SoundInstance.createUnseededRandom()
        );
        this.terminal = terminal;
        this.part = part;
        this.looping = true;
        this.delay = 0;
        this.volume = VOLUME;
        this.pitch = 1.0F;
        moveTo(where(terminal, part));
    }

    /**
     * Called by the quarry's renderer every frame: starts each loop this quarry should be playing and is not yet. Cheap
     * when both are already playing - two map lookups.
     */
    public static void ensure(QuarryTerminalBlockEntity terminal) {
        var sounds = PLAYING.computeIfAbsent(terminal, key -> new EnumMap<>(Part.class));

        for (var part : Part.values()) {
            var playing = sounds.get(part);

            if (playing != null && !playing.isStopped()) {
                continue;
            }

            if (!shouldPlay(terminal, part)) {
                continue;
            }

            var sound = new QuarryDrillSound(terminal, part);
            sounds.put(part, sound);
            Minecraft.getInstance().getSoundManager().play(sound);
        }
    }

    @Override
    public void tick() {
        if (!shouldPlay(terminal, part)) {
            stop();
            return;
        }

        moveTo(where(terminal, part));
    }

    private void moveTo(Vec3 at) {
        this.x = at.x;
        this.y = at.y;
        this.z = at.z;
    }

    /** The bit: the bottom of the cutter, under the head and as far down as it has dug. The head: the head itself. */
    private static Vec3 where(QuarryTerminalBlockEntity terminal, Part part) {
        var head = terminal.shownHead();

        return part == Part.BIT ? head.subtract(0.0D, terminal.shownDepth(), 0.0D) : head;
    }

    /**
     * The carriage is travelling: where it is shown is not yet where it is going, and the cutter is already up (it
     * rises first and only then does the head move - QuarryTerminalBlockEntity.animate). The shown position is advanced
     * here too, by the time that has passed, so the sound keeps track while the quarry is out of view; it is
     * time-based, so the renderer's own calls are not doubled.
     */
    private static boolean armsMoving(QuarryTerminalBlockEntity terminal) {
        terminal.animate(0.0F);

        var shown = terminal.shownHead();
        var target = terminal.headPosition();
        var travelling = Math.abs(target.x - shown.x) > 1.0E-3D || Math.abs(target.z - shown.z) > 1.0E-3D;

        return travelling && terminal.shownDepth() <= 1.0E-3D;
    }

    private static boolean shouldPlay(QuarryTerminalBlockEntity terminal, Part part) {
        var player = Minecraft.getInstance().player;

        if (terminal.isRemoved() || terminal.getLevel() == null || player == null || player.level() != terminal.getLevel()) {
            return false;
        }

        if (part == Part.ARMS) {
            if (!armsMoving(terminal)) {
                return false;
            }
        } else if (terminal.screen() != QuarryTerminalBlockEntity.Screen.WORKING) {
            return false;
        }

        return player.position().distanceToSqr(where(terminal, part)) <= RANGE * RANGE;
    }
}
