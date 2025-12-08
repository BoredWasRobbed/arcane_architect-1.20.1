package net.bored.client.render;

import net.bored.block.RuneBlock;
import net.bored.block.enums.RuneType;
import net.bored.block.entity.RuneBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class RuneBlockEntityRenderer implements BlockEntityRenderer<RuneBlockEntity> {

    private static final Identifier BEAM_TEXTURE = new Identifier("textures/entity/beacon_beam.png");

    public RuneBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(RuneBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();

        // 1. Setup Transformation - VISUAL TWEAK: Lowered height
        // Moved from 0.1 to 0.05 to sit closer to the ground
        matrices.translate(0.5, 0.05, 0.5);
        matrices.scale(0.8f, 0.8f, 0.8f);

        long time = entity.getWorld() != null ? entity.getWorld().getTime() : 0;
        float rotation = (time + tickDelta) * 1.5f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

        BlockState state = entity.getCachedState();
        RuneType type = RuneType.AETHER;
        if (state.getBlock() instanceof RuneBlock) {
            type = state.get(RuneBlock.TYPE);
        }

        float activeR = type.r;
        float activeG = type.g;
        float activeB = type.b;

        float dimFactor = 0.1f;
        float baseR = activeR * dimFactor;
        float baseG = activeG * dimFactor;
        float baseB = activeB * dimFactor;

        float r = baseR;
        float g = baseG;
        float b = baseB;

        if (entity.activationTimer > 0) {
            float maxTime = RuneBlockEntity.MAX_ACTIVATION_TIME;
            float progress = (entity.activationTimer - tickDelta) / maxTime;
            float intensity = MathHelper.clamp(progress, 0.0f, 1.0f);

            if (intensity > 0.8f) {
                float flash = (intensity - 0.8f) * 5.0f;
                r = MathHelper.lerp(flash, activeR, 1.0f);
                g = MathHelper.lerp(flash, activeG, 1.0f);
                b = MathHelper.lerp(flash, activeB, 1.0f);
            } else {
                r = MathHelper.lerp(intensity, baseR, activeR);
                g = MathHelper.lerp(intensity, baseG, activeG);
                b = MathHelper.lerp(intensity, baseB, activeB);
            }
        }

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(BEAM_TEXTURE));

        // VISUAL TWEAK: Thinner lines
        float thickness = 0.02f; // Was 0.03f

        switch (type) {
            case AIR -> drawPolygon(matrices, buffer, 3, 0.5f, thickness, light, overlay, r, g, b);
            case FIRE -> drawStar(matrices, buffer, 5, 0.2f, 0.5f, thickness, light, overlay, r, g, b);
            case EARTH -> drawPolygon(matrices, buffer, 4, 0.5f, thickness, light, overlay, r, g, b);
            case WATER -> drawPolygon(matrices, buffer, 5, 0.5f, thickness, light, overlay, r, g, b);
            case AMPLIFY -> {
                matrices.push();
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45));
                drawPolygon(matrices, buffer, 4, 0.6f, thickness, light, overlay, r, g, b);
                matrices.pop();
            }
            case CHANNEL -> drawLines(matrices, buffer, 0.5f, thickness, light, overlay, r, g, b);
            case WALL -> {
                drawBeam(matrices, buffer, -0.5f, 0, 0, 0.5f, 0, 0, thickness * 1.5f, light, overlay, r, g, b);
                drawBeam(matrices, buffer, 0, 0, -0.5f, 0, 0, 0.5f, thickness * 1.5f, light, overlay, r, g, b);
            }
            case ITEM -> {
                drawPolygon(matrices, buffer, 6, 0.5f, thickness, light, overlay, r, g, b);
                ItemStack stack = entity.getStack();
                if (!stack.isEmpty()) {
                    matrices.push();
                    matrices.translate(0, 0.4, 0);
                    matrices.scale(0.8f, 0.8f, 0.8f);
                    MinecraftClient.getInstance().getItemRenderer().renderItem(
                            stack,
                            ModelTransformationMode.FIXED,
                            light,
                            overlay,
                            matrices,
                            vertexConsumers,
                            entity.getWorld(),
                            0
                    );
                    matrices.pop();
                }
            }
            default -> drawPolygon(matrices, buffer, 32, 0.5f, thickness, light, overlay, r, g, b);
        }

        matrices.pop();
    }

    private void drawPolygon(MatrixStack matrices, VertexConsumer buffer, int segments, float radius, float thickness, int light, int overlay, float r, float g, float b) {
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) i / segments * MathHelper.TAU;
            float angle2 = (float) (i + 1) / segments * MathHelper.TAU;
            float x1 = MathHelper.cos(angle1) * radius;
            float z1 = MathHelper.sin(angle1) * radius;
            float x2 = MathHelper.cos(angle2) * radius;
            float z2 = MathHelper.sin(angle2) * radius;
            drawBeam(matrices, buffer, x1, 0, z1, x2, 0, z2, thickness, light, overlay, r, g, b);
        }
    }

    private void drawStar(MatrixStack matrices, VertexConsumer buffer, int points, float innerRadius, float outerRadius, float thickness, int light, int overlay, float r, float g, float b) {
        int segments = points * 2;
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) i / segments * MathHelper.TAU;
            float angle2 = (float) (i + 1) / segments * MathHelper.TAU;
            float r1 = (i % 2 == 0) ? outerRadius : innerRadius;
            float r2 = ((i + 1) % 2 == 0) ? outerRadius : innerRadius;
            float x1 = MathHelper.cos(angle1) * r1;
            float z1 = MathHelper.sin(angle1) * r1;
            float x2 = MathHelper.cos(angle2) * r2;
            float z2 = MathHelper.sin(angle2) * r2;
            drawBeam(matrices, buffer, x1, 0, z1, x2, 0, z2, thickness, light, overlay, r, g, b);
        }
    }

    private void drawLines(MatrixStack matrices, VertexConsumer buffer, float length, float thickness, int light, int overlay, float r, float g, float b) {
        drawBeam(matrices, buffer, -0.2f, 0, -length, -0.2f, 0, length, thickness, light, overlay, r, g, b);
        drawBeam(matrices, buffer, 0.2f, 0, -length, 0.2f, 0, length, thickness, light, overlay, r, g, b);
    }

    private void drawBeam(MatrixStack matrices, VertexConsumer buffer,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float thickness, int light, int overlay,
                          float r, float g, float b) {

        float dx = x2 - x1;
        float dz = z2 - z1;
        float len = MathHelper.sqrt(dx * dx + dz * dz);
        if (len < 0.0001f) return;

        float nx = dx / len;
        float nz = dz / len;

        float px = -nz * thickness;
        float pz = nx * thickness;

        float xSL = x1 + px; float zSL = z1 + pz;
        float xSR = x1 - px; float zSR = z1 - pz;
        float xEL = x2 + px; float zEL = z2 + pz;
        float xER = x2 - px; float zER = z2 - pz;

        // VISUAL TWEAK: Vertical Squashing
        // Instead of y1 +- thickness, we simply use y1 to y1 + small_height
        // This makes them look like flat strips on the ground
        float yMin = y1;
        float yMax = y1 + 0.015f;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float a = 1.0f;

        // TOP
        vertex(buffer, matrix, xSL, yMax, zSL, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xEL, yMax, zEL, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xER, yMax, zER, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xSR, yMax, zSR, r, g, b, a, light, overlay);

        // BOTTOM
        vertex(buffer, matrix, xSL, yMin, zSL, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xSR, yMin, zSR, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xER, yMin, zER, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xEL, yMin, zEL, r, g, b, a, light, overlay);

        // SIDES
        vertex(buffer, matrix, xSL, yMin, zSL, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xEL, yMin, zEL, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xEL, yMax, zEL, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xSL, yMax, zSL, r, g, b, a, light, overlay);

        vertex(buffer, matrix, xSR, yMin, zSR, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xSR, yMax, zSR, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xER, yMax, zER, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xER, yMin, zER, r, g, b, a, light, overlay);

        // CAPS
        vertex(buffer, matrix, xSR, yMin, zSR, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xSL, yMin, zSL, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xSL, yMax, zSL, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xSR, yMax, zSR, r, g, b, a, light, overlay);

        vertex(buffer, matrix, xEL, yMin, zEL, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xER, yMin, zER, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xER, yMax, zER, r, g, b, a, light, overlay);
        vertex(buffer, matrix, xEL, yMax, zEL, r, g, b, a, light, overlay);
    }

    private void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, float r, float g, float b, float a, int light, int overlay) {
        buffer.vertex(matrix, x, y, z)
                .color(r, g, b, a)
                .texture(0.5f, 0.5f)
                .overlay(overlay)
                .light(light)
                .normal(0, 1, 0)
                .next();
    }
}