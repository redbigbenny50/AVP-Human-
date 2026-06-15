package com.human.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StructureTemplatePool.class)
public interface MixinStructureTemplatePool_Accessor {

    @Accessor("templates")
    ObjectArrayList<StructurePoolElement> avp_human$getTemplates();

    @Accessor("rawTemplates")
    List<Pair<StructurePoolElement, Integer>> avp_human$getRawTemplates();

    @Mutable
    @Accessor("rawTemplates")
    void avp_human$setRawTemplates(List<Pair<StructurePoolElement, Integer>> rawTemplates);
}
