package com.human.neoforge.compat.create;

import com.human.Human;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * Oct 24 - the Create converters: the ALTERNATOR (Create rotation to avp_human power) and the ELECTRIC GEARBOX
 * (avp_human power to Create rotation), each in steel, W-Y and USCM.
 * <p>
 * ⚠ NeoForge only, and only with Create installed: HumanNeoForge calls {@link #register} after checking for Create, and
 * nothing else touches this package, so none of Create's classes load without it. [stated] Oct 24 - "neo only makes
 * sense since theres no fabric version yet".
 * </p>
 */
public final class HumanCreate {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Human.MOD_ID);

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Human.MOD_ID);

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
        Registries.BLOCK_ENTITY_TYPE,
        Human.MOD_ID
    );

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Human.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AlternatorMenu>> ALTERNATOR_MENU = MENUS.register(
        "alternator",
        () -> IMenuTypeExtension.create(AlternatorMenu::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ElectricGearboxMenu>> GEARBOX_MENU = MENUS.register(
        "electric_gearbox",
        () -> IMenuTypeExtension.create(ElectricGearboxMenu::new)
    );

    public static final Map<ConverterEdition, DeferredBlock<AlternatorBlock>> ALTERNATORS = new EnumMap<>(ConverterEdition.class);

    public static final Map<ConverterEdition, DeferredBlock<ElectricGearboxBlock>> GEARBOXES = new EnumMap<>(ConverterEdition.class);

    static {
        for (var edition : ConverterEdition.values()) {
            var alternator = BLOCKS.register(edition.id("alternator"), () -> new AlternatorBlock(properties(edition), edition));
            ALTERNATORS.put(edition, alternator);
            ITEMS.register(edition.id("alternator"), () -> new BlockItem(alternator.get(), new Item.Properties()));

            var gearbox = BLOCKS.register(edition.id("electric_gearbox"), () -> new ElectricGearboxBlock(properties(edition), edition));
            GEARBOXES.put(edition, gearbox);
            ITEMS.register(edition.id("electric_gearbox"), () -> new BlockItem(gearbox.get(), new Item.Properties()));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlternatorBlockEntity>> ALTERNATOR_ENTITY = BLOCK_ENTITIES
        .register(
            "alternator",
            () -> BlockEntityType.Builder.of(
                (pos, state) -> new AlternatorBlockEntity(HumanCreate.ALTERNATOR_ENTITY.get(), pos, state),
                ALTERNATORS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
            ).build(null)
        );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricGearboxBlockEntity>> GEARBOX_ENTITY = BLOCK_ENTITIES
        .register(
            "electric_gearbox",
            () -> BlockEntityType.Builder.of(
                (pos, state) -> new ElectricGearboxBlockEntity(HumanCreate.GEARBOX_ENTITY.get(), pos, state),
                GEARBOXES.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
            ).build(null)
        );

    private HumanCreate() {}

    /** Machine-grade, like the other machines: a pickaxe to break it and keep it. */
    private static BlockBehaviour.Properties properties(ConverterEdition edition) {
        return BlockBehaviour.Properties.of()
            .mapColor(
                switch (edition) {
                    case STEEL -> MapColor.METAL;
                    case WY -> MapColor.SNOW;
                    case USCM -> MapColor.COLOR_GREEN;
                }
            )
            .strength(3.5F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(HumanCreate::addToCreativeTabs);
    }

    /** Steel in the power tab, W-Y and USCM in their own tabs - where the other edition machines are. */
    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        for (var edition : ConverterEdition.values()) {
            if (event.getTabKey() == edition.tab()) {
                event.accept(ALTERNATORS.get(edition).get());
                event.accept(GEARBOXES.get(edition).get());
            }
        }
    }
}
