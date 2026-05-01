package dev.pieter.simulated_pistons.content;

import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Predicate;

public class SPPlacementHelpers {
    private static int smallCogHelper = -1;
    private static int largeCogHelper = -1;

    public static void init() {
        if (smallCogHelper != -1) {
            return;
        }

        smallCogHelper = PlacementHelpers.register(new PistonCogPlacementHelper(false));
        largeCogHelper = PlacementHelpers.register(new PistonCogPlacementHelper(true));
    }

    public static ItemInteractionResult tryPlaceCog(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
        if (!player.mayBuild() || player.isShiftKeyDown() || !(stack.getItem() instanceof final BlockItem blockItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        final IPlacementHelper helper = PlacementHelpers.get(ICogWheel.isLargeCogItem(stack) ? largeCogHelper : smallCogHelper);
        if (!helper.matchesItem(stack) || !helper.matchesState(state)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return helper.getOffset(player, level, state, pos, hitResult, stack)
                .placeInWorld(level, blockItem, player, hand, hitResult);
    }

    private static class PistonCogPlacementHelper implements IPlacementHelper {
        private final boolean large;

        PistonCogPlacementHelper(final boolean large) {
            this.large = large;
        }

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            final Predicate<ItemStack> cogType = this.large ? ICogWheel::isLargeCogItem : ICogWheel::isSmallCogItem;
            return cogType.and(ICogWheel::isDedicatedCogItem);
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return state -> state.getBlock() instanceof SimulatedPistonBlock
                    && state.getValue(SimulatedPistonBlock.SEGMENT).hasController();
        }

        @Override
        public PlacementOffset getOffset(final Player player, final Level world, final BlockState state, final BlockPos pos, final BlockHitResult ray) {
            final Direction.Axis axis = state.getValue(SimulatedPistonBlock.FACING).getAxis();
            final Direction face = ray.getDirection();
            if (face.getAxis() == axis) {
                return PlacementOffset.fail();
            }

            if (this.large) {
                for (final Direction direction : IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(), face.getAxis(), axis)) {
                    final BlockPos newPos = pos.relative(face).relative(direction);
                    if (world.getBlockState(newPos).canBeReplaced() && CogWheelBlock.isValidCogwheelPosition(false, world, newPos, axis)) {
                        return PlacementOffset.success(newPos, placed -> placed.setValue(CogWheelBlock.AXIS, axis));
                    }
                }
                return PlacementOffset.fail();
            }

            for (final Direction direction : IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(), axis)) {
                final BlockPos newPos = pos.relative(direction);
                if (world.getBlockState(newPos).canBeReplaced() && CogWheelBlock.isValidCogwheelPosition(false, world, newPos, axis)) {
                    return PlacementOffset.success(newPos, placed -> placed.setValue(CogWheelBlock.AXIS, axis));
                }
            }
            return PlacementOffset.fail();
        }
    }
}
