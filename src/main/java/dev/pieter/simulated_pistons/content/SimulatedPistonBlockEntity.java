package dev.pieter.simulated_pistons.content;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.free.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.free.FreeConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.pieter.simulated_pistons.SimulatedPistons;
import dev.pieter.simulated_pistons.index.SPBlockEntityTypes;
import dev.pieter.simulated_pistons.index.SPBlocks;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

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
    @Nullable
    private BlockPos linkPos;
    private String lastAssemblyStatus = "idle";
    private String lastMotionStatus = "idle";
    private double baseSubLevelX;
    private double baseSubLevelY;
    private double baseSubLevelZ;
    private double baseParentLocalX;
    private double baseParentLocalY;
    private double baseParentLocalZ;
    private float lastAppliedExtension;
    private boolean hasAssemblyPayload;
    private boolean assembleNextTick;
    private boolean toggleAssemblyNextTick;
    private boolean assemblySuppressedUntilStopped;
    private float assemblySuppressedSpeed;
    @Nullable
    private FreeConstraintHandle pistonConstraint;
    private int debugTickCounter;
    private float debugLastActuatorSpeed = Float.NaN;
    private float debugLastExtension = Float.NaN;
    private String debugLastActuatorSource = "unset";
    @Nullable
    private BlockPos debugLastActuatorSourcePos;

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
        this.cogwheel.tick();
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        final float actuatorSpeed = this.getActuatorSpeed();
        this.lastActuatorSpeed = actuatorSpeed;
        this.updateAssemblySuppression(actuatorSpeed);
        this.debugInputTick("after_speed_read", actuatorSpeed, 0, false);

        final boolean toggledAssembly = this.toggleAssemblyNextTick;
        if (this.toggleAssemblyNextTick) {
            this.toggleAssemblyNextTick = false;
            if (this.isAttachmentAssembled()) {
                this.disassembleAttachment();
                if (actuatorSpeed != 0) {
                    this.assemblySuppressedUntilStopped = true;
                    this.assemblySuppressedSpeed = actuatorSpeed;
                    this.assembleNextTick = false;
                    this.setChanged();
                    this.sendData();
                }
            } else {
                this.assemblySuppressedUntilStopped = false;
                this.assemblySuppressedSpeed = 0;
                this.assembleAttachment();
            }
        }

        if (!this.assemblySuppressedUntilStopped
                && (this.assembleNextTick || (!toggledAssembly && actuatorSpeed != 0))
                && !this.isAttachmentAssembled()) {
            this.assembleAttachment();
        }
        this.assembleNextTick = false;

        final float movementSpeed = this.getMovementSpeed(actuatorSpeed);
        this.lastMovementSpeed = movementSpeed;
        this.lastTargetExtension = movementSpeed > 0 ? this.chainLength : 0;

        final float previousExtension = this.extension;
        if (movementSpeed != 0) {
            this.extension = Mth.clamp(this.extension + movementSpeed, 0, this.chainLength);
        }

        final boolean extensionChanged = this.extension != previousExtension;
        if (this.isAttachmentAssembled()) {
            this.ensurePistonConstraint();
            this.updatePistonConstraintMotor();
            this.moveAssembledSubLevel();
            this.lastAppliedExtension = this.extension;
        }
        this.debugInputTick("after_motion", actuatorSpeed, movementSpeed, extensionChanged);

        if (extensionChanged) {
            this.setChanged();
            this.sendData();
        }
    }

    private void debugInputTick(final String phase, final float actuatorSpeed, final float movementSpeed, final boolean extensionChanged) {
        this.debugTickCounter++;
        final boolean speedChanged = actuatorSpeed != this.debugLastActuatorSpeed;
        final boolean extensionChangedSinceLastLog = this.extension != this.debugLastExtension;
        final boolean periodic = this.isAttachmentAssembled() && this.debugTickCounter % 80 == 0;
        if (!speedChanged && !extensionChanged && !extensionChangedSinceLastLog && !periodic) {
            return;
        }

        this.debugLastActuatorSpeed = actuatorSpeed;
        this.debugLastExtension = this.extension;
        SimulatedPistons.LOGGER.info("[SP INPUT DEBUG] {} controller={} assembled={} source={} sourcePos={} actuatorSpeed={} movementSpeed={} extension={} target={} chainLength={} constraint={} subLevelId={} linkPos={} suppressed={}/{}",
                phase,
                this.worldPosition,
                this.isAttachmentAssembled(),
                this.debugLastActuatorSource,
                this.debugLastActuatorSourcePos,
                actuatorSpeed,
                movementSpeed,
                this.extension,
                this.lastTargetExtension,
                this.chainLength,
                this.pistonConstraint == null ? "null" : this.pistonConstraint.isValid(),
                this.subLevelId,
                this.linkPos,
                this.assemblySuppressedUntilStopped,
                this.assemblySuppressedSpeed);
    }

    private void updateAssemblySuppression(final float actuatorSpeed) {
        if (!this.assemblySuppressedUntilStopped) {
            return;
        }

        if (actuatorSpeed != 0 && actuatorSpeed == this.assemblySuppressedSpeed) {
            return;
        }

        this.assemblySuppressedUntilStopped = false;
        this.assemblySuppressedSpeed = 0;
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
        if (this.isControllerSegment()) {
            this.toggleAssemblyNextTick = true;
            this.setChanged();
        }
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
            this.debugLastActuatorSource = extraCogSpeed != 0 ? "extra_cog" : "no_level";
            this.debugLastActuatorSourcePos = this.worldPosition;
            return extraCogSpeed;
        }

        final BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof SimulatedPistonBlock)) {
            this.debugLastActuatorSource = "not_piston";
            this.debugLastActuatorSourcePos = this.worldPosition;
            return 0;
        }

        final Direction.Axis pistonAxis = state.getValue(SimulatedPistonBlock.FACING).getAxis();
        this.debugLastActuatorSource = "none";
        this.debugLastActuatorSourcePos = null;
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
                this.debugLastActuatorSource = "neighbor_" + direction.getName();
                this.debugLastActuatorSourcePos = neighbourPos;
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

    private boolean isControllerSegment() {
        final BlockState state = this.getBlockState();
        return state.getBlock() instanceof SimulatedPistonBlock
                && isControllerSegment(state.getValue(SimulatedPistonBlock.SEGMENT));
    }

    private static boolean isControllerSegment(final SimulatedPistonBlock.Segment segment) {
        return segment == SimulatedPistonBlock.Segment.SINGLE || segment == SimulatedPistonBlock.Segment.CONTROLLER;
    }

    private void requestAssemblyNextTick() {
        this.updateAssemblySuppression(this.getActuatorSpeed());
        if (this.isControllerSegment() && !this.assemblySuppressedUntilStopped) {
            this.assembleNextTick = true;
            this.setChanged();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.removePistonConstraint();
    }

    public void assembleAttachment() {
        if (this.level == null || this.level.isClientSide || this.isAttachmentAssembled() || !this.isControllerSegment()) {
            return;
        }

        final BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof SimulatedPistonBlock)) {
            return;
        }

        final Direction facing = state.getValue(SimulatedPistonBlock.FACING);
        final BlockPos headPos = this.worldPosition.relative(facing, Math.max(0, this.chainLength - 1));
        final BlockPos toAssemble = headPos.relative(facing);
        final boolean emptyFront = this.level.getBlockState(toAssemble).isAir();

        try {
            final Object result = emptyFront ? null : this.assemblePayload(headPos, toAssemble);

            if (result == null) {
                if (!emptyFront) {
                    this.lastAssemblyStatus = "nothing_to_assemble";
                    this.setChanged();
                    this.sendData();
                    return;
                }

                this.assembleEmpty(facing, headPos, toAssemble);
                return;
            }

            final SubLevel subLevel = (SubLevel) result.getClass().getMethod("subLevel").invoke(result);
            final BlockPos offset = (BlockPos) result.getClass().getMethod("offset").invoke(result);
            this.subLevelId = (UUID) subLevel.getClass().getMethod("getUniqueId").invoke(subLevel);
            this.subLevelAnchor = toAssemble.offset(offset);
            this.disassemblyGoal = toAssemble;
            this.hasAssemblyPayload = true;
            this.linkPos = headPos.offset(offset);
            this.placeLinkBlock(facing);
            this.setHeadAssembled(facing, true);
            this.captureBaseSubLevelPosition(subLevel);
            if (subLevel instanceof final SubLevel attached) {
                this.attachPistonConstraint(attached, facing);
            }
            this.lastAppliedExtension = this.extension;
            this.lastAssemblyStatus = "assembled";
            this.lastMotionStatus = "assembled";
            this.setChanged();
            this.sendData();
        } catch (final ReflectiveOperationException e) {
            this.lastAssemblyStatus = e.getClass().getSimpleName();
            this.setChanged();
            this.sendData();
        }
    }

    private Object assemblePayload(final BlockPos headPos, final BlockPos toAssemble) throws ReflectiveOperationException {
        final Class<?> helperClass = Class.forName("dev.simulated_team.simulated.util.SimAssemblyHelper");
        final Method assemble = helperClass.getMethod(
                "assembleFromSingleBlock",
                net.minecraft.world.level.Level.class,
                BlockPos.class,
                BlockPos.class,
                boolean.class,
                boolean.class
        );
        return assemble.invoke(null, this.level, headPos, toAssemble, false, false);
    }

    private Vec3 projectedCenter(final BlockPos pos) {
        if (this.level == null) {
            return Vec3.atCenterOf(pos);
        }

        return Sable.HELPER.projectOutOfSubLevel(this.level, Vec3.atCenterOf(pos));
    }

    private void assembleEmpty(final Direction facing, final BlockPos headPos, final BlockPos toAssemble) {
        if (!(this.level instanceof final ServerLevel serverLevel)) {
            return;
        }

        final ServerSubLevelContainer container = SubLevelContainer.getContainer(serverLevel);
        final Vec3 projectedHeadCenter = this.projectedCenter(headPos);
        final Vector3d projectedHeadLowerCorner = new Vector3d(
                projectedHeadCenter.x - .5,
                projectedHeadCenter.y - .5,
                projectedHeadCenter.z - .5
        );
        final Pose3d pose = new Pose3d();
        pose.position().set(projectedHeadCenter.x, projectedHeadCenter.y, projectedHeadCenter.z);

        final ServerSubLevel subLevel = (ServerSubLevel) container.allocateNewSubLevel(pose);
        final LevelPlot plot = subLevel.getPlot();
        final ChunkPos center = plot.getCenterChunk();
        plot.newEmptyChunk(center);
        plot.getEmbeddedLevelAccessor().setBlock(
                BlockPos.ZERO,
                SPBlocks.SIMULATED_PISTON_LINK.get().defaultBlockState().setValue(SimulatedPistonLinkBlock.FACING, facing),
                3
        );

        final BlockPos plotAnchor = plot.getCenterBlock();
        final Vector3dc centerOfMass = subLevel.getMassTracker().getCenterOfMass();
        final Vector3d subLevelPosition = new Vector3d(projectedHeadLowerCorner);
        if (centerOfMass != null) {
            subLevelPosition.add(centerOfMass.x() - plotAnchor.getX(), centerOfMass.y() - plotAnchor.getY(), centerOfMass.z() - plotAnchor.getZ());
        } else {
            subLevel.logicalPose().rotationPoint().set(plotAnchor.getX() + .5, plotAnchor.getY() + .5, plotAnchor.getZ() + .5);
        }
        subLevel.logicalPose().position().set(subLevelPosition.x, subLevelPosition.y, subLevelPosition.z);

        final BlockPos offset = plotAnchor.subtract(headPos);
        this.subLevelId = subLevel.getUniqueId();
        this.subLevelAnchor = toAssemble.offset(offset);
        this.disassemblyGoal = toAssemble;
        this.linkPos = headPos.offset(offset);
        this.hasAssemblyPayload = false;
        this.setHeadAssembled(facing, true);
        this.captureEmptyBasePosition(subLevel);

        final PhysicsPipeline pipeline = container.physicsSystem().getPipeline();
        pipeline.teleport(subLevel, subLevel.logicalPose().position(), subLevel.logicalPose().orientation());
        subLevel.updateLastPose();

        if (this.level.getBlockEntity(this.linkPos) instanceof final SimulatedPistonLinkBlockEntity link) {
            link.setParent(this);
        }
        this.attachPistonConstraint(subLevel, facing);
        this.lastAppliedExtension = this.extension;
        this.lastAssemblyStatus = "assembled";
        this.lastMotionStatus = "assembled";
        this.setChanged();
        this.sendData();
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

            this.extension = 0;
            this.moveAssembledSubLevel();
            this.removePistonConstraint();

            final SubLevel subLevel = (SubLevel) this.getSubLevel(container);

            if (subLevel != null) {
                this.destroyLinkBlock();
                if (!subLevel.isRemoved()) {
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
            }

            this.subLevelId = null;
            this.subLevelAnchor = null;
            this.disassemblyGoal = null;
            this.linkPos = null;
            this.hasAssemblyPayload = false;
            this.setHeadAssembled(this.getBlockState().getValue(SimulatedPistonBlock.FACING), false);
            this.lastAppliedExtension = 0;
            this.lastAssemblyStatus = "disassembled";
            this.lastMotionStatus = "idle";
            this.resetExtensionOnly();
        } catch (final ReflectiveOperationException e) {
            this.lastAssemblyStatus = e.getClass().getSimpleName();
            this.setChanged();
            this.sendData();
        }
    }

    public void cleanupAfterPistonRemoved() {
        if (this.level == null || this.level.isClientSide || !this.isAttachmentAssembled()) {
            return;
        }

        this.disassembleAttachment();
    }

    private void captureBaseSubLevelPosition(final Object subLevel) throws ReflectiveOperationException {
        final Object position = this.getSubLevelPosition(subLevel);
        this.baseSubLevelX = this.readDouble(position, "x");
        this.baseSubLevelY = this.readDouble(position, "y");
        this.baseSubLevelZ = this.readDouble(position, "z");
        this.captureBaseParentLocalPosition();
    }

    private void placeLinkBlock(final Direction facing) {
        if (this.level == null || this.linkPos == null) {
            return;
        }

        this.level.setBlockAndUpdate(this.linkPos, SPBlocks.SIMULATED_PISTON_LINK.get().defaultBlockState().setValue(SimulatedPistonLinkBlock.FACING, facing));
        if (this.level.getBlockEntity(this.linkPos) instanceof final SimulatedPistonLinkBlockEntity link) {
            link.setParent(this);
        }
    }

    private void destroyLinkBlock() {
        if (this.level == null || this.linkPos == null) {
            return;
        }

        if (!this.level.getBlockState(this.linkPos).is(SPBlocks.SIMULATED_PISTON_LINK.get())) {
            return;
        }

        this.prepareLinkForCleanup();
        this.level.setBlock(this.linkPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
    }

    private void prepareLinkForCleanup() {
        if (this.level == null || this.linkPos == null) {
            return;
        }

        if (this.level.getBlockEntity(this.linkPos) instanceof final SimulatedPistonLinkBlockEntity link) {
            link.beforeCleanup();
        }
    }

    private void setHeadAssembled(final Direction facing, final boolean assembled) {
        if (this.level == null) {
            return;
        }

        final BlockPos headPos = this.worldPosition.relative(facing, Math.max(0, this.chainLength - 1));
        final BlockState headState = this.level.getBlockState(headPos);
        if (headState.getBlock() instanceof SimulatedPistonBlock && headState.getValue(SimulatedPistonBlock.SEGMENT).hasAttachmentFace()) {
            this.level.setBlockAndUpdate(headPos, headState.setValue(SimulatedPistonBlock.ASSEMBLED, assembled));
        }
    }

    private void attachPistonConstraint(final SubLevel attached, final Direction facing) {
        if (!(this.level instanceof ServerLevel serverLevel) || !(attached instanceof ServerSubLevel attachedServerSubLevel)) {
            return;
        }

        this.removePistonConstraint();

        final ServerSubLevelContainer container = SubLevelContainer.getContainer(serverLevel);
        final PhysicsPipeline pipeline = container.physicsSystem().getPipeline();
        final ServerSubLevel containing = (ServerSubLevel) Sable.HELPER.getContaining(this);
        final BlockPos headPos = this.worldPosition.relative(facing, this.chainLength);
        final BlockPos attachmentPos = this.linkPos != null ? this.linkPos.relative(facing) : headPos;
        final FreeConstraintConfiguration config = new FreeConstraintConfiguration(
                new Vector3d(headPos.getX() + .5, headPos.getY() + .5, headPos.getZ() + .5),
                new Vector3d(attachmentPos.getX() + .5, attachmentPos.getY() + .5, attachmentPos.getZ() + .5),
                new Quaterniond()
        );

        this.pistonConstraint = pipeline.addConstraint(containing, attachedServerSubLevel, config);
        this.updatePistonConstraintMotor();
    }

    private void ensurePistonConstraint() {
        if (this.pistonConstraint != null && this.pistonConstraint.isValid()) {
            return;
        }

        this.pistonConstraint = null;
        if (this.level == null || this.level.isClientSide || this.subLevelId == null) {
            return;
        }

        final BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof SimulatedPistonBlock)) {
            return;
        }

        try {
            final ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel) this.level);
            final SubLevel subLevel = container.getSubLevel(this.subLevelId);
            if (subLevel != null) {
                this.attachPistonConstraint(subLevel, state.getValue(SimulatedPistonBlock.FACING));
            } else {
                SimulatedPistons.LOGGER.info("[SP INPUT DEBUG] ensure constraint: missing sublevel controller={} subLevelId={} linkPos={} extension={}", this.worldPosition, this.subLevelId, this.linkPos, this.extension);
            }
        } catch (final RuntimeException e) {
            SimulatedPistons.LOGGER.info("[SP INPUT DEBUG] ensure constraint exception: controller={} type={} message={}", this.worldPosition, e.getClass().getSimpleName(), e.getMessage());
            this.lastMotionStatus = e.getClass().getSimpleName();
        }
    }

    void updatePistonConstraintMotor() {
        if (this.pistonConstraint == null) {
            return;
        }

        if (!this.pistonConstraint.isValid()) {
            SimulatedPistons.LOGGER.info("[SP INPUT DEBUG] motor update: invalid constraint controller={} subLevelId={} linkPos={} extension={}", this.worldPosition, this.subLevelId, this.linkPos, this.extension);
            this.pistonConstraint = null;
            return;
        }

        final Direction facing = this.getBlockState().getValue(SimulatedPistonBlock.FACING);
        final int pistonAxisIndex = facing.getAxis().ordinal();
        final double signedExtension = this.extension * facing.getAxisDirection().getStep();

        for (final ConstraintJointAxis angularAxis : ConstraintJointAxis.ANGULAR) {
            this.pistonConstraint.setMotor(angularAxis, 0.0, 100000.0, 2500.0, false, 0.0);
        }

        for (int index = 0; index < ConstraintJointAxis.LINEAR.length; index++) {
            final ConstraintJointAxis linearAxis = ConstraintJointAxis.LINEAR[index];
            final double target = index == pistonAxisIndex ? signedExtension : 0.0;
            this.pistonConstraint.setMotor(linearAxis, target, 100000.0, 2500.0, false, 0.0);
        }
        this.pistonConstraint.setContactsEnabled(false);
    }

    private void removePistonConstraint() {
        if (this.pistonConstraint != null) {
            this.pistonConstraint.remove();
            this.pistonConstraint = null;
        }
    }

    private void captureEmptyBasePosition(final ServerSubLevel subLevel) {
        this.baseSubLevelX = subLevel.logicalPose().position().x();
        this.baseSubLevelY = subLevel.logicalPose().position().y();
        this.baseSubLevelZ = subLevel.logicalPose().position().z();
        this.captureBaseParentLocalPosition();
    }

    private void captureBaseParentLocalPosition() {
        final Vector3d base = new Vector3d(this.baseSubLevelX, this.baseSubLevelY, this.baseSubLevelZ);
        final SubLevel containing = Sable.HELPER.getContaining(this);
        final Vector3d localBase = containing != null ? containing.logicalPose().transformPositionInverse(base) : base;
        this.baseParentLocalX = localBase.x;
        this.baseParentLocalY = localBase.y;
        this.baseParentLocalZ = localBase.z;
    }

    private void moveAssembledSubLevel() {
        if (this.level == null || this.level.isClientSide || this.subLevelId == null) {
            return;
        }

        final BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof SimulatedPistonBlock)) {
            return;
        }

        try {
            final Class<?> containerClass = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            final Object container = containerClass
                    .getMethod("getContainer", net.minecraft.world.level.Level.class)
                    .invoke(null, this.level);
            final Object subLevel = this.getSubLevel(container);
            if (subLevel == null) {
                this.lastMotionStatus = "missing_sublevel";
                return;
            }

            final Direction facing = state.getValue(SimulatedPistonBlock.FACING);
            final Vector3d parentLocalTarget = new Vector3d(
                    this.baseParentLocalX + facing.getStepX() * this.extension,
                    this.baseParentLocalY + facing.getStepY() * this.extension,
                    this.baseParentLocalZ + facing.getStepZ() * this.extension
            );
            final SubLevel containing = Sable.HELPER.getContaining(this);
            final Vector3d target = containing != null ? containing.logicalPose().transformPosition(parentLocalTarget) : parentLocalTarget;
            final Object pose = subLevel.getClass().getMethod("logicalPose").invoke(subLevel);
            final Object position = pose.getClass().getMethod("position").invoke(pose);
            position.getClass().getMethod("set", double.class, double.class, double.class).invoke(position, target.x, target.y, target.z);

            this.teleportSubLevel(container, subLevel, pose, position);
            subLevel.getClass().getMethod("updateLastPose").invoke(subLevel);

            this.lastAppliedExtension = this.extension;
            this.lastMotionStatus = "moved";
        } catch (final ReflectiveOperationException e) {
            this.lastMotionStatus = e.getClass().getSimpleName();
        }
    }

    private Object getSubLevel(final Object container) throws ReflectiveOperationException {
        if (this.subLevelId == null) {
            return null;
        }

        return container
                .getClass()
                .getMethod("getSubLevel", UUID.class)
                .invoke(container, this.subLevelId);
    }

    private void removeSubLevel(final Object container, final Object subLevel) throws ReflectiveOperationException {
        final Object plotPos = this.invokeNoArg(subLevel, "plotPos");
        final int x = this.readInt(plotPos, "x");
        final int z = this.readInt(plotPos, "z");
        final Class<?> reasonClass = Class.forName("dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason");
        final Object removed = Enum.valueOf((Class<? extends Enum>) reasonClass.asSubclass(Enum.class), "REMOVED");
        container.getClass().getMethod("removeSubLevel", int.class, int.class, reasonClass).invoke(container, x, z, removed);
    }

    private Object invokeNoArg(final Object source, final String name) throws ReflectiveOperationException {
        try {
            return source.getClass().getMethod(name).invoke(source);
        } catch (final NoSuchMethodException e) {
            final Method method = source.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            return method.invoke(source);
        }
    }

    private int readInt(final Object source, final String name) throws ReflectiveOperationException {
        try {
            return ((Number) source.getClass().getMethod(name).invoke(source)).intValue();
        } catch (final NoSuchMethodException e) {
            return ((Number) source.getClass().getField(name).get(source)).intValue();
        }
    }

    private Object getSubLevelPosition(final Object subLevel) throws ReflectiveOperationException {
        final Object pose = subLevel.getClass().getMethod("logicalPose").invoke(subLevel);
        return pose.getClass().getMethod("position").invoke(pose);
    }

    private double readDouble(final Object source, final String methodName) throws ReflectiveOperationException {
        return ((Number) source.getClass().getMethod(methodName).invoke(source)).doubleValue();
    }

    private void teleportSubLevel(final Object container, final Object subLevel, final Object pose, final Object position) throws ReflectiveOperationException {
        final Object physicsSystem = container.getClass().getMethod("physicsSystem").invoke(container);
        final Object pipeline = physicsSystem.getClass().getMethod("getPipeline").invoke(physicsSystem);
        final Object orientation = pose.getClass().getMethod("orientation").invoke(pose);

        for (final Method method : pipeline.getClass().getMethods()) {
            if (!"teleport".equals(method.getName()) || method.getParameterCount() != 3) {
                continue;
            }

            method.invoke(pipeline, subLevel, position, orientation);
            return;
        }

        throw new NoSuchMethodException("teleport");
    }

    @Override
    public float propagateRotationTo(final KineticBlockEntity target, final BlockState stateFrom, final BlockState stateTo, final BlockPos diff, final boolean connectedViaAxes, final boolean connectedViaCogs) {
        return this.linkPos != null && stateTo.getBlock() instanceof SimulatedPistonLinkBlock ? 1 : super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
    }

    @Override
    public boolean isCustomConnection(final KineticBlockEntity other, final BlockState state, final BlockState otherState) {
        return this.linkPos != null && otherState.getBlock() instanceof SimulatedPistonLinkBlock;
    }

    @Override
    public List<BlockPos> addPropagationLocations(final IRotate block, final BlockState state, final List<BlockPos> neighbours) {
        if (this.linkPos != null) {
            neighbours.add(this.linkPos);
        }

        return super.addPropagationLocations(block, state, neighbours);
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
        tag.putString("LastMotionStatus", this.lastMotionStatus);
        tag.putBoolean("HasAssemblyPayload", this.hasAssemblyPayload);
        tag.putBoolean("AssemblySuppressedUntilStopped", this.assemblySuppressedUntilStopped);
        tag.putFloat("AssemblySuppressedSpeed", this.assemblySuppressedSpeed);
        tag.putDouble("BaseSubLevelX", this.baseSubLevelX);
        tag.putDouble("BaseSubLevelY", this.baseSubLevelY);
        tag.putDouble("BaseSubLevelZ", this.baseSubLevelZ);
        tag.putDouble("BaseParentLocalX", this.baseParentLocalX);
        tag.putDouble("BaseParentLocalY", this.baseParentLocalY);
        tag.putDouble("BaseParentLocalZ", this.baseParentLocalZ);
        tag.putFloat("LastAppliedExtension", this.lastAppliedExtension);
        if (this.subLevelId != null) {
            tag.putUUID("SubLevelID", this.subLevelId);
        }
        if (this.subLevelAnchor != null) {
            tag.put("SubLevelAnchor", NbtUtils.writeBlockPos(this.subLevelAnchor));
        }
        if (this.disassemblyGoal != null) {
            tag.put("DisassemblyGoal", NbtUtils.writeBlockPos(this.disassemblyGoal));
        }
        if (this.linkPos != null) {
            tag.put("LinkPos", NbtUtils.writeBlockPos(this.linkPos));
        }
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.chainLength = Math.max(1, tag.getInt("ChainLength"));
        this.extension = Math.min(tag.getFloat("Extension"), this.chainLength);
        this.lastAssemblyStatus = tag.getString("LastAssemblyStatus");
        this.lastMotionStatus = tag.getString("LastMotionStatus");
        this.hasAssemblyPayload = tag.contains("HasAssemblyPayload") ? tag.getBoolean("HasAssemblyPayload") : tag.hasUUID("SubLevelID");
        this.assemblySuppressedUntilStopped = tag.getBoolean("AssemblySuppressedUntilStopped");
        this.assemblySuppressedSpeed = tag.getFloat("AssemblySuppressedSpeed");
        this.baseSubLevelX = tag.getDouble("BaseSubLevelX");
        this.baseSubLevelY = tag.getDouble("BaseSubLevelY");
        this.baseSubLevelZ = tag.getDouble("BaseSubLevelZ");
        this.baseParentLocalX = tag.contains("BaseParentLocalX") ? tag.getDouble("BaseParentLocalX") : this.baseSubLevelX;
        this.baseParentLocalY = tag.contains("BaseParentLocalY") ? tag.getDouble("BaseParentLocalY") : this.baseSubLevelY;
        this.baseParentLocalZ = tag.contains("BaseParentLocalZ") ? tag.getDouble("BaseParentLocalZ") : this.baseSubLevelZ;
        this.lastAppliedExtension = tag.getFloat("LastAppliedExtension");
        this.subLevelId = tag.hasUUID("SubLevelID") ? tag.getUUID("SubLevelID") : null;
        this.subLevelAnchor = tag.contains("SubLevelAnchor") ? NbtUtils.readBlockPos(tag, "SubLevelAnchor").orElse(null) : null;
        this.disassemblyGoal = tag.contains("DisassemblyGoal") ? NbtUtils.readBlockPos(tag, "DisassemblyGoal").orElse(null) : null;
        this.linkPos = tag.contains("LinkPos") ? NbtUtils.readBlockPos(tag, "LinkPos").orElse(null) : null;
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
        public void onSpeedChanged(final float previousSpeed) {
            super.onSpeedChanged(previousSpeed);
            SimulatedPistons.LOGGER.info("[SP INPUT DEBUG] extra cog speed changed: parent={} previous={} current={} assembled={}", this.parent.getBlockPos(), previousSpeed, this.speed, this.parent.isAttachmentAssembled());

            if (this.speed != 0 && !this.parent.isAttachmentAssembled()) {
                this.parent.requestAssemblyNextTick();
            }
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
