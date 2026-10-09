package com.human.fabric.data.tag;

import com.human.common.registry.init.HumanFluids;
import com.human.compatibility.HumanCommonFluidTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

/**
 * Our fluids in the shared {@code c:} tags. ⚠ Before this provider existed, {@code data/c/tags/fluid/latex.json} sat in
 * the generated folder with nothing generating it - left over from an earlier provider. This owns it again, with the
 * same contents, and adds honey.
 */
public class HumanFluidTagProvider extends FabricTagProvider.FluidTagProvider {

    public HumanFluidTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        getOrCreateTagBuilder(HumanCommonFluidTags.LATEX).add(HumanFluids.LATEX.get(), HumanFluids.FLOWING_LATEX.get());
        getOrCreateTagBuilder(HumanCommonFluidTags.HONEY).add(HumanFluids.HONEY.get(), HumanFluids.FLOWING_HONEY.get());
    }
}
