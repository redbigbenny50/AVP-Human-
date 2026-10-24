package com.human.fabric.quarry;

import com.human.common.gameplay.quarry.QuarryProtection;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The quarry's permission check on Fabric: Fabric API's {@link PlayerBlockBreakEvents#BEFORE} for a fake player
 * carrying the owner's id. Claim mods that listen to it refuse inside claims the owner may not break in.
 * <p>
 * ⚠ Mods that hook block breaking through ARCHITECTURY's own event instead (FTB Chunks does, on Fabric) are not reached
 * by this; covering them needs the Architectury jar to compile against.
 * </p>
 */
public final class FabricQuarryProtection implements QuarryProtection {

    private FabricQuarryProtection() {}

    public static void install() {
        QuarryProtection.Holder.install(new FabricQuarryProtection());
    }

    @Override
    public boolean mayBreak(ServerLevel level, BlockPos pos, @Nullable UUID owner) {
        var player = FakePlayer.get(level, QuarryProtection.profileFor(level, owner));
        var state = level.getBlockState(pos);

        return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, level.getBlockEntity(pos));
    }
}
