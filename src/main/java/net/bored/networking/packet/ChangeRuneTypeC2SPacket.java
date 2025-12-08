package net.bored.networking.packet;

import net.bored.block.RuneBlock;
import net.bored.block.enums.RuneType;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class ChangeRuneTypeC2SPacket {
    public static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler,
                               PacketByteBuf buf, PacketSender responseSender) {
        BlockPos pos = buf.readBlockPos();
        RuneType newType = buf.readEnumConstant(RuneType.class);

        server.execute(() -> {
            // Verify the player is close enough to modify
            if (player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) > 64) {
                return;
            }

            BlockState state = player.getWorld().getBlockState(pos);
            if (state.getBlock() instanceof RuneBlock) {
                player.getWorld().setBlockState(pos, state.with(RuneBlock.TYPE, newType));

                player.getWorld().playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                player.sendMessage(Text.literal("§bSelected Rune: §e" + newType.name()), true);
            }
        });
    }
}