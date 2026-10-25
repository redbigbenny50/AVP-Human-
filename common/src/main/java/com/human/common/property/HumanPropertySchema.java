package com.human.common.property;

import com.blib.api.common.property.v1.BLibPropertySchema;

public class HumanPropertySchema {

    static final BLibPropertySchema SCHEMA = BLibPropertySchema.builder()
        .withPropertyValueAlignment(true)
        .addComment("If enabled, nukes will explode on dedicated servers.")
        .addComment("By default, nukes are always enabled on singleplayer.")
        .addProperty(
            HumanProperties.Blocks.Nuke.ENABLED.key(),
            HumanProperties.Blocks.Nuke.ENABLED.defaultValue()
        )
        .addComment("Maximum cloudlets rendered for one mushroom cloud effect.")
        .addProperty(
            HumanProperties.Blocks.Nuke.CLOUD_PARTICLE_BUDGET.key(),
            HumanProperties.Blocks.Nuke.CLOUD_PARTICLE_BUDGET.defaultValue()
        )
        .addComment("Client screen flash intensity for nearby nuclear explosions. Set to 0 to disable.")
        .addProperty(
            HumanProperties.Blocks.Nuke.SCREEN_FLASH_INTENSITY.key(),
            HumanProperties.Blocks.Nuke.SCREEN_FLASH_INTENSITY.defaultValue()
        )
        .addComment("Client screen shake intensity for nearby nuclear explosions. Set to 0 to disable.")
        .addProperty(
            HumanProperties.Blocks.Nuke.SCREEN_SHAKE_INTENSITY.key(),
            HumanProperties.Blocks.Nuke.SCREEN_SHAKE_INTENSITY.defaultValue()
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
        .addComment("Create converters (NeoForge with Create only). FE per tick for each RPM of the alternator's input.")
        .addProperty(
            HumanProperties.Blocks.CreateConverters.ALTERNATOR_FE_PER_RPM.key(),
            HumanProperties.Blocks.CreateConverters.ALTERNATOR_FE_PER_RPM.defaultValue()
        )
        .addComment("FE per tick the electric gearbox draws for each RPM it turns at.")
        .addProperty(
            HumanProperties.Blocks.CreateConverters.GEARBOX_FE_PER_RPM.key(),
            HumanProperties.Blocks.CreateConverters.GEARBOX_FE_PER_RPM.defaultValue()
        )
        .addComment("The least FE per tick a running electric gearbox draws.")
        .addProperty(
            HumanProperties.Blocks.CreateConverters.GEARBOX_MIN_DRAW.key(),
            HumanProperties.Blocks.CreateConverters.GEARBOX_MIN_DRAW.defaultValue()
        )
        .addComment("Stress units per RPM: the alternator's load on Create, and the gearbox's capacity.")
        .addProperty(
            HumanProperties.Blocks.CreateConverters.STRESS_PER_RPM.key(),
            HumanProperties.Blocks.CreateConverters.STRESS_PER_RPM.defaultValue()
        )
        .addComment("FE each converter holds internally.")
        .addProperty(
            HumanProperties.Blocks.CreateConverters.BUFFER.key(),
            HumanProperties.Blocks.CreateConverters.BUFFER.defaultValue()
        )
        .addComment("If disabled, speed and efficiency cards do nothing in an alternator.")
        .addProperty(
            HumanProperties.Blocks.CreateConverters.ALTERNATOR_UPGRADES.key(),
            HumanProperties.Blocks.CreateConverters.ALTERNATOR_UPGRADES.defaultValue()
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
