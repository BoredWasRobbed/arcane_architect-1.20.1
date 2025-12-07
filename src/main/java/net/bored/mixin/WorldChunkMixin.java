package net.bored.mixin;

import net.bored.access.AetherAttachment;
import net.bored.data.AetherChunkData;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

// Target Chunk instead of WorldChunk so ProtoChunks (loading chunks) also get the data
@Mixin(Chunk.class)
public abstract class WorldChunkMixin implements AetherAttachment {

    @Unique
    private final AetherChunkData aetherData = new AetherChunkData();

    @Override
    public AetherChunkData getAetherData() {
        return this.aetherData;
    }
}