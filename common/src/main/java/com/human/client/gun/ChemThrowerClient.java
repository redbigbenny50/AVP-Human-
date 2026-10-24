package com.human.client.gun;

import com.human.Human;
import com.human.client.screen.ChemWheelScreen;
import com.human.common.gameplay.item.gun.chem.ChemCone;
import com.human.common.gameplay.item.gun.chem.ChemThrowerReloading;
import com.human.common.gameplay.vapor.VaporKind;
import com.human.common.network.packet.C2SGunReloadPayload;
import com.human.common.network.packet.S2CChemBlastPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The chem thrower on the client: the reload key, the selection wheel, and drawing the blast.
 * <h2>RELOAD - [stated] Oct 18</h2>
 * <ul>
 * <li>A tank on the gun: the key goes to the server, which plays remove + purge and then either loads the only kind
 * carried or tells this client to open the wheel ({@link #onOpenWheel}).</li>
 * <li>An EMPTY gun with more than one kind carried: the key opens the wheel here and now
 * ({@link ChemWheelScreen}).</li>
 * <li>[stated] The wheel only ever opens from the reload key (or straight after a purge). Switching item closes it; the
 * gun stays empty until the key is pressed again - so it can be carried empty, ready, with no menu in the way.</li>
 * </ul>
 * <h2>THE BLAST</h2> The server says, every tick of firing, where the blast is ({@link S2CChemBlastPayload}); this
 * scatters dust in the vapor's colour through the cone - 1 block wide at the nozzle, 3 x 3 at the end, fading at the
 * edges like the distributor's fog. Denser than the fog ([stated] 80 % against 45 %). Placeholder, like the fog, until
 * the real vapor visuals.
 */
public final class ChemThrowerClient {

    private static final int VIEW_DISTANCE = 64;

    private static final int PUFFS_PER_TICK = 14;

    private ChemThrowerClient() {}

    public static void onReloadKey(Player player, ItemStack gun) {
        // Mid-reload (the remove/purge or the insert is still playing): the key does nothing.
        if (player.getCooldowns().isOnCooldown(gun.getItem())) {
            return;
        }

        if (ChemThrowerReloading.onGun(gun) == null && ChemThrowerReloading.needsChoice(player)) {
            openWheel(player, gun);
            return;
        }

        Human.MOD.networking().sendToServer(C2SGunReloadPayload.INSTANCE);
    }

    /** The server finished a purge and there is a choice to make. Only if the emptied gun is still in hand. */
    public static void onOpenWheel() {
        var player = Minecraft.getInstance().player;

        if (player != null) {
            openWheel(player, player.getMainHandItem());
        }
    }

    private static void openWheel(Player player, ItemStack gun) {
        var minecraft = Minecraft.getInstance();

        if (minecraft.screen != null || !ChemThrowerReloading.isChemThrower(gun) || ChemThrowerReloading.onGun(gun) != null) {
            return;
        }

        if (ChemThrowerReloading.choiceKinds(player).size() < 2) {
            return;
        }

        minecraft.setScreen(new ChemWheelScreen());
    }

    public static void onBlast(S2CChemBlastPayload payload) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var kind = VaporKind.byOrdinal(payload.vapor());

        if (level == null || minecraft.player == null || kind == null) {
            return;
        }

        // [stated] Oct 19 - the spray's sound: fades in, loops while it is held, fades out on release.
        com.human.client.sound.ChemThrowerSound.onBlast(payload.shooter());

        var nozzle = new Vec3(payload.x(), payload.y(), payload.z());

        if (minecraft.player.position().distanceToSqr(nozzle) > VIEW_DISTANCE * VIEW_DISTANCE) {
            return;
        }

        var direction = new Vec3(payload.dx(), payload.dy(), payload.dz()).normalize();
        var side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));

        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }

        side = side.normalize();

        var up = side.cross(direction).normalize();
        var look = com.human.common.gameplay.vapor.VaporLook.of(kind);
        var random = level.random;
        var reach = Math.max(0.0D, payload.reach());

        for (var i = 0; i < PUFFS_PER_TICK; i++) {
            var along = random.nextDouble() * reach;
            var half = ChemCone.halfAt(along, payload.farHalf());
            var u = (random.nextDouble() * 2.0D - 1.0D) * half;
            var v = (random.nextDouble() * 2.0D - 1.0D) * half;
            var point = nozzle.add(direction.scale(along)).add(side.scale(u)).add(up.scale(v));
            var strength = ChemCone.strengthAt(point, nozzle, direction, reach, payload.farHalf(), 0.0D);

            if (random.nextFloat() >= strength) {
                continue;
            }

            // [stated] Oct 18 - each vapor's own mix: one style near the nozzle, another further out.
            var style = look.blastStyle(along, random.nextFloat());
            var puff = com.human.common.gameplay.vapor.VaporParticleOptions.of(kind, style, 0.35F, 1.1F, 0);

            level.addParticle(
                puff,
                point.x,
                point.y,
                point.z,
                direction.x * 0.15D + (random.nextDouble() - 0.5D) * 0.03D,
                direction.y * 0.15D + (random.nextDouble() - 0.5D) * 0.03D,
                direction.z * 0.15D + (random.nextDouble() - 0.5D) * 0.03D
            );
        }
    }
}
