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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SimulatedPistonBlock extends DirectionalKineticBlock implements IBE<SimulatedPistonBlockEntity>, IRotate, ExtraKinetics.ExtraKineticsBlock {
    public static final EnumProperty<Segment> SEGMENT = EnumProperty.create("segment", Segment.class);
    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");

    public SimulatedPistonBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(SEGMENT, Segment.SINGLE).setValue(ASSEMBLED, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(SEGMENT, ASSEMBLED));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
        final ItemInteractionResult cogPlacement = SPPlacementHelpers.tryPlaceCog(stack, state, level, pos, player, hand, hitResult);
        if (cogPlacement.consumesAction()) {
            return cogPlacement;
        }

        if (!player.mayBuild() || player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!state.getValue(SEGMENT).hasController()) {
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
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            cleanupChainAssembly(level, pos, facing);
        }

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
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return assembledHeadNeedsRecess(state) ? recessedShape(state.getValue(FACING)) : super.getShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return assembledHeadNeedsRecess(state) ? recessedShape(state.getValue(FACING)) : super.getCollisionShape(state, level, pos, context);
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
            final boolean keepAssembled = state.getValue(ASSEMBLED) && segment.hasAttachmentFace();
            final BlockState updated = state.setValue(SEGMENT, segment).setValue(ASSEMBLED, keepAssembled);
            if (state != updated) {
                level.setBlock(cursor, updated, Block.UPDATE_ALL);
            }
            if (level.getBlockEntity(cursor) instanceof SimulatedPistonBlockEntity be) {
                be.setChainLength(length);
            }
            cursor = cursor.relative(facing);
        }
    }

    private static void cleanupChainAssembly(final Level level, final BlockPos origin, final Direction facing) {
        BlockPos start = origin;
        while (isAlignedPiston(level.getBlockState(start.relative(facing.getOpposite())), facing)) {
            start = start.relative(facing.getOpposite());
        }

        BlockPos cursor = start;
        while (isAlignedPiston(level.getBlockState(cursor), facing)) {
            if (level.getBlockEntity(cursor) instanceof final SimulatedPistonBlockEntity be && be.isAttachmentAssembled()) {
                be.cleanupAfterPistonRemoved();
                return;
            }
            cursor = cursor.relative(facing);
        }
    }

    private static boolean isAlignedPiston(final BlockState state, final Direction facing) {
        return state.getBlock() instanceof SimulatedPistonBlock && state.getValue(FACING) == facing;
    }

    private static boolean assembledHeadNeedsRecess(final BlockState state) {
        return state.getValue(ASSEMBLED) && state.getValue(SEGMENT).hasAttachmentFace();
    }

    private static VoxelShape recessedShape(final Direction facing) {
        final double inset = 12 / 16.0;
        return switch (facing) {
            case UP -> Shapes.box(0, 0, 0, 1, inset, 1);
            case DOWN -> Shapes.box(0, 1 - inset, 0, 1, 1, 1);
            case NORTH -> Shapes.box(0, 0, 1 - inset, 1, 1, 1);
            case SOUTH -> Shapes.box(0, 0, 0, 1, 1, inset);
            case WEST -> Shapes.box(1 - inset, 0, 0, 1, 1, 1);
            case EAST -> Shapes.box(0, 0, 0, inset, 1, 1);
        };
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

        boolean hasAttachmentFace() {
            return this == SINGLE || this == HEAD;
        }

        boolean hasController() {
            return this == SINGLE || this == CONTROLLER;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
