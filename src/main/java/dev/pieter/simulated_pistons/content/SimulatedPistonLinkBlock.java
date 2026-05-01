package dev.pieter.simulated_pistons.content;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.pieter.simulated_pistons.index.SPBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SimulatedPistonLinkBlock extends DirectionalKineticBlock implements IBE<SimulatedPistonLinkBlockEntity> {
    public SimulatedPistonLinkBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(final BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public Class<SimulatedPistonLinkBlockEntity> getBlockEntityClass() {
        return SimulatedPistonLinkBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SimulatedPistonLinkBlockEntity> getBlockEntityType() {
        return SPBlockEntityTypes.SIMULATED_PISTON_LINK.get();
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        return new ItemStack(Items.AIR);
    }
}
