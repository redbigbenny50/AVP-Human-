package com.human.client.sound;

import com.human.common.gameplay.cosmetic.CosmeticBlock;
import com.human.common.gameplay.cosmetic.CosmeticBlockEntity;
import com.human.common.gameplay.cosmetic.CosmeticDesign;
import com.human.common.registry.init.HumanSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Set;

/**
 * A running cosmetic machine's working loop ([stated] Oct 31: placeholder sounds for cosmetics that need them). Built
 * like the powered machines' {@code MachineWorkingSound}: ONE looping instance, not a clip re-triggered each cycle, and
 * it stops itself the moment the piece is switched off, broken, or left out of range - so a room full of switched-on
 * fans the player has walked away from costs nothing.
 * <p>
 * Cutting and shaping tools (lathe, mill, saws, grinder, assembly rig) play the tool loop; everything else that turns
 * (fans, the centrifuge, projectors) plays the hum. Both are placeholders in sounds.json.
 * </p>
 */
public class CosmeticMachineSound extends AbstractTickableSoundInstance {

    /** Beyond this the loop is stopped outright rather than left playing at zero volume. */
    static final double RANGE = 16.0D;

    /**
     * At most this many cosmetic loops play at once. A hall of running fans would otherwise stack dozens of identical
     * loops into noise and eat the sound engine's channels; past the cap, extra machines simply stay quiet until a
     * playing one stops.
     */
    static final int MAX_LOOPS = 6;

    /** The loops playing now - a playing loop is counted here until it stops or the sound engine drops it. */
    private static final java.util.Set<CosmeticMachineSound> PLAYING = new java.util.HashSet<>();

    private static final Set<CosmeticDesign> TOOLS = EnumSet.of(
        CosmeticDesign.LATHE, CosmeticDesign.CNC_MILL, CosmeticDesign.TABLE_SAW, CosmeticDesign.BAND_SAW,
        CosmeticDesign.COLD_SAW, CosmeticDesign.BENCH_GRINDER, CosmeticDesign.ASSEMBLY_RIG
    );

    private final CosmeticBlockEntity piece;

    public CosmeticMachineSound(CosmeticBlockEntity piece) {
        super(
            TOOLS.contains(piece.block().design()) ? HumanSoundEvents.BLOCK_COSMETIC_TOOL_LOOP.get() : HumanSoundEvents.BLOCK_COSMETIC_HUM_LOOP.get(),
            SoundSource.BLOCKS,
            SoundInstance.createUnseededRandom()
        );
        this.piece = piece;
        this.looping = true;
        this.delay = 0;
        this.volume = TOOLS.contains(piece.block().design()) ? 0.35F : 0.5F;
        this.pitch = TOOLS.contains(piece.block().design()) ? 1.3F : 1.0F;

        var pos = piece.getBlockPos();

        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
    }

    @Override
    public void tick() {
        if (piece.isRemoved() || !running(piece) || !inRange(piece)) {
            stop();
        }
    }

    static boolean running(CosmeticBlockEntity piece) {
        var state = piece.getBlockState();

        return state.hasProperty(CosmeticBlock.SPINNING) && state.getValue(CosmeticBlock.SPINNING);
    }

    static boolean inRange(CosmeticBlockEntity piece) {
        var player = Minecraft.getInstance().player;

        return player != null && player.distanceToSqr(Vec3.atCenterOf(piece.getBlockPos())) <= RANGE * RANGE;
    }

    /**
     * Each client tick on a running-machine piece: start the loop when it is on and none is playing (switched on, or the
     * player walked back into range), and forget a loop that has stopped itself. Reads only the synced block state.
     */
    public static void tick(CosmeticBlockEntity piece) {
        var manager = Minecraft.getInstance().getSoundManager();

        // A loop that stopped itself - or that the sound engine never actually played (out of channels) - frees its
        // slot; otherwise a refused loop would hold a slot forever.
        if (piece.clientSound instanceof CosmeticMachineSound sound && (sound.isStopped() || !manager.isActive(sound))) {
            PLAYING.remove(sound);
            piece.clientSound = null;
        }

        if (piece.clientSound == null && running(piece) && inRange(piece)) {
            PLAYING.removeIf(playing -> playing.isStopped() || !manager.isActive(playing));

            if (PLAYING.size() >= MAX_LOOPS) {
                return;
            }

            var sound = new CosmeticMachineSound(piece);
            piece.clientSound = sound;
            PLAYING.add(sound);
            manager.play(sound);
        }
    }
}
