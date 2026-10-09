package com.human.common.gameplay.block.pipe;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Who may squeeze through pipes and cables instead of being blocked by them. [stated] Sep 30: players - and then
 * "leashed animals the player is pulling, hired npcs like marines, and pets should also be allowed through". Everyone
 * who passes is slowed while inside; every other mob collides as before.
 */
public final class PipePassage {

    private PipePassage() {}

    public static boolean passes(@Nullable Entity entity) {
        if (entity instanceof Player) {
            return true;
        }

        // A mob on a lead a player is holding.
        if (entity instanceof Mob mob && mob.getLeashHolder() instanceof Player) {
            return true;
        }

        // Pets: any animal with an owner - tamed wolves (the marine dog), cats, parrots, tamed horses.
        if (entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null) {
            return true;
        }

        // A marine a player has hired - its leader is only ever the hiring player (a squad leader is kept apart).
        return entity instanceof Marine marine && marine.hasLeader();
    }

    /** Flying or spectating players are not slowed. */
    public static boolean slowed(Entity entity) {
        return !(entity instanceof Player player) || !player.isSpectator() && !player.getAbilities().flying;
    }
}
