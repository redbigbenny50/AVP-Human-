package com.human.fabric.data.damage_type;

import com.human.common.registry.key.HumanDamageTypeKeys;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageType;

public class DamageTypeBootstrapper {

    public static void bootstrap(BootstrapContext<DamageType> registry) {
        registry.register(HumanDamageTypeKeys.BULLET, new DamageType("bullet", 0.1F));
        registry.register(HumanDamageTypeKeys.FLAMETHROW, new DamageType("flamethrow", 0.1F));
        registry.register(HumanDamageTypeKeys.RAZOR_WIRE, new DamageType("razor_wire", 0.1F));
        registry.register(HumanDamageTypeKeys.RADIATION, new DamageType("radiation", 0.1F));
        registry.register(HumanDamageTypeKeys.CRYOFLUID, new DamageType("cryofluid", 0.0F));
    }
}
