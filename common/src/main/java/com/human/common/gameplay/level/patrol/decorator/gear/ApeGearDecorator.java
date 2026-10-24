package com.human.common.gameplay.level.patrol.decorator.gear;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.level.patrol.decorator.MarineDecorator;
import com.human.common.gameplay.level.patrol.decorator.util.MarineGearDecoratorUtil;
import com.human.common.registry.init.item.HumanArmorItems;
import com.human.common.registry.init.item.HumanGunItems;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;

public class ApeGearDecorator implements MarineDecorator {

    public static final ApeGearDecorator INSTANCE = new ApeGearDecorator();

    private ApeGearDecorator() {}

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
        MarineGearDecoratorUtil.equipArmorItem(marine, HumanArmorItems.WY_APE_HELMET.get());
        MarineGearDecoratorUtil.equipArmorItem(marine, HumanArmorItems.WY_APE_CHESTPLATE.get());
        MarineGearDecoratorUtil.equipArmorItem(marine, HumanArmorItems.WY_APE_LEGGINGS.get());
        MarineGearDecoratorUtil.equipArmorItem(marine, HumanArmorItems.WY_APE_BOOTS.get());
    }

    public void applyExclusives(Marine marine) {
        MarineGearDecoratorUtil.giveItem(marine, HumanGunItems.FLAMETHROWER_SEVASTOPOL.get());
    }

    public void applyExtras(Marine marine) {
        // [stated] Oct 13 kit clean-up: the grenade only - no longer the tactical marine's 128 torches and water
        // bucket.
        MarineGearDecorator.INSTANCE.applyExtras(marine);

        for (var i = 0; i < 2; i++) {
            var healingPotion = PotionContents.createItemStack(Items.POTION, Potions.FIRE_RESISTANCE);
            MarineGearDecoratorUtil.giveItem(marine, healingPotion);

            var regenerationPotion = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.FIRE_RESISTANCE);
            MarineGearDecoratorUtil.giveItem(marine, regenerationPotion);
        }
    }

    public void applyPrimaryWeapon(Marine marine) {
        MarineGearDecoratorUtil.giveItem(marine, HumanGunItems.M41A_PULSE_RIFLE.get());
    }

    public void applySecondaryWeapon(Marine marine) {
        TacticalMarineGearDecorator.INSTANCE.applySecondaryWeapon(marine);
    }

    public void applyMeleeWeapon(Marine marine) {
        MarineGearDecoratorUtil.giveItem(marine, HumanItems.TACTICAL_KNIFE.get());
    }
}
