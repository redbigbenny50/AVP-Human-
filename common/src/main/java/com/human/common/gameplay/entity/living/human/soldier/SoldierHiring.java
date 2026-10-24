package com.human.common.gameplay.entity.living.human.soldier;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.fabrication.Faction;
import net.minecraft.world.entity.player.Player;

/**
 * Who a player may hire ([stated] Oct 13): "you can buy special contracts with the wy faction - until then they aren't
 * hireable". USCM soldiers are hired as always; Weyland-Yutani soldiers refuse.
 * <p>
 * The single place hiring is decided, so W-Y contracts plug in here when they exist.
 * </p>
 */
public final class SoldierHiring {

    public static final String CONTRACT_ONLY = "message.avp_human.soldier.contract_only";

    private SoldierHiring() {}

    public static boolean canHire(Marine soldier, Player player) {
        return soldier.faction() == Faction.USCM;
    }
}
