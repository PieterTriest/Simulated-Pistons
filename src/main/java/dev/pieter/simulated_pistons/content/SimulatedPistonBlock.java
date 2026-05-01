package dev.pieter.simulated_pistons.content;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import dev.pieter.simulated_pistons.index.SPBlockEntityTypes;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SimulatedPistonBlock extends DirectionalKineticBlock implements IBE<SimulatedPistonBlockEntity>, IRotate, ExtraKinetics.ExtraKineticsBlock {
    public static final EnumProperty<Segment> SEGMENT = EnumProperty.create("segment", Segment.class);

    public SimulatedPistonBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(SEGMENT, Segment.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(SEGMENT));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
        if (!player.mayBuild() || player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            this.withBlockEntityDo(level, pos, SimulatedPistonBlockEntity::resetExtension);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            updateChain(level, pos, state.getValue(FACING));
        }
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        final Direction facing = state.getValue(FACING);
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            updateChain(level, pos.relative(facing), facing);
            updateChain(level, pos.relative(facing.getOpposite()), facing);
        }
    }

    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final InteractionResult result = super.onWrenched(state, context);
        if (result.consumesAction() && !level.isClientSide) {
            final BlockState updated = level.getBlockState(pos);
            if (updated.getBlock() instanceof SimulatedPistonBlock) {
                updateChain(level, pos, updated.getValue(FACING));
            }
            updateChain(level, pos.relative(state.getValue(FACING)), state.getValue(FACING));
            updateChain(level, pos.relative(state.getValue(FACING).getOpposite()), state.getValue(FACING));
            IWrenchable.playRotateSound(level, pos);
        }
        return result;
    }

    @Override
    public Direction.Axis getRotationAxis(final BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(final LevelReader level, final BlockPos pos, final BlockState state, final Direction face) {
        return face.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public Class<SimulatedPistonBlockEntity> getBlockEntityClass() {
        return SimulatedPistonBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SimulatedPistonBlockEntity> getBlockEntityType() {
        return SPBlockEntityTypes.SIMULATED_PISTON.get();
    }

    @Override
    public IRotate getExtraKineticsRotationConfiguration() {
        return SimulatedPistonBlockEntity.PistonCogBlockEntity.EXTRA_COGWHEEL_CONFIG;
    }

    public static void updateChain(final Level level, final BlockPos origin, final Direction facing) {
        if (!(level.getBlockState(origin).getBlock() instanceof SimulatedPistonBlock)) {
            return;
        }

        BlockPos start = origin;
        while (isAlignedPiston(level.getBlockState(start.relative(facing.getOpposite())), facing)) {
            start = start.relative(facing.getOpposite());
        }

        int length = 0;
        BlockPos cursor = start;
        while (isAlignedPiston(level.getBlockState(cursor), facing)) {
            length++;
            cursor = cursor.relative(facing);
        }

        cursor = start;
        for (int index = 0; index < length; index++) {
            final Segment segment = Segment.forIndex(index, length);
            final BlockState state = level.getBlockState(cursor);
            if (state.getValue(SEGMENT) != segment) {
                level.setBlock(cursor, state.setValue(SEGMENT, segment), Block.UPDATE_ALL);
            }
            if (level.getBlockEntity(cursor) instanceof SimulatedPistonBlockEntity be) {
                be.setChainLength(length);
            }
            cursor = cursor.relative(facing);
        }
    }

    private static boolean isAlignedPiston(final BlockState state, final Direction facing) {
        return state.getBlock() instanceof SimulatedPistonBlock && state.getValue(FACING) == facing;
    }

    public enum Segment implements StringRepresentable {
        SINGLE("single"),
        CONTROLLER("controller"),
        MIDDLE("middle"),
        HEAD("head");

        private final String name;

        Segment(final String name) {
            this.name = name;
        }

        static Segment forIndex(final int index, final int length) {
            if (length <= 1) {
                return SINGLE;
            }
            if (index == 0) {
                return CONTROLLER;
            }
            if (index == length - 1) {
                return HEAD;
            }
            return MIDDLE;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
