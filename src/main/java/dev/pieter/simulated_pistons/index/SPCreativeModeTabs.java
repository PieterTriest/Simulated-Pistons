package dev.pieter.simulated_pistons.index;

import dev.pieter.simulated_pistons.SimulatedPistons;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SPCreativeModeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SimulatedPistons.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + SimulatedPistons.MOD_ID + ".main"))
                    .icon(() -> new ItemStack(SPBlocks.SIMULATED_PISTON_ITEM.get()))
                    .displayItems((parameters, output) -> output.accept(SPBlocks.SIMULATED_PISTON_ITEM.get()))
                    .build()
    );

    public static void register(final IEventBus modBus) {
        CREATIVE_MODE_TABS.register(modBus);
    }
}
