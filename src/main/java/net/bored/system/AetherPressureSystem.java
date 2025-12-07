package net.bored.system;

import net.bored.access.AetherAttachment;
import net.bored.data.AetherChunkData;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

public class AetherPressureSystem {

    public static void updateChunkPressure(ServerWorld world, Chunk chunk) {
        AetherChunkData data = ((AetherAttachment) chunk).getAetherData();

        long currentTime = world.getTime();
        if (currentTime < data.getLastUpdateTick() + 100) {
            return;
        }

        // Calculate where the pressure WANTS to be
        float targetPressure = calculateTargetPressure(world, chunk);

        float current = data.getPressure();
        float stability = data.getStability();

        // Increase change rate slightly (0.05 -> 0.10) to make it more responsive
        float changeRate = 0.10f * stability;

        float newPressure = MathHelper.lerp(changeRate, current, targetPressure);

        if (stability < 1.0f) {
            data.setStability(Math.min(1.0f, stability + 0.01f));
        }

        data.setPressure(newPressure);
        data.setLastUpdateTick(currentTime);
    }

    // Changed to public so DebugCommand can see what the target is
    public static float calculateTargetPressure(ServerWorld world, Chunk chunk) {
        BlockPos centerPos = chunk.getPos().getCenterAtY(world.getSeaLevel());
        Biome biome = world.getBiome(centerPos).value();

        float basePressure = 0.5f;

        // 0. Spatial Noise (Increased influence: 0.1 -> 0.2 range)
        long seed = chunk.getPos().toLong();
        float noise = ((float) (seed % 100) / 100.0f) * 0.2f - 0.1f;
        basePressure += noise;

        // 1. Biome Factors (Aggressive Tuning)
        if (world.getBiome(centerPos).isIn(BiomeTags.IS_HILL) || world.getBiome(centerPos).isIn(BiomeTags.IS_MOUNTAIN)) {
            basePressure += 0.3f; // Was 0.2
        }
        if (world.getBiome(centerPos).isIn(BiomeTags.IS_OCEAN)) {
            basePressure += 0.15f; // Oceans are high pressure zones
        }

        // Temperature check
        float temp = biome.getTemperature();
        if (temp > 1.5f) {
            basePressure += 0.25f; // Hot (Desert) -> Major High Pressure
        } else if (temp < 0.2f) {
            basePressure += 0.2f; // Cold -> High Pressure
        }

        // Humidity check (Jungle/Swamp)
        if (world.getBiome(centerPos).isIn(BiomeTags.IS_JUNGLE) || world.getBiome(centerPos).isIn(BiomeTags.HAS_CLOSER_WATER_FOG)) {
            basePressure -= 0.3f; // Humid -> Major Low Pressure
        }

        // 2. Weather Factors
        if (world.isRaining()) {
            basePressure -= 0.15f;
        }
        if (world.isThundering()) {
            basePressure -= 0.3f; // Massive drop
        }

        // 3. Time of Day
        long time = world.getTimeOfDay() % 24000;
        double timeCycle = Math.sin((time / 24000.0) * 2 * Math.PI);
        basePressure += (float) (timeCycle * 0.1f);

        // 4. Altitude Factor (Lowered threshold so it affects more terrain)
        int surfaceHeight = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, centerPos.getX(), centerPos.getZ());

        // Start affecting pressure at Y=70 instead of Y=100
        if (surfaceHeight > 70) {
            basePressure += 0.15f * ((surfaceHeight - 70) / 100.0f);
        }
        // Caves/Deep areas
        else if (surfaceHeight < 50) {
            basePressure -= 0.2f;
        }

        return MathHelper.clamp(basePressure, 0.0f, 1.0f);
    }
}