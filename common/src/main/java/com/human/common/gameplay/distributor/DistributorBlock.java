package com.human.common.gameplay.distributor;

import com.human.common.gameplay.block.power.machine.ElectricMachineBlock;
import com.human.common.gameplay.machine.ElectricMachineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The distributor block: a tier-1 machine in every way ([stated] Oct 17 - same family as the press, hydrator, freezer
 * and grinder), whose block entity is the {@link DistributorBlockEntity}. Facing, the light states, the screen, buckets
 * on the block, the back breaker and spilling its contents when broken are all the shared machine block's.
 */
public class DistributorBlock extends ElectricMachineBlock {

    public DistributorBlock(Properties properties) {
        super(properties, ElectricMachineKind.DISTRIBUTOR);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new DistributorBlockEntity(blockPos, blockState);
    }
}
