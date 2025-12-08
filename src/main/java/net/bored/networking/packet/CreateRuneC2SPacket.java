package net.bored.networking.packet;

import net.bored.access.AetherAttachment;
import net.bored.data.AetherChunkData;
import net.bored.registry.ModBlocks;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.block.BlockState;
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
        // Switch to server thread
        server.execute(() -> {
            ServerWorld world = (ServerWorld) player.getWorld();
            Chunk chunk = world.getChunk(player.getBlockPos());

            if (chunk instanceof AetherAttachment attachment) {
                AetherChunkData data = attachment.getAetherData();

                // Define "Good Enough" pressure (e.g., > 50% pressure)
                float pressureThreshold = 0.5f;

                if (data.getPressure() >= pressureThreshold) {
                    // Raycast to find target
                    HitResult hit = player.raycast(5.0, 0.0f, false);

                    if (hit.getType() == HitResult.Type.BLOCK) {
                        BlockHitResult blockHit = (BlockHitResult) hit;
                        BlockPos targetPos = blockHit.getBlockPos();
                        Direction side = blockHit.getSide();
                        BlockState targetState = world.getBlockState(targetPos);

                        BlockPos placePos;

                        // 1. Check if the block we are looking at is replaceable (Grass, Snow, Water, etc.)
                        if (targetState.isReplaceable()) {
                            // If replaceable, we place the rune IN this block
                            placePos = targetPos;
                        } else {
                            // 2. If solid, we must be looking at the TOP face to place above it
                            if (side != Direction.UP) {
                                player.sendMessage(Text.literal("§cRunes must be placed on the top of a block."), true);
                                return;
                            }
                            placePos = targetPos.offset(Direction.UP);
                        }

                        // 3. Verify Support: Check the block BELOW the placement spot
                        BlockPos supportPos = placePos.down();
                        BlockState supportState = world.getBlockState(supportPos);

                        // It must be a full solid face on top (prevents bottom slabs, stairs, fences, etc.)
                        // AND it cannot be another rune (prevents stacking)
                        boolean isSolidSupport = supportState.isSideSolidFullSquare(world, supportPos, Direction.UP);
                        boolean isRune = supportState.getBlock() == ModBlocks.AETHER_RUNE;

                        if (!isSolidSupport || isRune) {
                            player.sendMessage(Text.literal("§cRunes require a solid, flat surface to rest on."), true);
                            return;
                        }

                        // 4. Final check: Is the placement spot itself valid (air/replaceable)?
                        BlockState placeState = world.getBlockState(placePos);
                        if (placeState.isReplaceable()) {

                            // Check for Waterlogging
                            FluidState fluidState = placeState.getFluidState();
                            boolean isWater = fluidState.getFluid() == Fluids.WATER;

                            // Prepare state: Set Waterlogged property if placed in water
                            BlockState runeState = ModBlocks.AETHER_RUNE.getDefaultState()
                                    .with(Properties.WATERLOGGED, isWater);

                            // Place the Rune
                            world.setBlockState(placePos, runeState);

                            // --- Visuals & Audio ---

                            // Sound: A mix of magical chime and air pressure release
                            // If underwater, maybe pitch it down slightly?
                            float pitch = isWater ? 0.8f : 1.0f;
                            world.playSound(null, placePos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0f, pitch);
                            world.playSound(null, placePos, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.BLOCKS, 0.5f, 2.0f); // High pitch whoosh

                            // Particles: Magical rising sparks
                            world.spawnParticles(ParticleTypes.END_ROD,
                                    placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5,
                                    10, 0.2, 0.2, 0.2, 0.05);

                            // Particles: Pressure puff at the base (use Bubble if water)
                            if (isWater) {
                                world.spawnParticles(ParticleTypes.BUBBLE,
                                        placePos.getX() + 0.5, placePos.getY() + 0.1, placePos.getZ() + 0.5,
                                        5, 0.3, 0.05, 0.3, 0.05);
                            } else {
                                world.spawnParticles(ParticleTypes.CLOUD,
                                        placePos.getX() + 0.5, placePos.getY() + 0.1, placePos.getZ() + 0.5,
                                        5, 0.3, 0.05, 0.3, 0.02);
                            }

                            player.sendMessage(Text.literal("§bAether Rune created!"), true);

                            // Optional: Consume a bit of pressure/stability here
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