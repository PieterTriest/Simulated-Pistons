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
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SimulatedPistonLinkBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelActor {
    @Nullable
    private BlockPos parent;
    @Nullable
    private UUID parentSubLevelId;
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
        this.parent = parent.getBlockPos();
        this.parentSubLevelId = parentSubLevel != null ? parentSubLevel.getUniqueId() : null;
        this.setChanged();
        this.sendData();
    }

    @Override
    public void remove() {
        if (!this.level.isClientSide && !this.assembling && this.parent != null) {
            this.level.destroyBlock(this.parent, false);
        }

        super.remove();
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
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.parent = tag.contains("ParentPos") ? NbtUtils.readBlockPos(tag, "ParentPos").orElse(null) : null;
        this.parentSubLevelId = tag.hasUUID("ParentSubLevelId") ? tag.getUUID("ParentSubLevelId") : null;
    }
}
