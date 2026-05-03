package dev.pieter.simulated_pistons.content;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.pieter.simulated_pistons.index.SPBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SimulatedPistonLinkBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelActor {
    @Nullable
    private BlockPos parent;
    @Nullable
    private UUID parentSubLevelId;
    private int chainLength = 1;
    private float parentExtension;
    private boolean assembling;

    public SimulatedPistonLinkBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public static SimulatedPistonLinkBlockEntity create(final BlockPos pos, final BlockState state) {
        return new SimulatedPistonLinkBlockEntity(dev.pieter.simulated_pistons.index.SPBlockEntityTypes.SIMULATED_PISTON_LINK.get(), pos, state);
    }

    public void beforeAssembly() {
        this.assembling = true;
    }

    public void beforeCleanup() {
        this.assembling = true;
    }

    public void setParent(final SimulatedPistonBlockEntity parent) {
        final SubLevel parentSubLevel = Sable.HELPER.getContaining(parent);
        this.assembling = false;
        this.parent = parent.getBlockPos();
        this.parentSubLevelId = parentSubLevel != null ? parentSubLevel.getUniqueId() : null;
        this.chainLength = parent.getChainLength();
        this.parentExtension = parent.getExtension();
        this.setChanged();
        this.sendData();
    }

    public int getChainLength() {
        return this.chainLength;
    }

    public float getParentExtension() {
        return this.parentExtension;
    }

    public void setParentExtension(final float parentExtension) {
        final float clamped = Math.min(Math.max(0, parentExtension), this.chainLength);
        if (this.parentExtension == clamped) {
            return;
        }
        this.parentExtension = clamped;
        this.setChanged();
        this.sendData();
    }

    public void toggleParentAssembly() {
        if (this.level == null || this.level.isClientSide || this.parent == null) {
            return;
        }

        if (this.level.getBlockEntity(this.parent) instanceof final SimulatedPistonBlockEntity piston) {
            piston.resetExtension();
        }
    }

    public void fixParentLinkingWhenMoved() {
        if (this.level == null || this.level.isClientSide || this.parent == null) {
            return;
        }

        final BlockEntity be = this.level.getBlockEntity(this.parent);
        if (be instanceof final SimulatedPistonBlockEntity piston) {
            piston.setLinkPos(this.getBlockPos());

            final SubLevel newSubLevel = Sable.HELPER.getContaining(this);
            if (newSubLevel != null) {
                final UUID newSubLevelId = newSubLevel.getUniqueId();
                if (!newSubLevelId.equals(piston.getSubLevelId())) {
                    piston.setSubLevelId(newSubLevelId);
                    piston.reattachConstraint(newSubLevel);
                }
            }

            piston.associateLinkWithParent();
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        final BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof SimulatedPistonLinkBlock)) {
            return super.getRenderBoundingBox();
        }

        final net.minecraft.core.Direction shaftDirection = state.getValue(SimulatedPistonLinkBlock.FACING).getOpposite();
        final BlockPos end = this.getBlockPos().relative(shaftDirection, Math.max(1, this.chainLength));
        return AABB.encapsulatingFullBlocks(this.getBlockPos(), end).inflate(1.0);
    }

    @Override
    public void remove() {
        if (!this.level.isClientSide && !this.assembling && this.parent != null) {
            this.level.destroyBlock(this.getHeadPistonPos(), false);
        }

        super.remove();
    }

    private BlockPos getHeadPistonPos() {
        if (this.level == null || this.parent == null) {
            return this.worldPosition;
        }

        final BlockState parentState = this.level.getBlockState(this.parent);
        if (!(parentState.getBlock() instanceof SimulatedPistonBlock)) {
            return this.parent;
        }

        final Direction facing = parentState.getValue(SimulatedPistonBlock.FACING);
        return this.parent.relative(facing, Math.max(0, this.chainLength - 1));
    }

    @Override
    public float propagateRotationTo(final KineticBlockEntity target, final BlockState stateFrom, final BlockState stateTo, final BlockPos diff, final boolean connectedViaAxes, final boolean connectedViaCogs) {
        return this.parent != null && target.equals(this.level.getBlockEntity(this.parent)) ? 1 : super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
    }

    @Override
    public boolean isCustomConnection(final KineticBlockEntity other, final BlockState state, final BlockState otherState) {
        return this.parent != null && other.equals(this.level.getBlockEntity(this.parent));
    }

    @Override
    public List<BlockPos> addPropagationLocations(final IRotate block, final BlockState state, final List<BlockPos> neighbours) {
        if (this.parent != null) {
            neighbours.add(this.parent);
        }

        return super.addPropagationLocations(block, state, neighbours);
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        if (this.parent == null) {
            return;
        }

        final BlockEntity parentBE = this.level.getBlockEntity(this.parent);
        if (parentBE instanceof final SimulatedPistonBlockEntity piston) {
            piston.updatePistonConstraintMotor();
        }
    }

    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getConnectionDependencies() {
        if (this.parentSubLevelId == null || this.level == null) {
            return null;
        }

        final SubLevel subLevel = SubLevelContainer.getContainer(this.level).getSubLevel(this.parentSubLevelId);
        return subLevel != null ? List.of(subLevel) : null;
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (this.parent != null) {
            tag.put("ParentPos", NbtUtils.writeBlockPos(this.parent));
        }
        if (this.parentSubLevelId != null) {
            tag.putUUID("ParentSubLevelId", this.parentSubLevelId);
        }
        tag.putInt("ChainLength", this.chainLength);
        tag.putFloat("ParentExtension", this.parentExtension);
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.parent = tag.contains("ParentPos") ? NbtUtils.readBlockPos(tag, "ParentPos").orElse(null) : null;
        this.parentSubLevelId = tag.hasUUID("ParentSubLevelId") ? tag.getUUID("ParentSubLevelId") : null;
        this.chainLength = Math.max(1, tag.getInt("ChainLength"));
        this.parentExtension = Math.min(Math.max(0, tag.getFloat("ParentExtension")), this.chainLength);
    }
}
