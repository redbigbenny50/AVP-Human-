package com.human.fabric.data.lang.en_us.provider;

import com.human.common.registry.init.HumanChemicals;
import com.human.common.registry.init.HumanVaporizer;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import java.util.function.Consumer;

/** [stated] Oct 17 - names for the chem line: the xeno acids, the gas tanks and the vaporizer. */
public final class EnUsChemicalsProvider {

    public static final Consumer<FabricLanguageProvider.TranslationBuilder> CONSUMER = builder -> {
        // The acids: placed liquid, fluid (NeoForge's fluid-type name), bucket.
        builder.add(HumanChemicals.XENO_ACID_BLOCK.get(), "Xeno Acid");
        builder.add(HumanChemicals.NETHER_XENO_ACID_BLOCK.get(), "Nether Xeno Acid");
        builder.add(HumanChemicals.IRRADIATED_XENO_ACID_BLOCK.get(), "Irradiated Xeno Acid");
        builder.add("fluid_type.avp_human.xeno_acid", "Xeno Acid");
        builder.add("fluid_type.avp_human.nether_xeno_acid", "Nether Xeno Acid");
        builder.add("fluid_type.avp_human.irradiated_xeno_acid", "Irradiated Xeno Acid");
        builder.add(HumanChemicals.XENO_ACID_BUCKET.get(), "Xeno Acid Bucket");
        builder.add(HumanChemicals.NETHER_XENO_ACID_BUCKET.get(), "Nether Xeno Acid Bucket");
        builder.add(HumanChemicals.IRRADIATED_XENO_ACID_BUCKET.get(), "Irradiated Xeno Acid Bucket");

        // The tanks.
        builder.add(HumanChemicals.ALUMINUM_TANK.get(), "Aluminum Tank");
        builder.add(HumanChemicals.CRYO_TANK.get(), "Cryo Tank");
        builder.add(HumanChemicals.INFERNIUM_TANK.get(), "Infernium Tank");
        builder.add(HumanChemicals.ACIDIUM_TANK.get(), "Acidium Tank");
        builder.add(HumanChemicals.NETHER_ACIDIUM_TANK.get(), "Nether Acidium Tank");
        builder.add(HumanChemicals.IRRADIUM_TANK.get(), "Irradium Tank");

        // The vaporizer. Its screen's title is the block's name.
        builder.add(HumanVaporizer.VAPORIZER.get(), "Vaporizer");
        builder.add(HumanVaporizer.WY_VAPORIZER.get(), "Weyland-Yutani Vaporizer");
        builder.add(HumanVaporizer.USCM_VAPORIZER.get(), "USCM Vaporizer");
        builder.add(HumanVaporizer.VAPORIZER_TOP.get(), "Vaporizer");

        // [stated] Oct 17 - the distributor. Its screen's title is the block's name, kept short so the mode buttons
        // fit.
        builder.add(com.human.common.registry.init.HumanDistributor.DISTRIBUTOR.get(), "Distributor");
        builder.add(com.human.common.registry.init.HumanDistributor.WY_DISTRIBUTOR.get(), "Weyland-Yutani Distributor");
        builder.add(com.human.common.registry.init.HumanDistributor.USCM_DISTRIBUTOR.get(), "USCM Distributor");

        // The vapors, the distributor's screen and the chem thrower's selection wheel.
        builder.add("vapor.avp_human.cryo", "Cryomist");
        builder.add("vapor.avp_human.infernium", "Infernium");
        builder.add("vapor.avp_human.acidium", "Acidium");
        builder.add("vapor.avp_human.nether_acidium", "Nether Acidium");
        builder.add("vapor.avp_human.irradium", "Irradium");
        builder.add("gui.avp_human.distributor.mode.always", "ALWAYS");
        builder.add("gui.avp_human.distributor.mode.redstone", "REDSTONE");
        builder.add("gui.avp_human.distributor.mode.sensor", "SENSOR");
        builder.add("gui.avp_human.distributor.mode.always.title", "Always");
        builder.add("gui.avp_human.distributor.mode.redstone.title", "Redstone");
        builder.add("gui.avp_human.distributor.mode.sensor.title", "Sensor");
        builder.add("gui.avp_human.distributor.mode.always.desc", "Pours while it has vapor and power");
        builder.add("gui.avp_human.distributor.mode.redstone.desc", "Pours only while powered by redstone");
        builder.add("gui.avp_human.distributor.mode.sensor.desc", "Pours while a hostile is in its room, stops 5 s after it clears");
        builder.add("gui.avp_human.distributor.chamber", "Vapor Chamber");
        builder.add("gui.avp_human.distributor.chamber.amount", "%s / %s vapor");
        builder.add("gui.avp_human.distributor.status.pouring", "Pouring");
        builder.add("gui.avp_human.distributor.power", "Uses %s FE/t while pouring");
        builder.add("gui.avp_human.distributor.remote.pour", "MUTHUR: ordered to pour");
        builder.add("gui.avp_human.distributor.remote.stop", "MUTHUR: ordered to stop");
        builder.add("gui.avp_human.distributor.sensor.hostile", "Sensor: hostile in the room");
        builder.add("gui.avp_human.distributor.sensor.clear", "Sensor: room clear");
        builder.add("gui.avp_human.distributor.tank_slot", "Vapor Tank");
        builder.add("gui.avp_human.distributor.tank_slot.desc", "Used up to fill an empty chamber");
        builder.add("gui.avp_human.chem_thrower.ammo", "Load Vapor");
        builder.add("message.avp_human.chem_thrower.choose", "Empty - press reload to choose a vapor");
        builder.add("message.avp_human.chem_thrower.no_tank", "No vapor tank");
        builder.add("tooltip.avp_human.chem_thrower.unloaded", "Empty");
    };

    private EnUsChemicalsProvider() {}
}
