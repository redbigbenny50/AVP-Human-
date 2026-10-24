package com.human.neoforge.quarry;

import com.human.common.gameplay.quarry.QuarryProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The quarry's permission check on NeoForge: a {@link BlockEvent.BreakEvent} for a fake player carrying the owner's id.
 * Claim mods (FTB Chunks, Open Parties and Claims, ...) cancel it inside claims the owner may not break in.
 */
public final class NeoForgeQuarryProtection implements QuarryProtection {

    private NeoForgeQuarryProtection() {}

    public static void install() {
        QuarryProtection.Holder.install(new NeoForgeQuarryProtection());
    }

    @Override
    public boolean mayBreak(ServerLevel level, BlockPos pos, @Nullable UUID owner) {
        var player = FakePlayerFactory.get(level, QuarryProtection.profileFor(level, owner));
        var event = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);

        NeoForge.EVENT_BUS.post(event);

        return !event.isCanceled();
    }
}
