package net.bored.networking.packet;

import net.bored.aether.AetherAttachment;
import net.bored.block.AbstractRuneBlock;
import net.bored.block.entity.RuneBlockEntity;
import net.bored.aether.AetherChunkData;
import net.bored.registry.ModBlocks;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.Chunk;

public class CreateRuneC2SPacket {
    public static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler,
                               PacketByteBuf buf, PacketSender responseSender) {
        server.execute(() -> {
            ServerWorld world = (ServerWorld) player.getWorld();
            Chunk chunk = world.getChunk(player.getBlockPos());

            if (chunk instanceof AetherAttachment attachment) {
                AetherChunkData data = attachment.getAetherData();
                float pressureThreshold = 0.5f;

                if (data.getPressure() >= pressureThreshold) {
                    HitResult hit = player.raycast(5.0, 0.0f, false);

                    if (hit.getType() == HitResult.Type.BLOCK) {
                        BlockHitResult blockHit = (BlockHitResult) hit;
                        BlockPos targetPos = blockHit.getBlockPos();
                        Direction side = blockHit.getSide(); // The face we clicked on
                        BlockState targetState = world.getBlockState(targetPos);

                        BlockPos placePos;

                        if (targetState.isReplaceable()) {
                            placePos = targetPos;
                            // If placing in air/grass, assume UP unless we can infer otherwise
                            side = Direction.UP;
                        } else {
                            // Place adjacent to the face we clicked
                            placePos = targetPos.offset(side);
                        }

                        // Verify Support: The block 'behind' the rune (where we clicked) must be solid
                        BlockPos supportPos = placePos.offset(side.getOpposite());
                        BlockState supportState = world.getBlockState(supportPos);

                        boolean isSolidSupport = supportState.isSideSolidFullSquare(world, supportPos, side);
                        boolean isRune = supportState.getBlock() == ModBlocks.AETHER_RUNE;

                        if (!isSolidSupport || isRune) {
                            player.sendMessage(Text.literal("§cRunes require a solid surface."), true);
                            return;
                        }

                        BlockState placeState = world.getBlockState(placePos);
                        if (placeState.isReplaceable()) {
                            FluidState fluidState = placeState.getFluidState();
                            boolean isWater = fluidState.getFluid() == Fluids.WATER;

                            // Set FACING to the side we clicked (pointing OUT from the wall)
                            BlockState runeState = ModBlocks.AETHER_RUNE.getDefaultState()
                                    .with(Properties.WATERLOGGED, isWater)
                                    .with(AbstractRuneBlock.FACING, side);

                            world.setBlockState(placePos, runeState);

                            BlockEntity be = world.getBlockEntity(placePos);
                            if (be instanceof RuneBlockEntity rune) {
                                rune.setOwner(player.getUuid());
                            }

                            // Visuals
                            float pitch = isWater ? 0.8f : 1.0f;
                            world.playSound(null, placePos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0f, pitch);
                            world.playSound(null, placePos, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.BLOCKS, 0.5f, 2.0f);

                            world.spawnParticles(ParticleTypes.END_ROD,
                                    placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5,
                                    10, 0.2, 0.2, 0.2, 0.05);

                            player.sendMessage(Text.literal("§bAether Rune created!"), true);
                        } else {
                            player.sendMessage(Text.literal("§cCannot place rune here."), true);
                        }
                    } else {
                        player.sendMessage(Text.literal("§eLook at a block to create a rune."), true);
                    }
                } else {
                    player.sendMessage(Text.literal("§cNot enough Aether Pressure here (" + String.format("%.0f%%", data.getPressure() * 100) + "). Need > 50%."), true);
                }
            }
        });
    }
}