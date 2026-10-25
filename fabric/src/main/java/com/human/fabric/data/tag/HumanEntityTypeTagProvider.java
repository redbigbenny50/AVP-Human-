package com.human.fabric.data.tag;

import com.blib.api.common.tag.v1.BLibEntityTypeTags;
import com.human.common.registry.init.HumanEntityTypes;
import com.human.common.registry.tag.HumanEntityTypeTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.concurrent.CompletableFuture;

public class HumanEntityTypeTagProvider extends FabricTagProvider.EntityTypeTagProvider {

    public HumanEntityTypeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        addHatedByMarines();
        addIgnoredByMarines();
        addSmartTargetable();
        addHumanoids();
        addRadiationResistant();

        // Compatibility
        addHatedByXenomorphs();
        addHosts();
        addGigeresqueHosts();
        addPredatorHeatTiers();
        addPredatorNetEscapeTier();
        addSoldiers();
        addCaptureCordBlacklist();
    }

    /**
     * Where this mod's mobs sit in AVP: Predator's thermal vision.
     * <p>
     * Declared from this side rather than predator's: the tags are keyed by ResourceLocation, so no dependency in
     * either direction is needed, and marines stay correct here as the roster changes instead of drifting out of a
     * hardcoded list in another mod.
     * <p>
     * ⚠ Anything absent reads at ambient. The sentry turret and thrown grenade are deliberately absent — a machine and
     * an unlit charge have no heat to give away.
     */
    private void addPredatorHeatTiers() {
        // Marines are people: they read exactly as warm as a player does, which is the point of hunting them.
        getOrCreateTagBuilder(predatorHeatTag("warm_blooded_large"))
            .add(HumanEntityTypes.MARINE.get())
            .add(splitSoldiers());

        getOrCreateTagBuilder(predatorHeatTag("warm_blooded"))
            .add(HumanEntityTypes.MARINE_DOG.get());

        // Ordnance and open flame. A rocket in flight and a flamethrower's jet are both fire by any reading, and a
        // detonation is the hottest thing on the field.
        getOrCreateTagBuilder(predatorHeatTag("burning"))
            .add(
                HumanEntityTypes.FLAMETHROW.get(),
                HumanEntityTypes.ROCKET.get(),
                HumanEntityTypes.NUKE.get(),
                HumanEntityTypes.MUSHROOM_CLOUD.get()
            );
    }

    /**
     * Where a marine sits in avp_predator's net escape ladder. [stated] "the net rules we made have to apply to the
     * marines as well." The ladder's own fallback already lands a 0.6-wide armed mob on the medium tier (a zombie's 5%
     * roll), so this pins that outcome against a future hitbox change rather than changing it. Raw ResourceLocation
     * because avp_predator is compile-only here and absent at datagen; the tag simply does nothing without it.
     */
    private void addPredatorNetEscapeTier() {
        getOrCreateTagBuilder(
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("avp_predator", "net_escape_medium"))
        ).add(HumanEntityTypes.MARINE.get()).add(splitSoldiers());
    }

    private static TagKey<EntityType<?>> predatorHeatTag(String tier) {
        return TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("avp_predator", "heat/" + tier)
        );
    }

    /**
     * Oct 5 - monsters marines leave alone on sight (MarineHuntRules). The yautja keep [stated] their ruling: fought
     * only when one targets a marine uncloaked or strikes first. ⚠ Named by string - avp_predator is compile-only here,
     * and referencing its tag class would kill this whole provider at datagen (the cross-mod-datagen lesson).
     */
    private void addIgnoredByMarines() {
        getOrCreateTagBuilder(HumanEntityTypeTags.IGNORED_BY_MARINES)
            .addOptionalTag(
                TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("avp_predator", "predators"))
            );
    }

    private void addHatedByMarines() {
        getOrCreateTagBuilder(HumanEntityTypeTags.HATED_BY_MARINES)
            .addOptionalTag(EntityTypeTags.ILLAGER)
            .addOptionalTag(EntityTypeTags.ILLAGER_FRIENDS)
            .addOptionalTag(EntityTypeTags.UNDEAD)
            .addOptionalTag(EntityTypeTags.WITHER_FRIENDS)
            .add(EntityType.CAVE_SPIDER)
            .add(EntityType.CREEPER)
            .add(EntityType.BLAZE)
            .add(EntityType.BREEZE)
            .add(EntityType.ELDER_GUARDIAN)
            .add(EntityType.GHAST)
            .add(EntityType.GUARDIAN)
            .add(EntityType.HOGLIN)
            .add(EntityType.MAGMA_CUBE)
            .add(EntityType.PIGLIN)
            .add(EntityType.PIGLIN_BRUTE)
            .add(EntityType.SHULKER)
            .add(EntityType.SILVERFISH)
            .add(EntityType.SLIME)
            .add(EntityType.SPIDER)
            .add(EntityType.VEX)
            .add(EntityType.WARDEN)
            // Ad Astra. Folded in from the community avp_ad_astra datapack, Sep 22.
            .addOptional(ResourceLocation.fromNamespaceAndPath("ad_astra", "star_crawler"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("ad_astra", "zombified_mogler"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("ad_astra", "zombified_pygro"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("ad_astra", "sulfur_creeper"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("ad_astra", "corrupted_lunarian"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("ad_astra", "pygro"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("ad_astra", "pygro_brute"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("ad_astra", "martian_raptor"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("ad_astra", "mogler"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("ad_astra", "glacian_ram"))
            // Villagers Reborn (slimpatch) illagers: vanilla ones arrive via #minecraft:illager above, these are not in
            // it.
            .addOptional(ResourceLocation.fromNamespaceAndPath("slimpatch", "pillager"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("slimpatch", "vindicator"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("slimpatch", "evoker"))
            // ⚠ Oct 5 - [stated] "yes have the marines hunt xenos on site". Every xenomorph form, eggs and huggers
            // included, through avp_alien's own umbrella tag. Until now no avp_alien entry was here at all, so a
            // marine only fought a xeno that had already targeted it, a squadmate or its leader.
            // ⚠⚠ BUILT FROM A STRING, NEVER FROM avp_alien's TAG CLASS: avp_alien is compile-only for this module and
            // absent from the datagen runtime, so referencing its constant kills the whole provider (the
            // cross-mod-datagen lesson). An optional tag is simply empty when avp_alien is not installed.
            // ⚠ Yautja are deliberately NOT here - his ruling is that marines fight a yautja only when it targets them
            // uncloaked or strikes first, which the threat check already does without a hate entry.
            .addOptionalTag(
                TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("avp_alien", "aliens"))
            );
    }

    /**
     * ⚠ REUSES THE MARINE HATE LIST RATHER THAN RESTATING IT. Everything a marine shoots at is something a smart weapon
     * should box, so this is one optional tag reference instead of a second list that would quietly drift out of step
     * with the first the moment either is edited.
     */
    private void addSmartTargetable() {
        getOrCreateTagBuilder(HumanEntityTypeTags.SMART_TARGETABLE)
            .addOptionalTag(HumanEntityTypeTags.HATED_BY_MARINES);

        // ⚠ A SEPARATE EXCLUSION LIST, BECAUSE TAGS CANNOT SUBTRACT. SMART_TARGETABLE is seeded wholesale from the
        // marine hate list and there is no way to write "all of that except one entry" in a tag file.
        // ⭐ Endermen teleport the moment they are looked at, so a tracker locks, loses them and re-acquires whatever
        // was behind them - and they are not what a marine is hunting.
        getOrCreateTagBuilder(HumanEntityTypeTags.SMART_TARGETING_IGNORED)
            .add(EntityType.ENDERMAN);
    }

    private void addHumanoids() {
        getOrCreateTagBuilder(BLibEntityTypeTags.HUMANOIDS)
            .add(
                HumanEntityTypes.MARINE.get()
            )
            .add(splitSoldiers());
    }

    private void addRadiationResistant() {
        getOrCreateTagBuilder(HumanEntityTypeTags.RADIATION_RESISTANT)
            .addOptionalTag(EntityTypeTags.UNDEAD)
            .add(EntityType.CREEPER);

        // Anything vanilla already treats as unfreezable - strays, polar bears, snow golems and the like.
        getOrCreateTagBuilder(HumanEntityTypeTags.CRYOFLUID_IMMUNE)
            .addOptionalTag(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES);

        // [stated] "dragon and wither no warden and elder guardian yes" — the two that cannot be frozen solid.
        getOrCreateTagBuilder(HumanEntityTypeTags.FREEZE_SOLID_IMMUNE)
            .add(EntityType.ENDER_DRAGON, EntityType.WITHER);

        // [stated] Oct 19 - avp_alien's eggs and nether strain. Optional ids, so the tags load without avp_alien.
        // Irradiated eggs and xenos are not here: they are immune to cold (avp_alien's own cryo-fuel rule).
        getOrCreateTagBuilder(HumanEntityTypeTags.FREEZES_INSTANTLY)
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "ovomorph"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "aberrant_ovomorph"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "royal_ovomorph"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "royal_aberrant_ovomorph"));

        getOrCreateTagBuilder(HumanEntityTypeTags.SHATTERS_FROM_COLD)
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_ovomorph"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "royal_nether_ovomorph"));

        getOrCreateTagBuilder(HumanEntityTypeTags.FREEZE_OR_EXPLODE)
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_drone"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_warrior"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_runner"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_prowler"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_spitter"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_boiler"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_burster"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_carrier"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_chrysalis"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_crusher"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_praetorian"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_ravager"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_razor_claw"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_harbinger"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_predalien"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_queen"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_empress"));

        // Bosses do not go in a jar. Packs extend this.
        getOrCreateTagBuilder(HumanEntityTypeTags.CRYOTANK_BLACKLIST)
            .add(EntityType.WITHER, EntityType.ENDER_DRAGON, EntityType.ELDER_GUARDIAN, EntityType.WARDEN);
    }

    private void addHatedByXenomorphs() {
        getOrCreateTagBuilder(foreignEntityTypeTag("avp_alien", "hated_by_xenomorphs"))
            .add(HumanEntityTypes.MARINE.get())
            .add(splitSoldiers());
    }

    private void addHosts() {
        // Oct 24 - [stated, relayed] "Facehuggers are not attacking colonists at all." The colonist was never in this
        // tag, so to every facehugger, egg and parasite rule it simply was not a host.
        getOrCreateTagBuilder(foreignEntityTypeTag("avp_alien", "hosts"))
            .add(HumanEntityTypes.MARINE.get())
            .add(HumanEntityTypes.COLONIST.get())
            .add(splitSoldiers());
    }

    /**
     * Oct 7 - Gigeresque compatibility (tag names read from Gigeresque 0.8.16): marines and colonists are neomorph
     * hosts ({@code gigeresque:neohost}) and classic facehugger hosts ({@code gigeresque:classicalienhost}); the marine
     * dog is a Stalker host ({@code gigeresque:largemutanthost}) and a runner host ({@code gigeresque:runnerhost}). All
     * of these are part of {@code gigeresque:allhosts}. They merge with Gigeresque's own lists ("replace": false) and
     * are inert data when Gigeresque is not installed.
     */
    private void addGigeresqueHosts() {
        getOrCreateTagBuilder(foreignEntityTypeTag("gigeresque", "neohost"))
            .add(HumanEntityTypes.MARINE.get(), HumanEntityTypes.COLONIST.get())
            .add(splitSoldiers());

        // Classic facehugger hosts: a hugged marine or colonist bursts a classic chestburster.
        getOrCreateTagBuilder(foreignEntityTypeTag("gigeresque", "classicalienhost"))
            .add(HumanEntityTypes.MARINE.get(), HumanEntityTypes.COLONIST.get())
            .add(splitSoldiers());

        // Runner hosts (Gigeresque's dog-born runner line; wolves are already in it).
        getOrCreateTagBuilder(foreignEntityTypeTag("gigeresque", "runnerhost"))
            .add(HumanEntityTypes.MARINE_DOG.get());

        // The marine dog bursts a Stalker. In Gigeresque 0.8.16 the Stalker is what a LARGE mutant host produces
        // (GigEntityUtils: isTargetLargeMutantHost -> MUTANT_STALKER); wolves are already in that tag.
        getOrCreateTagBuilder(foreignEntityTypeTag("gigeresque", "largemutanthost"))
            .add(HumanEntityTypes.MARINE_DOG.get());
    }

    /**
     * Builds a TagKey owned by ANOTHER mod from its raw id, instead of importing that mod's tag class.
     * <p>
     * Datagen only ever needs the tag's IDENTITY to write a JSON file, never the foreign class - and importing it made
     * this provider fail with NoClassDefFoundError whenever the sibling mod was absent from the DATAGEN runtime
     * classpath (it is compile-only here). Raw ids keep these compatibility tags working no matter which siblings are
     * present, and an unused tag file for an absent mod is simply inert data.
     * </p>
     */
    /**
     * [stated] Oct 13 - the soldier types split out of the marine. They go everywhere the marine goes above, and:
     * <ul>
     * <li>{@code avp_human:soldiers} - every soldier type, the marine included, for faction work and datapacks.</li>
     * <li>avp_alien's {@code xenomorph_threat_3_high_danger} and avp_predator's {@code skinnable}, which list
     * {@code avp_human:marine} by name in their own files (read from avp_alien 0.2.7s-fork and avp_predator
     * 0.1.6ab-fork). Tag files merge across mods, so the new types are added from this side and neither sibling needs a
     * change; without them installed the files are inert.</li>
     * </ul>
     */
    private void addSoldiers() {
        getOrCreateTagBuilder(HumanEntityTypeTags.SOLDIERS)
            .add(HumanEntityTypes.MARINE.get())
            .add(splitSoldiers());

        getOrCreateTagBuilder(foreignEntityTypeTag("avp_alien", "xenomorph_threat_3_high_danger"))
            .add(splitSoldiers());

        getOrCreateTagBuilder(foreignEntityTypeTag("avp_predator", "skinnable"))
            .add(splitSoldiers());
    }

    /**
     * [stated] Oct 14 - what the capture cord refuses: avp_alien's capture-chain blacklist (the boss mobs) plus the
     * sentry turret, which is a machine rather than a specimen. Datapackable.
     */
    private void addCaptureCordBlacklist() {
        getOrCreateTagBuilder(HumanEntityTypeTags.CAPTURE_CORD_BLACKLIST)
            .add(EntityType.ENDER_DRAGON)
            .add(EntityType.WARDEN)
            .add(EntityType.WITHER)
            .add(HumanEntityTypes.SENTRY_TURRET.get());
    }

    /** The soldier types split out of the marine (Oct 13), for every tag the marine is in. */
    private static EntityType<?>[] splitSoldiers() {
        return new EntityType<?>[] {
            HumanEntityTypes.TACTICAL_MARINE.get(),
            HumanEntityTypes.WY_COMMANDO.get(),
            HumanEntityTypes.WY_ELITE.get(),
            HumanEntityTypes.WY_SPEC_OPS_COMMANDO.get(),
            HumanEntityTypes.WY_SPEC_OPS_ELITE.get(),
            HumanEntityTypes.WY_APE_TROOPER.get()
        };
    }

    private static TagKey<EntityType<?>> foreignEntityTypeTag(String namespace, String path) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
