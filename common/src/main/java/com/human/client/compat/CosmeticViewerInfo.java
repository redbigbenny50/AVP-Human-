package com.human.client.compat;

import com.blib.api.client.viewer.v1.BLibViewers;
import com.blib.api.client.viewer.v1.ViewerInfo;
import com.human.Human;
import com.human.common.gameplay.cosmetic.CosmeticBlock;
import com.human.common.gameplay.cosmetic.CosmeticBunks;
import com.human.common.gameplay.cosmetic.CosmeticCatalog;
import com.human.common.gameplay.cosmetic.CosmeticDesign;
import com.human.common.gameplay.cosmetic.CosmeticModelTables;
import com.human.common.gameplay.cosmetic.CosmeticStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The cosmetic furniture's information pages - what right-clicking a piece does - registered ONCE through BLib's viewer
 * bridge, which shows each as JEI's, REI's and EMI's own native information page. [stated] Oct 24: every new block is
 * recognised by all three viewers. (These used to be added to the JEI and EMI plugins only, so REI - the viewer this
 * mod actually wires - showed none of them.)
 * <p>
 * The crafter's recipe tab stays in each viewer's own plugin: a recipe category is not an information page.
 * </p>
 */
public final class CosmeticViewerInfo {

    private CosmeticViewerInfo() {}

    public static void register() {
        page("cosmetic_contents", CosmeticCatalog::specimenStacks, "contents");
        page("cosmetic_dye", designs(CosmeticBlock::dyeable), "dye");
        page("cosmetic_motion", designs(design -> CosmeticBlock.hasDoors(design) || CosmeticBlock.spins(design)), "motion");
        page("cosmetic_mount", designs(design -> design.mount() == CosmeticDesign.Mount.WALL), "mount");
        page(
            "cosmetic_rack",
            designs(
                design -> CosmeticModelTables.SLOTS.containsKey(design.id())
                    && !CosmeticModelTables.SLOT_BONES.containsKey(design.id())
            ),
            "rack"
        );
        page("cosmetic_shelf", designs(design -> CosmeticModelTables.SLOT_BONES.containsKey(design.id())), "shelf");
        page("cosmetic_seat", designs(design -> CosmeticModelTables.SEATS.containsKey(design.id())), "seat");
        page("cosmetic_bunk", designs(CosmeticBunks::isBunk), "bunk");
        page("cosmetic_holo", designs(design -> design == CosmeticDesign.MAPPING_TABLE), "holo");
        page("cosmetic_wall_map", designs(design -> design == CosmeticDesign.WALL_DISPLAY_ARRAY), "wall_map");
        page("cosmetic_table_map", designs(design -> design == CosmeticDesign.BRIEFING_TABLE), "table_map");
        page("cosmetic_storage", designs(CosmeticStorage::has), "storage");
    }

    private static Supplier<List<ItemStack>> designs(Predicate<CosmeticDesign> test) {
        return () -> CosmeticCatalog.stacksWhere(test);
    }

    private static void page(String id, Supplier<List<ItemStack>> items, String key) {
        BLibViewers.registerInfo(
            new ViewerInfo(
                ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, id),
                items,
                List.of(Component.translatable("info.avp_human.cosmetic." + key))
            )
        );
    }
}
