package net.bored.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.bored.access.AetherAttachment;
import net.bored.data.AetherChunkData;
import net.bored.system.AetherPressureSystem;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.chunk.Chunk;

public class AetherDebugCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("aa")
                .then(CommandManager.literal("pressure")
                        .then(CommandManager.literal("get")
                                .executes(AetherDebugCommand::getPressure))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("value", FloatArgumentType.floatArg(0, 1))
                                        .executes(AetherDebugCommand::setPressure)))
                )
        );
    }

    private static int getPressure(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            Chunk chunk = player.getWorld().getChunk(player.getBlockPos());

            if (chunk instanceof AetherAttachment attachment) {
                AetherChunkData data = attachment.getAetherData();

                // Calculate what the pressure WANTS to be right now
                float target = AetherPressureSystem.calculateTargetPressure((ServerWorld) player.getWorld(), chunk);

                context.getSource().sendMessage(Text.literal("§b[Arcane Architect] §7Chunk Data:"));
                context.getSource().sendMessage(Text.literal("  §fCurrent Pressure: §e" + String.format("%.2f", data.getPressure())));
                context.getSource().sendMessage(Text.literal("  §fTarget Pressure:  §6" + String.format("%.2f", target)));
                context.getSource().sendMessage(Text.literal("  §fStability:        §a" + String.format("%.2f", data.getStability())));

                return 1;
            } else {
                context.getSource().sendError(Text.literal("Chunk data attachment missing."));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error retrieving data."));
            e.printStackTrace();
            return 0;
        }
    }

    private static int setPressure(CommandContext<ServerCommandSource> context) {
        try {
            float newValue = FloatArgumentType.getFloat(context, "value");
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            Chunk chunk = player.getWorld().getChunk(player.getBlockPos());

            if (chunk instanceof AetherAttachment attachment) {
                AetherChunkData data = attachment.getAetherData();
                data.setPressure(newValue);
                context.getSource().sendMessage(Text.literal("§b[Arcane Architect]§r Set Pressure to: §e" + newValue));
                return 1;
            }
            return 0;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Error setting data."));
            return 0;
        }
    }
}