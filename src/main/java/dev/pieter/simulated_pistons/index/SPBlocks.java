package dev.pieter.simulated_pistons.index;

import dev.pieter.simulated_pistons.SimulatedPistons;
import dev.pieter.simulated_pistons.content.SimulatedPistonBlock;
import dev.pieter.simulated_pistons.content.SimulatedPistonLinkBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SPBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SimulatedPistons.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, SimulatedPistons.MOD_ID);

    public static final DeferredBlock<SimulatedPistonBlock> SIMULATED_PISTON = BLOCKS.registerBlock(
            "simulated_piston",
            SimulatedPistonBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5f, 6f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
    );

    public static final DeferredBlock<SimulatedPistonLinkBlock> SIMULATED_PISTON_LINK = BLOCKS.registerBlock(
            "simulated_piston_link",
            SimulatedPistonLinkBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5f, 6f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
    );

    public static final DeferredHolder<Item, BlockItem> SIMULATED_PISTON_ITEM = ITEMS.register(
            "simulated_piston",
            () -> new BlockItem(SIMULATED_PISTON.get(), new Item.Properties())
    );

    public static void register(final IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
