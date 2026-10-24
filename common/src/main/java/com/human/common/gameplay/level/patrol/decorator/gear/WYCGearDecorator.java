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
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class WYCGearDecorator implements MarineDecorator {

    public static final WYCGearDecorator INSTANCE = new WYCGearDecorator();

    private final List<Tuple2<Integer, Supplier<Item>>> weightedPrimaryWeapons;

    private WYCGearDecorator() {
        var base = new ArrayList<>(TacticalMarineGearDecorator.INSTANCE.getWeightedPrimaryWeapons());

        base.add(new Tuple2<>(40, HumanGunItems.M41A_PULSE_RIFLE));

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
        MarineGearDecoratorUtil.equipArmorItem(marine, HumanArmorItems.WY_COMMANDO_HELMET.get());
        MarineGearDecoratorUtil.equipArmorItem(marine, HumanArmorItems.WY_COMMANDO_CHESTPLATE.get());
        MarineGearDecoratorUtil.equipArmorItem(marine, HumanArmorItems.WY_COMMANDO_LEGGINGS.get());
        MarineGearDecoratorUtil.equipArmorItem(marine, HumanArmorItems.WY_COMMANDO_BOOTS.get());
    }

    public void applyExclusives(Marine marine) {}

    public void applyExtras(Marine marine) {
        // [stated] Oct 13 kit clean-up: the grenade, not the APE's extras - no torches or water bucket, and fire
        // resistance stays with the APE (it goes with the flamethrower). The elite and both spec ops build on this.
        MarineGearDecorator.INSTANCE.applyExtras(marine);

        MarineGearDecoratorUtil.giveItem(marine, Items.TOTEM_OF_UNDYING);
        MarineGearDecoratorUtil.giveItem(marine, Items.GOLDEN_APPLE, 4);

        var healingPotion = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING);
        MarineGearDecoratorUtil.giveItem(marine, healingPotion);

        var regenerationPotion = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.REGENERATION);
        MarineGearDecoratorUtil.giveItem(marine, regenerationPotion);
    }

    public void applyPrimaryWeapon(Marine marine) {
        MarineGearDecoratorUtil.giveWeightedItemFromPool(marine, weightedPrimaryWeapons);
    }

    public void applySecondaryWeapon(Marine marine) {
        ApeGearDecorator.INSTANCE.applySecondaryWeapon(marine);
    }

    public void applyMeleeWeapon(Marine marine) {
        MarineGearDecoratorUtil.giveItem(marine, HumanItems.TACTICAL_KNIFE.get());
    }
}
