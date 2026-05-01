package dev.pieter.simulated_pistons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.pieter.simulated_pistons.content.SimulatedPistonBlock;
import dev.pieter.simulated_pistons.content.SimulatedPistonBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class SimulatedPistonRenderer extends KineticBlockEntityRenderer<SimulatedPistonBlockEntity> {
    public SimulatedPistonRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(final SimulatedPistonBlockEntity be, final float partialTicks, final PoseStack ms, final MultiBufferSource buffer, final int light, final int overlay) {
        final BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof SimulatedPistonBlock)) {
            return;
        }

        final Direction facing = state.getValue(SimulatedPistonBlock.FACING);
        final SimulatedPistonBlock.Segment segment = state.getValue(SimulatedPistonBlock.SEGMENT);
        final VertexConsumer vb = buffer.getBuffer(RenderType.solid());

        if (segment == SimulatedPistonBlock.Segment.SINGLE || segment == SimulatedPistonBlock.Segment.CONTROLLER) {
            renderRotatingBuffer(be, CachedBuffers.partialFacing(SPPartialModels.SHAFT_SIXTEENTH, state, facing.getOpposite()), ms, vb, light);
            renderRotatingBuffer(be.getExtraKinetics(), CachedBuffers.partialFacingVertical(SPPartialModels.CONTROLLER_GEAR, state, facing), ms, vb, light);
        }

        if (!state.getValue(SimulatedPistonBlock.ASSEMBLED) && (segment == SimulatedPistonBlock.Segment.SINGLE || segment == SimulatedPistonBlock.Segment.HEAD)) {
            renderRotatingBuffer(be, CachedBuffers.partialFacing(SPPartialModels.SHAFT_SIXTEENTH, state, facing), ms, vb, light);
        }
    }
}
