package com.human.common.gameplay.block.resonator;

import net.minecraft.core.BlockPos;

import java.util.function.BiConsumer;

/**
 * Quality pass, Oct 21 - the wiring tool (shared code, loaded on dedicated servers too) shows a resonator's sphere on
 * the client. It used to call the client-only sphere renderer directly; that call only ever ran on the client, but a
 * shared class naming a client class is exactly what crashed dedicated servers before. Now the shared side only knows
 * this hook: it does nothing until the client sets it (HumanClient.initialize), and on a server it is never set.
 */
public final class ResonatorSphereHook {

    private ResonatorSphereHook() {}

    public static volatile BiConsumer<BlockPos, Integer> show = (pos, radius) -> {};
}
