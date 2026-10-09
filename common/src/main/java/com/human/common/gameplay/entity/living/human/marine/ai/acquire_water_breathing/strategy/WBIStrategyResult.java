package com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy;

import com.human.common.gameplay.entity.ai.utility.item.ItemStrategyResult;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;

public record WBIStrategyResult<T extends ItemTarget>(
    T itemTarget,
    WBIStrategy strategy,
    double score
) implements ItemStrategyResult<T, WBIStrategy> {}
