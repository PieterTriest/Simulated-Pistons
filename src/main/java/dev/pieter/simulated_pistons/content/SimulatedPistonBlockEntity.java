package dev.pieter.simulated_pistons.content;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import dev.pieter.simulated_pistons.index.SPBlockEntityTypes;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;

public class SimulatedPistonBlockEntity extends KineticBlockEntity implements ExtraKinetics {
    private final PistonCogBlockEntity cogwheel;
    private int chainLength = 1;
    private float extension;
    private float lastActuatorSpeed;
    private float lastMovementSpeed;
    private float lastTargetExtension;
    @Nullable
    private UUID subLevelId;
    @Nullable
    private BlockPos subLevelAnchor;
    @Nullable
    private BlockPos disassemblyGoal;
    private String lastAssemblyStatus = "idle";

    public SimulatedPistonBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.cogwheel = new PistonCogBlockEntity(type, pos, state, this);
    }

    public static SimulatedPistonBlockEntity create(final BlockPos pos, final BlockState state) {
        return new SimulatedPistonBlockEntity(SPBlockEntityTypes.SIMULATED_PISTON.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        final float actuatorSpeed = this.getActuatorSpeed();
        this.lastActuatorSpeed = actuatorSpeed;
        final float movementSpeed = this.getMovementSpeed(actuatorSpeed);
        this.lastMovementSpeed = movementSpeed;
        this.lastTargetExtension = movementSpeed > 0 ? this.chainLength : 0;

        if (movementSpeed == 0) {
            return;
        }

        final float previousExtension = this.extension;
        this.extension = Mth.clamp(this.extension + movementSpeed, 0, this.chainLength);
        if (this.extension == previousExtension) {
            return;
        }

        this.setChanged();
        this.sendData();
    }

    public void setChainLength(final int chainLength) {
        final int clamped = Math.max(1, chainLength);
        if (this.chainLength == clamped) {
            return;
        }
        this.chainLength = clamped;
        this.extension = Math.min(this.extension, clamped);
        this.setChanged();
        this.sendData();
    }

    public int getChainLength() {
        return this.chainLength;
    }

    public float getExtension() {
        return this.extension;
    }

    public void resetExtension() {
        if (this.isAttachmentAssembled()) {
            this.disassembleAttachment();
            return;
        }

        this.assembleAttachment();
    }

    private void resetExtensionOnly() {
        this.extension = 0;
        this.lastMovementSpeed = 0;
        this.lastTargetExtension = 0;
        this.setChanged();
        this.sendData();
    }

    private float getActuatorSpeed() {
        final float extraCogSpeed = this.cogwheel.getSpeed();
        if (extraCogSpeed != 0 || this.level == null) {
            return extraCogSpeed;
        }

        final BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof SimulatedPistonBlock)) {
            return 0;
        }

        final Direction.Axis pistonAxis = state.getValue(SimulatedPistonBlock.FACING).getAxis();
        for (final Direction direction : Direction.values()) {
            if (direction.getAxis() == pistonAxis) {
                continue;
            }

            final BlockPos neighbourPos = this.worldPosition.relative(direction);
            final BlockState neighbourState = this.level.getBlockState(neighbourPos);
            if (!(neighbourState.getBlock() instanceof ICogWheel cogWheel)) {
                continue;
            }

            if (cogWheel.getRotationAxis(neighbourState) != pistonAxis) {
                continue;
            }

            final BlockEntity neighbour = this.level.getBlockEntity(neighbourPos);
            if (neighbour instanceof final KineticBlockEntity kineticNeighbour) {
                return kineticNeighbour.getSpeed();
            }
        }

        return 0;
    }

    private float getMovementSpeed(final float actuatorSpeed) {
        if (actuatorSpeed == 0) {
            return 0;
        }

        final float movementSpeed = -Mth.clamp(convertToLinear(actuatorSpeed), -.49f, .49f);
        return Mth.clamp(movementSpeed, 0 - this.extension, this.chainLength - this.extension);
    }

    @Override
    public KineticBlockEntity getExtraKinetics() {
        return this.cogwheel;
    }

    @Override
    public boolean shouldConnectExtraKinetics() {
        return false;
    }

    @Override
    public String getExtraKineticsSaveName() {
        return "PistonCog";
    }

    public boolean isAttachmentAssembled() {
        return this.subLevelId != null;
    }

    public void assembleAttachment() {
        if (this.level == null || this.level.isClientSide || this.isAttachmentAssembled()) {
            return;
        }

        final BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof SimulatedPistonBlock)) {
            return;
        }

        final Direction facing = state.getValue(SimulatedPistonBlock.FACING);
        final BlockPos headPos = this.worldPosition.relative(facing, Math.max(0, this.chainLength - 1));
        final BlockPos toAssemble = headPos.relative(facing);

        try {
            final Class<?> helperClass = Class.forName("dev.simulated_team.simulated.util.SimAssemblyHelper");
            final Method assemble = helperClass.getMethod(
                    "assembleFromSingleBlock",
                    net.minecraft.world.level.Level.class,
                    BlockPos.class,
                    BlockPos.class,
                    boolean.class,
                    boolean.class
            );

            final Object result = assemble.invoke(null, this.level, headPos, toAssemble, false, false);
            if (result == null) {
                this.lastAssemblyStatus = "nothing_to_assemble";
                this.setChanged();
                this.sendData();
                return;
            }

            final Object subLevel = result.getClass().getMethod("subLevel").invoke(result);
            final BlockPos offset = (BlockPos) result.getClass().getMethod("offset").invoke(result);
            this.subLevelId = (UUID) subLevel.getClass().getMethod("getUniqueId").invoke(subLevel);
            this.subLevelAnchor = toAssemble.offset(offset);
            this.disassemblyGoal = toAssemble;
            this.lastAssemblyStatus = "assembled";
            this.setChanged();
            this.sendData();
        } catch (final ReflectiveOperationException e) {
            this.lastAssemblyStatus = e.getClass().getSimpleName();
            this.setChanged();
            this.sendData();
        }
    }

    public void disassembleAttachment() {
        if (this.level == null || this.level.isClientSide || this.subLevelId == null || this.subLevelAnchor == null || this.disassemblyGoal == null) {
            this.resetExtensionOnly();
            return;
        }

        try {
            final Class<?> containerClass = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            final Object container = containerClass
                    .getMethod("getContainer", net.minecraft.world.level.Level.class)
                    .invoke(null, this.level);
            final Object subLevel = container
                    .getClass()
                    .getMethod("getSubLevel", UUID.class)
                    .invoke(container, this.subLevelId);

            if (subLevel != null) {
                final Class<?> helperClass = Class.forName("dev.simulated_team.simulated.util.SimAssemblyHelper");
                final Class<?> subLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
                helperClass
                        .getMethod(
                                "disassembleSubLevel",
                                net.minecraft.world.level.Level.class,
                                subLevelClass,
                                BlockPos.class,
                                BlockPos.class,
                                Rotation.class,
                                boolean.class
                        )
                        .invoke(null, this.level, subLevel, this.subLevelAnchor, this.disassemblyGoal, Rotation.NONE, true);
            }

            this.subLevelId = null;
            this.subLevelAnchor = null;
            this.disassemblyGoal = null;
            this.lastAssemblyStatus = "disassembled";
            this.resetExtensionOnly();
        } catch (final ReflectiveOperationException e) {
            this.lastAssemblyStatus = e.getClass().getSimpleName();
            this.setChanged();
            this.sendData();
        }
    }

    @Override
    public float calculateStressApplied() {
        return 0;
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("ChainLength", this.chainLength);
        tag.putFloat("Extension", this.extension);
        tag.putFloat("LastActuatorSpeed", this.lastActuatorSpeed);
        tag.putFloat("LastMovementSpeed", this.lastMovementSpeed);
        tag.putFloat("LastTargetExtension", this.lastTargetExtension);
        tag.putString("LastAssemblyStatus", this.lastAssemblyStatus);
        if (this.subLevelId != null) {
            tag.putUUID("SubLevelID", this.subLevelId);
        }
        if (this.subLevelAnchor != null) {
            tag.put("SubLevelAnchor", NbtUtils.writeBlockPos(this.subLevelAnchor));
        }
        if (this.disassemblyGoal != null) {
            tag.put("DisassemblyGoal", NbtUtils.writeBlockPos(this.disassemblyGoal));
        }
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.chainLength = Math.max(1, tag.getInt("ChainLength"));
        this.extension = Math.min(tag.getFloat("Extension"), this.chainLength);
        this.lastAssemblyStatus = tag.getString("LastAssemblyStatus");
        this.subLevelId = tag.hasUUID("SubLevelID") ? tag.getUUID("SubLevelID") : null;
        this.subLevelAnchor = tag.contains("SubLevelAnchor") ? NbtUtils.readBlockPos(tag, "SubLevelAnchor").orElse(null) : null;
        this.disassemblyGoal = tag.contains("DisassemblyGoal") ? NbtUtils.readBlockPos(tag, "DisassemblyGoal").orElse(null) : null;
    }

    public static class PistonCogBlockEntity extends KineticBlockEntity implements ExtraKinetics.ExtraKineticsBlockEntity {
        public static final ICogWheel EXTRA_COGWHEEL_CONFIG = new ICogWheel() {
            @Override
            public boolean hasShaftTowards(final LevelReader level, final BlockPos pos, final BlockState state, final Direction face) {
                return false;
            }

            @Override
            public Direction.Axis getRotationAxis(final BlockState state) {
                return state.getValue(SimulatedPistonBlock.FACING).getAxis();
            }
        };

        private final SimulatedPistonBlockEntity parent;

        public PistonCogBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state, final SimulatedPistonBlockEntity parent) {
            super(type, new ExtraBlockPos(pos), state);
            this.parent = parent;
        }

        @Override
        public KineticBlockEntity getParentBlockEntity() {
            return this.parent;
        }

        @Override
        protected boolean canPropagateDiagonally(final IRotate block, final BlockState state) {
            return true;
        }

        @Override
        public Component getKey() {
            return Component.translatable("block.simulated_pistons.simulated_piston.cog");
        }
    }
}
