package com.human.common.gameplay.entity.living.human.soldier;

import com.human.common.gameplay.fabrication.Faction;
import com.human.common.gameplay.level.patrol.decorator.MarineDecorator;

/**
 * What one soldier entity type is ([stated] Oct 13): its faction, the kit it spawns in, and which body it has under the
 * armour.
 *
 * @param faction      USCM or Weyland-Yutani
 * @param kit          the gear it spawns in - from a spawn egg or a command as much as from a patrol
 * @param employeeBody true for the Weyland-Yutani units, which wear the {@code weyland_yutani_employee} body (the
 *                     colonist's model, [stated] "the colonist is a better match than the marine"); false for the
 *                     marine body
 */
public record SoldierProfile(
    Faction faction,
    MarineDecorator kit,
    boolean employeeBody
) {}
