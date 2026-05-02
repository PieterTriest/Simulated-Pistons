package dev.pieter.simulated_pistons.content;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.pieter.simulated_pistons.index.SPBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SimulatedPistonLinkBlock extends DirectionalKineticBlock implements IBE<SimulatedPistonLinkBlockEntity>, BlockSubLevelAssemblyListener {
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
    public void beforeMove(final ServerLevel originLevel, final ServerLevel resultingLevel, final BlockState newState, final BlockPos oldPos, final BlockPos newPos) {
        this.withBlockEntityDo(originLevel, oldPos, SimulatedPistonLinkBlockEntity::beforeAssembly);
    }

    @Override
    public void afterMove(final ServerLevel originLevel, final ServerLevel resultingLevel, final BlockState newState, final BlockPos oldPos, final BlockPos newPos) {
        this.withBlockEntityDo(resultingLevel, newPos, SimulatedPistonLinkBlockEntity::fixParentLinkingWhenMoved);
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
        if (!player.mayBuild() || player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof final SimulatedPistonLinkBlockEntity link) {
            link.toggleParentAssembly();
        }
        return ItemInteractionResult.SUCCESS;
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
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return plateShape(state.getValue(FACING), false);
    }

    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return plateShape(state.getValue(FACING), true);
    }

    @Override
    protected VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return plateShape(state.getValue(FACING), false);
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        return new ItemStack(Items.AIR);
    }

    private static VoxelShape plateShape(final Direction facing, final boolean collision) {
        final double min = collision ? 3 / 16.0 : 0;
        final double max = collision ? 13 / 16.0 : 1;
        final double low = collision ? 12 / 16.0 : 12.1 / 16.0;

        return switch (facing) {
            case UP -> Shapes.box(min, low, min, max, 1, max);
            case DOWN -> Shapes.box(min, 0, min, max, 1 - low, max);
            case NORTH -> Shapes.box(min, min, 0, max, max, 1 - low);
            case SOUTH -> Shapes.box(min, min, low, max, max, 1);
            case WEST -> Shapes.box(0, min, min, 1 - low, max, max);
            case EAST -> Shapes.box(low, min, min, 1, max, max);
        };
    }
}
