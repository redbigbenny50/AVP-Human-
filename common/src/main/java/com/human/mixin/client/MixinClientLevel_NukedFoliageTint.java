package com.human.mixin.client;

import com.human.client.effect.NukedFoliageTint;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives {@link NukedFoliageTint#FALLOUT_WEIGHT_RESOLVER} a tint cache, so it can be used at all.
 * <p>
 * {@code ClientLevel.getBlockTint} looks its cache up with {@code tintCaches.get(resolver)} and does not null-check the
 * result, so an unregistered resolver is not merely uncached - it throws. Vanilla only ever puts three in (grass,
 * foliage, water). NeoForge exposes {@code ColorResolverManager} for this, but avp_human is multiloader and Fabric has
 * no equivalent, so registering here covers both loaders with one file.
 * </p>
 * <p>
 * Injected at the END of the constructor: the map is built in a field initializer, so it exists by then. Using
 * vanilla's own cache also means our weights are invalidated by the same chunk-reload and biome-resend paths - which
 * this feature depends on, since a nuke rewrites biomes while players are watching.
 * </p>
 */
@Mixin(ClientLevel.class)
public abstract class MixinClientLevel_NukedFoliageTint {

    @Shadow
    @Final
    private Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches;

    @Shadow
    public abstract int calculateBlockTint(net.minecraft.core.BlockPos blockPos, ColorResolver colorResolver);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void avp_human$registerFalloutTintCache(CallbackInfo callbackInfo) {
        tintCaches.put(
            NukedFoliageTint.FALLOUT_WEIGHT_RESOLVER,
            new BlockTintCache(pos -> calculateBlockTint(pos, NukedFoliageTint.FALLOUT_WEIGHT_RESOLVER))
        );
    }
}
