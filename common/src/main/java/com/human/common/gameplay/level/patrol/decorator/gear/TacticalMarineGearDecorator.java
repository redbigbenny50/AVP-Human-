package com.human.common.gameplay.level.patrol.decorator.gear;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.level.patrol.decorator.MarineDecorator;
import com.human.common.gameplay.level.patrol.decorator.util.MarineGearDecoratorUtil;
import com.human.common.registry.init.item.HumanArmorItems;
import com.human.common.registry.init.item.HumanGunItems;
import com.human.common.registry.init.item.HumanItems;
import com.just.core.functional.tuple.Tuple2;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TacticalMarineGearDecorator implements MarineDecorator {

    public static final TacticalMarineGearDecorator INSTANCE = new TacticalMarineGearDecorator();

    private final List<Tuple2<Integer, Supplier<Item>>> weightedPrimaryWeapons;

    private TacticalMarineGearDecorator() {
        var base = new ArrayList<>(MarineGearDecorator.INSTANCE.getWeightedPrimaryWeapons());

        base.add(new Tuple2<>(15, HumanGunItems.M42A3_SNIPER_RIFLE));

        this.weightedPrimaryWeapons = List.copyOf(base);
    }

    @Override
    public void decorate(Level level, Marine marine) {
        applyArmor(marine);
        applyPrimaryWeapon(marine);
        applySecondaryWeapon(marine);
        applyMeleeWeapon(marine);
        applyExtras(marine);
        applyExclusives(marine);
    }

    public void applyArmor(Marine marine) {
        MarineGearDecoratorUtil.equipArmorItem(marine, HumanArmorItems.TACTICAL_CAMO_HELMET.get());
        MarineGearDecoratorUtil.equipArmorItem(marine, HumanArmorItems.TACTICAL_CAMO_CHESTPLATE.get());
        MarineGearDecoratorUtil.equipArmorItem(marine, HumanArmorItems.TACTICAL_CAMO_LEGGINGS.get());
        MarineGearDecoratorUtil.equipArmorItem(marine, HumanArmorItems.TACTICAL_CAMO_BOOTS.get());
    }

    public void applyExclusives(Marine marine) {
        MarineGearDecoratorUtil.giveDroppableItem(marine, HumanItems.DOG_TAG.get());
        MarineGearDecoratorUtil.giveItem(marine, HumanItems.CORNBREAD.get(), 5 + marine.getRandom().nextInt(10));
        // [stated] Oct 27 - the dog handlers: bones are how a tactical marine recruits a wild wolf.
        MarineGearDecoratorUtil.giveItem(marine, Items.BONE, 7 + marine.getRandom().nextInt(12));
    }

    public void applyExtras(Marine marine) {
        MarineGearDecorator.INSTANCE.applyExtras(marine);

        MarineGearDecoratorUtil.giveItem(marine, Items.TORCH, 128);
        MarineGearDecoratorUtil.giveItem(marine, Items.WATER_BUCKET);
    }

    public void applyPrimaryWeapon(Marine marine) {
        MarineGearDecoratorUtil.giveWeightedItemFromPool(marine, weightedPrimaryWeapons);
    }

    public void applySecondaryWeapon(Marine marine) {
        MarineGearDecorator.INSTANCE.applySecondaryWeapon(marine);
    }

    public void applyMeleeWeapon(Marine marine) {
        MarineGearDecoratorUtil.giveItem(marine, HumanItems.TACTICAL_KNIFE.get());
    }

    public List<Tuple2<Integer, Supplier<Item>>> getWeightedPrimaryWeapons() {
        return weightedPrimaryWeapons;
    }
}
