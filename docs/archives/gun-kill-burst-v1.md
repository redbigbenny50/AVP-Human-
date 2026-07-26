# Gun Kill Burst v1 Archive

Archived before the Ultrakill-inspired redesign requested on 2026-07-25.

## Scope

This version is the active custom geometry kill effect implemented by:

- `common/src/main/java/com/human/client/effect/VoxelGunEffects.java`
- `common/src/main/java/com/human/common/network/packet/S2CGunKillEffectPayload.java`
- `common/src/main/java/com/human/common/gameplay/item/gun/attack/hitscan/HitScanGunAttackAction.java`

## Behavior

- Triggers only on confirmed lethal conventional-gun hits.
- Uses the exact bullet/AABB intersection point.
- Renders a brief radial burst of gravity-affected, intersecting flat droplets.
- Uses red for ordinary mobs, acid green for AVP-Alien entities, and blue-white for synthetic/robot-type entities.
- Uses a larger burst for shotgun kills.

## Restore point

Restore the `triggerKillEffect`, `KillDroplet`, and `S2CGunKillEffectPayload` implementation from the revision containing this archive if the later blood-spray redesign needs to be rolled back.
