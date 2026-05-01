package dev.pieter.simulated_pistons.client;

import dev.pieter.simulated_pistons.SimulatedPistons;
import dev.pieter.simulated_pistons.index.SPBlockEntityTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = SimulatedPistons.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class SPClientEvents {
    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SPBlockEntityTypes.SIMULATED_PISTON.get(), SimulatedPistonRenderer::new);
        event.registerBlockEntityRenderer(SPBlockEntityTypes.SIMULATED_PISTON_LINK.get(), SimulatedPistonLinkRenderer::new);
    }
}
