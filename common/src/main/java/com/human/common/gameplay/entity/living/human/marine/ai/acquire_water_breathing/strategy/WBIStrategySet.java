package com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy;

import com.human.common.gameplay.entity.ai.utility.item.ItemStrategySet;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy.impl.DrinkablePotionWBIStrategy;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy.impl.SplashPotionWBIStrategy;

import java.util.List;

/**
 * Drinkable and splash only. A lingering cloud does nothing to a swimmer - the cloud sits on a surface and the marine
 * is either floating above it or sinking through it - so there is no lingering strategy here, unlike fire resistance.
 */
public class WBIStrategySet implements ItemStrategySet<WBIStrategy> {

    public static final WBIStrategySet INSTANCE = new WBIStrategySet();

    private final List<WBIStrategy> strategies;

    private WBIStrategySet() {
        this.strategies = List.of(
            new DrinkablePotionWBIStrategy(),
            new SplashPotionWBIStrategy()
        );
    }

    @Override
    public List<WBIStrategy> getAll() {
        return strategies;
    }
}
