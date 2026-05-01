package dev.pieter.simulated_pistons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.pieter.simulated_pistons.content.SimulatedPistonLinkBlock;
import dev.pieter.simulated_pistons.content.SimulatedPistonLinkBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class SimulatedPistonLinkRenderer implements BlockEntityRenderer<SimulatedPistonLinkBlockEntity> {
    public SimulatedPistonLinkRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(final SimulatedPistonLinkBlockEntity be, final float partialTick, final PoseStack ms, final MultiBufferSource buffer, final int packedLight, final int packedOverlay) {
        final BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof SimulatedPistonLinkBlock)) {
            return;
        }

        final Direction facing = state.getValue(SimulatedPistonLinkBlock.FACING);
        final Direction shaftDirection = facing.getOpposite();
        final VertexConsumer vb = buffer.getBuffer(RenderType.solid());

        for (int segmentIndex = 0; segmentIndex < be.getChainLength(); segmentIndex++) {
            final int offset = segmentIndex;
            ms.pushPose();
            ms.translate(shaftDirection.getStepX() * offset, shaftDirection.getStepY() * offset, shaftDirection.getStepZ() * offset);
            CachedBuffers.partialFacing(SPPartialModels.PISTON_SHAFT_SEGMENT, state, shaftDirection)
                    .light(packedLight)
                    .renderInto(ms, vb);
            ms.popPose();
        }
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
