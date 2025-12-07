package net.bored;

import net.bored.client.render.RuneBlockEntityRenderer;
import net.bored.registry.ModBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public class ArcaneArchitectClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register the procedural renderer
        BlockEntityRendererRegistry.register(ModBlockEntities.RUNE_BLOCK_ENTITY, RuneBlockEntityRenderer::new);
    }
}