package dev.pieter.simulated_pistons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.pieter.simulated_pistons.content.SimulatedPistonLinkBlock;
import dev.pieter.simulated_pistons.content.SimulatedPistonLinkBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class SimulatedPistonLinkRenderer extends KineticBlockEntityRenderer<SimulatedPistonLinkBlockEntity> {
    private static final float TAIL_EXTENSION_THRESHOLD = 8 / 16.0f;

    public SimulatedPistonLinkRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(final SimulatedPistonLinkBlockEntity be, final float partialTick, final PoseStack ms, final MultiBufferSource buffer, final int packedLight, final int packedOverlay) {
        final BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof SimulatedPistonLinkBlock)) {
            return;
        }

        final Direction facing = state.getValue(SimulatedPistonLinkBlock.FACING);
        final Direction shaftDirection = facing.getOpposite();
        final VertexConsumer vb = buffer.getBuffer(RenderType.solid());

        renderRotatingBuffer(be, CachedBuffers.partialFacing(SPPartialModels.SHAFT_SIXTEENTH, state, facing), ms, vb, packedLight);

        final int chainLength = be.getChainLength();
        for (int segmentIndex = 0; segmentIndex < chainLength; segmentIndex++) {
            final int offset = segmentIndex;
            ms.pushPose();
            ms.translate(shaftDirection.getStepX() * offset, shaftDirection.getStepY() * offset, shaftDirection.getStepZ() * offset);
            CachedBuffers.partialFacing(this.getShaftSegment(chainLength, segmentIndex), state, shaftDirection)
                    .light(packedLight)
                    .renderInto(ms, vb);
            ms.popPose();
        }

        if (be.getParentExtension() > TAIL_EXTENSION_THRESHOLD) {
            final int offset = chainLength - 1;
            ms.pushPose();
            ms.translate(shaftDirection.getStepX() * offset, shaftDirection.getStepY() * offset, shaftDirection.getStepZ() * offset);
            CachedBuffers.partialFacing(SPPartialModels.PISTON_SHAFT_SEGMENT_TAIL_EXTENSION, state, shaftDirection)
                    .light(packedLight)
                    .renderInto(ms, vb);
            ms.popPose();
        }
    }

    private PartialModel getShaftSegment(final int chainLength, final int segmentIndex) {
        if (chainLength == 1) {
            return SPPartialModels.PISTON_SHAFT_SEGMENT_SINGLE;
        }
        if (segmentIndex == 0) {
            return SPPartialModels.PISTON_SHAFT_SEGMENT_HEAD;
        }
        if (segmentIndex == chainLength - 1) {
            return SPPartialModels.PISTON_SHAFT_SEGMENT_TAIL;
        }
        return SPPartialModels.PISTON_SHAFT_SEGMENT;
    }

    @Override
    public boolean shouldRenderOffScreen(final SimulatedPistonLinkBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
