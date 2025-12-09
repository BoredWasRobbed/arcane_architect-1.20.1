package net.bored.ritual;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;

public interface RitualEffect {
    void run(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation, UUID owner);
}