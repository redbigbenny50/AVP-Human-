package com.human.common.property;

import com.blib.api.common.property.v1.BLibPropertySchema;

public class HumanPropertySchema {

    static final BLibPropertySchema SCHEMA = BLibPropertySchema.builder()
        .withPropertyValueAlignment(true)
        .addComment("Radius in blocks a Resonator will look for resin blocks.")
        .addProperty(
            HumanProperties.Blocks.Resonator.REPLACE_FREQUENCY_IN_TICKS.key(),
            HumanProperties.Blocks.Resonator.REPLACE_FREQUENCY_IN_TICKS.defaultValue()
        )
        .addComment("Resonator will replace resin N ticks, where this value is N.")
        .addComment("1 second is 20 ticks. Default of 30 seconds.")
        .addProperty(
            HumanProperties.Blocks.Resonator.REPLACE_RADIUS_IN_BLOCKS.key(),
            HumanProperties.Blocks.Resonator.REPLACE_RADIUS_IN_BLOCKS.defaultValue()
        )
        .addBlankLine()
        .addComment("If enabled, nukes will explode on dedicated servers.")
        .addComment("By default, nukes are always enabled on singleplayer.")
        .addProperty(
            HumanProperties.Blocks.Nuke.ENABLED.key(),
            HumanProperties.Blocks.Nuke.ENABLED.defaultValue()
        )
        .addBlankLine()
        .addComment("Block radius that a turret looks for an ammo chest.")
        .addProperty(
            HumanProperties.Blocks.SentryTurret.AMMO_CHEST_RANGE.key(),
            HumanProperties.Blocks.SentryTurret.AMMO_CHEST_RANGE.defaultValue()
        )
        .addComment("Turret damage value.")
        .addProperty(
            HumanProperties.Blocks.SentryTurret.DAMAGE.key(),
            HumanProperties.Blocks.SentryTurret.DAMAGE.defaultValue()
        )
        .addComment("FOV range that turrets can target.")
        .addProperty(
            HumanProperties.Blocks.SentryTurret.FOV.key(),
            HumanProperties.Blocks.SentryTurret.FOV.defaultValue()
        )
        .addComment("Block range that turrets can target.")
        .addProperty(
            HumanProperties.Blocks.SentryTurret.RANGE.key(),
            HumanProperties.Blocks.SentryTurret.RANGE.defaultValue()
        )
        .addBlankLine()
        .addComment("If enabled, bullets from guns will damage blocks.")
        .addProperty(
            HumanProperties.Weapons.BULLETS_DAMAGE_BLOCKS_ENABLED.key(),
            HumanProperties.Weapons.BULLETS_DAMAGE_BLOCKS_ENABLED.defaultValue()
        )
        .build();

    private HumanPropertySchema() {
        throw new UnsupportedOperationException();
    }
}
