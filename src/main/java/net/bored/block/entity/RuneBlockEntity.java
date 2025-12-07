package net.bored.block.entity;

import net.bored.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class RuneBlockEntity extends BlockEntity {

    // Used for procedural animation (rotation, pulsing)
    public float tickCounter = 0;

    public RuneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNE_BLOCK_ENTITY, pos, state);
    }

    // We can add a tick method here if we need server-side logic updates.
    // For visual-only rotation, we usually handle it in the Renderer using world time.

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
    }
}