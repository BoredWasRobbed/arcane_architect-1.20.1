package net.bored.client;

import net.bored.block.RuneBlock;
import net.bored.client.gui.RuneSelectionScreen;
import net.bored.networking.ModMessages;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class KeyInputHandler {
    public static final String KEY_CATEGORY_TUTORIAL = "key.category.arcanearchitect.tutorial";
    public static final String KEY_CREATE_RUNE = "key.arcanearchitect.create_rune";

    public static KeyBinding createRuneKey;

    public static void registerKeyInputs() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (createRuneKey.wasPressed()) {
                handleKeyPress(client);
            }
        });
    }

    private static void handleKeyPress(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        HitResult hit = client.player.raycast(5.0, 0.0f, false);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = client.world.getBlockState(pos);

            // Context 1: Looking at an existing Rune -> Open Menu
            if (state.getBlock() instanceof RuneBlock) {
                client.setScreen(new RuneSelectionScreen(pos));
            }
            // Context 2: Looking at normal block -> Try to Create Rune
            else {
                ClientPlayNetworking.send(ModMessages.CREATE_RUNE_ID, PacketByteBufs.create());
            }
        } else {
            // Context 3: Looking at air -> Try to Create (Server will reject, but handles logic)
            ClientPlayNetworking.send(ModMessages.CREATE_RUNE_ID, PacketByteBufs.create());
        }
    }

    public static void register() {
        createRuneKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_CREATE_RUNE,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KEY_CATEGORY_TUTORIAL
        ));

        registerKeyInputs();
    }
}