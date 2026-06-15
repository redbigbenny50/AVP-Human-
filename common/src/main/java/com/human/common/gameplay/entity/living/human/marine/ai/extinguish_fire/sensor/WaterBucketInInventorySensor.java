package com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire.sensor;

import com.blib.api.common.inventory.v1.BLibInventory;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.state.ReadableWorldState;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class WaterBucketInInventorySensor {

    public static final StateKey.Sensed<Option<BLibInventory.Entry>> KEY = StateKey.sensed("water_bucket_in_inventory");

    public static <T extends LivingEntity & BLibInventoryHolder> @NotNull Option<BLibInventory.Entry> sense(
        T livingEntityWithInventory,
        ReadableWorldState worldState
    ) {
        var waterBucketEntries = livingEntityWithInventory.getInventory().selectEntries(Items.WATER_BUCKET);
        return waterBucketEntries.isEmpty()
            ? Option.none()
            : Option.ofNullable(waterBucketEntries.stream().findFirst().orElse(null));
    }

}
