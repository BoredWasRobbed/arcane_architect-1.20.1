package net.bored.client;

import net.bored.registry.ModBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.bored.client.render.RuneBlockEntityRenderer;

public class ArcaneArchitectClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register Key Bindings
        KeyInputHandler.register();

        // Register the procedural renderer
        BlockEntityRendererRegistry.register(ModBlockEntities.RUNE_BLOCK_ENTITY, RuneBlockEntityRenderer::new);
    }
}