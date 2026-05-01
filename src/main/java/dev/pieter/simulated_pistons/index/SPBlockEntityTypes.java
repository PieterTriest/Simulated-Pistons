package dev.pieter.simulated_pistons.index;

import dev.pieter.simulated_pistons.SimulatedPistons;
import dev.pieter.simulated_pistons.content.SimulatedPistonBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SPBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SimulatedPistons.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimulatedPistonBlockEntity>> SIMULATED_PISTON =
            BLOCK_ENTITY_TYPES.register("simulated_piston", () -> BlockEntityType.Builder
                    .of(SimulatedPistonBlockEntity::new, SPBlocks.SIMULATED_PISTON.get())
                    .build(null));

    public static void register(final IEventBus modBus) {
        BLOCK_ENTITY_TYPES.register(modBus);
    }
}
