package com.human.common.gameplay.storage;

import java.util.List;

/**
 * The six crate designs. Each family decides its colour range, the names its models and textures use, and the block
 * texture its particles borrow (the crate sheets themselves live outside the block atlas).
 */
public enum CrateFamily {

    HARDSHELL("hardshell_case", "hardshell_container", "Hardshell Case", "Hardshell Container", Palette.DYES),
    SLAB("slab_case", "slab_container", "Slab Case", "Slab Container", Palette.DYES),
    FRAMED("framed_crate", "framed_container", "Framed Crate", "Framed Container", Palette.METALS),
    TECH("tech_case", "tech_container", "Tech Case", "Tech Container", Palette.METALS),
    USCM("pallet_load", "uscm_container", "USCM Pallet Load", "USCM Container", Palette.USCM),
    WY("wy_crate", "wy_container", "W-Y Crate", "W-Y Container", Palette.WY);

    private final String crateShape;

    private final String containerShape;

    private final String crateName;

    private final String containerName;

    private final Palette palette;

    CrateFamily(String crateShape, String containerShape, String crateName, String containerName, Palette palette) {
        this.crateShape = crateShape;
        this.containerShape = containerShape;
        this.crateName = crateName;
        this.containerName = containerName;
        this.palette = palette;
    }

    public List<String> variants() {
        return palette.variants;
    }

    /** The geo model name, shared by every colour of this family at this size. */
    public String modelName(CrateSize size) {
        return size == CrateSize.CONTAINER ? containerShape : crateShape + "_" + size.suffix();
    }

    /** Block, item and texture id - they are all the same string. */
    public String id(String variant, CrateSize size) {
        return switch (this) {
            case WY -> modelName(size);
            case USCM -> size == CrateSize.CONTAINER ? containerShape : variant + "_" + modelName(size);
            default -> variant + "_" + modelName(size);
        };
    }

    public String displayName(String variant, CrateSize size) {
        var base = size == CrateSize.CONTAINER ? containerName : crateName;
        var named = palette == Palette.DYES || palette == Palette.METALS ? titleCase(variant) + " " + base : base;

        return size == CrateSize.CONTAINER ? named : named + " (" + size.suffix() + ")";
    }

    /** A block texture of the family's material, used only for breaking particles. */
    public String particleTexture(String variant) {
        return switch (palette) {
            case DYES -> variant + "_plastic";
            case METALS -> variant + "_block";
            case USCM -> "composite_block";
            case WY -> "white_plastic";
        };
    }

    public boolean isPlastic() {
        return palette == Palette.DYES;
    }

    private static String titleCase(String id) {
        var builder = new StringBuilder();

        for (var word : id.split("_")) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }

        return builder.toString();
    }

    private enum Palette {

        DYES(
            List.of(
                "white",
                "orange",
                "magenta",
                "light_blue",
                "yellow",
                "lime",
                "pink",
                "gray",
                "light_gray",
                "cyan",
                "purple",
                "blue",
                "brown",
                "green",
                "red",
                "black"
            )
        ),
        METALS(List.of("aluminum", "ferroaluminum", "steel", "titanium")),
        USCM(List.of("uscm")),
        WY(List.of("wy"));

        private final List<String> variants;

        Palette(List<String> variants) {
            this.variants = variants;
        }
    }
}
