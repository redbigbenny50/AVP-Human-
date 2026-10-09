package com.human.common.gameplay.block.cryo;

import com.human.common.gameplay.gene.GeneBonusDataEntry;
import com.human.common.model.GeneCarrier;
import com.human.common.registry.GeneBonusDataRegistry;
import com.human.common.util.GeneDataUtil;
import com.human.compatibility.avp_alien.AlienSpecimenInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The specimen's data sheet for the controller's SPECIMENS tab, written once when it is frozen - so the screen never
 * has to rebuild a mob to read it.
 * <p>
 * [stated] Oct 2: every stored specimen shows its genes - its type's DEFAULT genes (avp_human's data-driven gene table
 * for that mob) and any BONUS genes it carries itself - plus health, armour, attack and speed, and for xenomorphs its
 * strain, stage and hive.
 * </p>
 */
public final class SpecimenCard {

    private SpecimenCard() {}

    public static CompoundTag of(LivingEntity mob) {
        var card = new CompoundTag();
        card.putString("type", BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
        card.putFloat("health", mob.getHealth());
        card.putFloat("maxHealth", mob.getMaxHealth());
        card.putFloat("armor", mob.getArmorValue());
        card.putDouble(
            "attack",
            mob.getAttribute(Attributes.ATTACK_DAMAGE) != null ? mob.getAttributeValue(Attributes.ATTACK_DAMAGE) : -1.0
        );
        card.putDouble(
            "speed",
            mob.getAttribute(Attributes.MOVEMENT_SPEED) != null ? mob.getAttributeValue(Attributes.MOVEMENT_SPEED) : -1.0
        );

        card.put("defaultGenes", genes(GeneDataUtil.toList(GeneBonusDataRegistry.getOrDefault(mob.getType()))));

        if (mob instanceof GeneCarrier carrier) {
            var active = carrier.getOrCreateGeneManager().getGeneContainer().getActiveGeneMap().getBackingMap();
            card.put("bonusGenes", genes(GeneDataUtil.toList(active)));
        }

        var strain = AlienSpecimenInfo.strain(mob);

        if (strain != null) {
            card.putString("strain", strain);
            card.putString("stage", stage(mob.getType()));
            var hive = AlienSpecimenInfo.hive(mob);
            card.putString("hive", hive == null ? "NONE" : hive);

            // [stated] Oct 3: the short name shown on the card (NO_1_2); "hive" keeps the full id for copying.
            var hiveName = AlienSpecimenInfo.hiveName(mob);

            if (hiveName != null) {
                card.putString("hiveName", hiveName);
            }
        }

        return card;
    }

    /**
     * For a specimen frozen before cards existed: rebuilds the mob from its save data once (never added to the world)
     * to read it. Returns null if the mob's type no longer exists.
     */
    public static @Nullable CompoundTag rebuild(Level level, CompoundTag specimen) {
        var entity = EntityType.create(specimen.copy(), level).orElse(null);
        return entity instanceof LivingEntity living ? of(living) : null;
    }

    /** Life stage from the type's own name - xenomorph type ids say what they are. */
    private static String stage(EntityType<?> type) {
        var path = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();

        if (path.contains("ovomorph") || path.contains("egg")) {
            return "EGG";
        }

        if (path.contains("facehugger")) {
            return "PARASITE";
        }

        if (path.contains("chestburster")) {
            return "LARVA";
        }

        if (path.contains("adolescent")) {
            return "JUVENILE";
        }

        return "ADULT";
    }

    private static ListTag genes(List<GeneBonusDataEntry> entries) {
        var list = new ListTag();

        for (var entry : entries) {
            var gene = new CompoundTag();
            gene.putString("id", entry.id().toString());
            gene.putString("op", entry.operation().name());
            gene.putDouble("v", entry.value());
            list.add(gene);
        }

        return list;
    }

    public static ListTag geneList(CompoundTag card, String key) {
        return card.getList(key, Tag.TAG_COMPOUND);
    }
}
