package com.human.client.render.item.gun;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;
import com.human.client.animation.item.ChemThrowerAnimator;
import com.human.client.render.layer.AzOverlayTextureLayer;
import com.human.common.gameplay.item.gun.chem.ChemThrowerReloading;
import com.human.common.gameplay.vapor.VaporKind;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The chem thrower: his model ({@code chem_thrower.geo.json}) with the player-arm rig, his clips, and the CANISTER SKIN
 * of whatever vapor is loaded.
 * <h2>THE ARMS</h2> Nothing to do here: the rig's {@code leftArm}/{@code rightArm} bones are drawn as the player's own
 * skin arms by BLib in first person while a clip plays, and hidden everywhere else ({@code AzItemRendererPipeline}
 * hides both arm bones every frame) - the same path as the M42A3 sniper and M4RA battle rifle, the two avp_human guns
 * that already carry the rig. ⚠ So the arm boxes' own cubes never show in the GUI, the hotbar or third person.
 * <h2>THE CANISTER SKIN</h2> {@code cryomist_tank.png} is a full 256 x 128 sheet in the gun's own UV space that is
 * transparent everywhere except the propane canister. Redrawing the gun with it (the overlay layer, cutout) paints only
 * the canister - blue for cryo. Each vapor has its own ({@code textures/item/chem_thrower/<skin>.png}). [stated] Oct
 * 18: the gun's own texture has NO canister, so with no ammo nothing is drawn there and it looks empty. During a reload
 * the skin follows the clips: the old tank through remove and purge, the new one from the insert
 * ({@link ChemThrowerReloading#visibleTank}).
 * <h2>FROZEN OUTSIDE THE HANDS</h2> As the flamethrower: animated only in the four hand views, so the GUI icon and a
 * dropped gun do not move.
 */
public class ChemThrowerItemRenderer extends AzItemRenderer {

    private static final Map<VaporKind, ResourceLocation> SKINS = new EnumMap<>(VaporKind.class);

    private static final Map<VaporKind, Boolean> SKIN_EXISTS = new EnumMap<>(VaporKind.class);

    public ChemThrowerItemRenderer(String name) {
        super(
            AzItemRendererConfig.builder(HumanResources.itemGeoModelLocation(name), HumanResources.itemTextureLocation(name))
                .setAnimatorProvider(ChemThrowerAnimator::new)
                // ⚠ Like the flamethrower, this does not extend MuzzledGunItemRenderer, so it needs this line itself.
                .setShouldAnimateInContext(ChemThrowerItemRenderer::animatesInContext)
                .addRenderLayer(new AzOverlayTextureLayer<>(ChemThrowerItemRenderer::canisterSkin, false))
                .useNewOffset(true)
                .build()
        );
    }

    /**
     * ⚠ Oct 21 (tester: in third person the chem thrower is "held by the bottom tank ... its supposed to hold it from
     * the handle"). His clips are framed for the FIRST-PERSON camera: even the idle carries the whole gun 3 px left, 5
     * px up and 6 px forward (bone gHose2). Played in third person that lifted the gun until the hand sat on the bottom
     * canister. His display (taken from his .bbmodel) is authored on the gun at REST, so third person now shows it at
     * rest - exactly as his Blockbench preview - and the clips play in first person only.
     */
    private static boolean animatesInContext(ItemDisplayContext transformType) {
        return switch (transformType) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> true;
            default -> false;
        };
    }

    /** The loaded vapor's canister skin, or null (no overlay) for an unloaded gun or a vapor without one. */
    private static @Nullable ResourceLocation canisterSkin(ItemStack stack) {
        var level = Minecraft.getInstance().level;

        if (level == null) {
            return null;
        }

        // [stated] no ammo = no tank; the old tank stays on through remove and purge, the new one from insert.
        var kind = ChemThrowerReloading.visibleTank(stack, level);

        if (kind == null) {
            return null;
        }

        var location = SKINS.computeIfAbsent(kind, key -> HumanResources.location("textures/item/chem_thrower/" + skinName(key) + ".png"));
        var exists = SKIN_EXISTS.computeIfAbsent(
            kind,
            key -> Minecraft.getInstance().getResourceManager().getResource(location).isPresent()
        );

        return exists ? location : null;
    }

    /** His cryo skin is named for the cryomist it carries; the rest follow "<vapor>_tank" until he names them. */
    private static String skinName(VaporKind kind) {
        return kind == VaporKind.CRYO ? "cryomist_tank" : kind.id() + "_tank";
    }
}
