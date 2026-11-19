package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.Level;

public class CatEngineBlockEntity extends BlockEntity {
    private boolean isRotating = false;
    public float rotation = 0;

    public CatEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAT_ENGINE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CatEngineBlockEntity entity) {
        if (entity.isRotating()) {
            entity.rotation += 8.0f;
            if (entity.rotation >= 360) entity.rotation = 0;
        }
    }

    public void toggleRotation() {
        this.isRotating = !this.isRotating;
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    } // <-- IL MANQUAIT CETTE ACCOMANDE ICI !

    public boolean isRotating() {
        return isRotating;
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        isRotating = nbt.getBoolean("Rotating");
        rotation = nbt.getFloat("Rotation");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putBoolean("Rotating", isRotating);
        nbt.putFloat("Rotation", rotation);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}