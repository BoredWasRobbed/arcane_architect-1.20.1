package net.bored.mixin;

import net.bored.access.AetherAttachment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {

    // Save Data
    @Inject(method = "serialize", at = @At("RETURN"))
    private static void onSerialize(ServerWorld world, Chunk chunk, CallbackInfoReturnable<NbtCompound> cir) {
        NbtCompound tag = cir.getReturnValue();
        if (chunk instanceof AetherAttachment attachment) {
            NbtCompound aetherTag = new NbtCompound();
            attachment.getAetherData().writeNbt(aetherTag);
            tag.put("ArcaneArchitectData", aetherTag);
        }
    }

    // Load Data
    @Inject(method = "deserialize", at = @At("RETURN"))
    private static void onDeserialize(ServerWorld world, PointOfInterestStorage poiStorage, ChunkPos pos, NbtCompound nbt, CallbackInfoReturnable<ProtoChunk> cir) {
        Chunk chunk = cir.getReturnValue();
        if (chunk instanceof AetherAttachment attachment && nbt.contains("ArcaneArchitectData")) {
            attachment.getAetherData().readNbt(nbt.getCompound("ArcaneArchitectData"));
        }
    }
}