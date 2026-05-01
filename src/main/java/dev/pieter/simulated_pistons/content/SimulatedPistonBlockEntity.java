package dev.pieter.simulated_pistons.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.pieter.simulated_pistons.index.SPBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SimulatedPistonBlockEntity extends KineticBlockEntity {
    private int chainLength = 1;
    private float extension;

    public SimulatedPistonBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
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

        final float target = this.getSpeed() > 0 ? this.chainLength : 0;
        if (this.getSpeed() == 0 || this.extension == target) {
            return;
        }

        final float step = Math.min(Math.abs(this.getSpeed()) / 256f, 0.25f);
        if (this.extension < target) {
            this.extension = Math.min(target, this.extension + step);
        } else {
            this.extension = Math.max(target, this.extension - step);
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

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("ChainLength", this.chainLength);
        tag.putFloat("Extension", this.extension);
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.chainLength = Math.max(1, tag.getInt("ChainLength"));
        this.extension = Math.min(tag.getFloat("Extension"), this.chainLength);
    }
}
