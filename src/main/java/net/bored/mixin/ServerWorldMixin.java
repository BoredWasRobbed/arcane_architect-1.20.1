package net.bored.mixin;

import net.bored.aether.AetherPressureSystem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    // Hook into the method where the server ticks individual chunks.
    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void onTickChunk(WorldChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        // We pass the World and Chunk to our system.
        // The system itself now checks "lastUpdateTick" to ensure it only runs logic
        // once every 5 seconds, preventing lag.
        AetherPressureSystem.updateChunkPressure((ServerWorld) (Object) this, chunk);
    }
}