package com.human.client.render.cryo;

import com.human.Human;
import com.human.HumanResources;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The CRYOLINK screen's specimen icons. [stated] Oct 2: the mob's OWN model, drawn with a flat bright-green texture in
 * place of its own - the same way the net and frost layers re-draw a model - and held on one frame of its idle so an
 * adult's tail curls instead of sticking straight out.
 * <p>
 * Zero-thickness planes are DRAWN, unlike the net: [stated] Oct 2 the predalien's dreadlocks and the praetorian's crest
 * fin are built from them, and skipping them left the dreads invisible and the crest spikes floating. The flat texture
 * keeps each plane's own cut-out shape (below), so a plane shows exactly as much as it does normally.
 * </p>
 * <h2>Flat, but with the mob's own cut-outs</h2> A single solid-green texture would also fill every part the mob's
 * texture leaves see-through: a zombie's or player's hat layer, a villager's empty hat, fins and frills. So each mob
 * gets its own flat texture, made once from its real one - every pixel the original shows becomes the green, every
 * pixel it leaves clear stays clear.
 * <p>
 * Only copies the screen made itself are ever in the icon set, so no live mob is ever drawn this way.
 * </p>
 */
public final class CryoIcon {

    /** The flat colour: the screen's own bright phosphor green. */
    public static final ResourceLocation FLAT = HumanResources.location("textures/gui/cryo/specimen_flat.png");

    /** 0xAABBGGRR, as NativeImage stores it: the green 0x6EF58C, fully opaque. */
    private static final int GREEN_ABGR = 0xFF8CF56E;

    /**
     * [stated] Oct 3: below this many visible pixels a main texture does not show the mob - marines and colonists are
     * built from LAYERS (skin, outfit, armour, hair) over a near-empty base: human/base has 4 visible pixels (the
     * eyes), colonist/base has 0. Icons switch layers off, so such a mob came out as just its eyes, or nothing. A
     * texture this empty gives the whole model the solid green instead. Every real mob texture is far above this.
     */
    private static final int MIN_VISIBLE_PIXELS = 64;

    private static final Set<Entity> ICONS = Collections.newSetFromMap(new WeakHashMap<>());

    /** Original texture -> its flat-green twin. Built once per texture per session. */
    private static final Map<ResourceLocation, ResourceLocation> FLATTENED = new HashMap<>();

    private CryoIcon() {}

    public static void mark(Entity entity) {
        ICONS.add(entity);
    }

    public static boolean isIcon(Entity entity) {
        return !ICONS.isEmpty() && ICONS.contains(entity);
    }

    /** The flat-green twin of {@code original}, keeping its transparency. Falls back to plain {@link #FLAT}. */
    public static ResourceLocation flatFor(ResourceLocation original) {
        if (original == null) {
            return FLAT;
        }

        return FLATTENED.computeIfAbsent(original, CryoIcon::flatten);
    }

    private static ResourceLocation flatten(ResourceLocation original) {
        var minecraft = Minecraft.getInstance();
        var resource = minecraft.getResourceManager().getResource(original);

        if (resource.isEmpty()) {
            return FLAT;
        }

        try (var stream = resource.get().open(); var source = NativeImage.read(NativeImage.Format.RGBA, stream)) {
            var visible = 0;

            for (int y = 0; y < source.getHeight() && visible < MIN_VISIBLE_PIXELS; y++) {
                for (int x = 0; x < source.getWidth() && visible < MIN_VISIBLE_PIXELS; x++) {
                    if (source.getPixelRGBA(x, y) >>> 24 >= 26) {
                        visible++;
                    }
                }
            }

            // A layered mob's near-empty base: colour the whole model, not the few pixels the base happens to hold.
            if (visible < MIN_VISIBLE_PIXELS) {
                return FLAT;
            }

            var flat = new NativeImage(source.getWidth(), source.getHeight(), true);

            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    var alpha = source.getPixelRGBA(x, y) >>> 24;
                    flat.setPixelRGBA(x, y, alpha < 26 ? 0 : GREEN_ABGR);
                }
            }

            var location = HumanResources.location("cryo_icon/" + original.getNamespace() + "/" + original.getPath());
            minecraft.getTextureManager().register(location, new DynamicTexture(flat));
            return location;
        } catch (Exception exception) {
            Human.LOGGER.warn("[Cryo] Could not build a flat icon texture from {}: {}", original, exception.toString());
            return FLAT;
        }
    }

    /** After a resource reload the twins are rebuilt from whatever textures are now in play. */
    public static void clear() {
        FLATTENED.clear();
    }
}
