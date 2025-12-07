package net.bored.client.render;

import net.bored.block.entity.RuneBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RuneBlockEntityRenderer implements BlockEntityRenderer<RuneBlockEntity> {

    public RuneBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        // Constructor is empty as we don't need to load models currently
    }

    @Override
    public void render(RuneBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();

        // 1. CENTERING & CLAMPING
        // Move to the center of the block horizontally (0.5), but close to the ground vertically (0.1)
        matrices.translate(0.5, 0.1, 0.5);

        // Scale down further to ensure the circle and its thickness stay within bounds.
        // Radius 0.5 * scale 0.8 = 0.4. Plus thickness, it's safely inside.
        matrices.scale(0.8f, 0.8f, 0.8f);

        // 2. ANIMATION
        long time = entity.getWorld() != null ? entity.getWorld().getTime() : 0;

        // Rotate around the Y axis
        float rotation = (time + tickDelta) * 2.0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

        // Bobbing effect removed as requested

        // 3. DRAWING THE MAGIC CIRCLE
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getSolid());

        float radius = 0.5f; // Radius of the circle
        float thickness = 0.025f; // Thickness of the ring lines
        int segments = 32; // Number of segments to approximate the circle (higher = smoother)

        for (int i = 0; i < segments; i++) {
            // Calculate angles for current and next segment
            float angle1 = (float) i / segments * MathHelper.TAU; // TAU is 2*PI
            float angle2 = (float) (i + 1) / segments * MathHelper.TAU;

            // Calculate coordinates for the start and end of the segment on the XZ plane
            float x1 = MathHelper.cos(angle1) * radius;
            float z1 = MathHelper.sin(angle1) * radius;
            float x2 = MathHelper.cos(angle2) * radius;
            float z2 = MathHelper.sin(angle2) * radius;

            // Draw a thick beam connecting the two points. Y is 0 for a flat circle.
            drawBeam(matrices, buffer, x1, 0, z1, x2, 0, z2, thickness, light, overlay);
        }

        matrices.pop();
    }

    /**
     * Draws a box aligned between two points to simulate a thick line.
     * Simplification: This creates an axis-aligned box for the segment.
     */
    private void drawBeam(MatrixStack matrices, VertexConsumer buffer,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float thickness, int light, int overlay) {

        // Calculate min/max for the box
        float minX = Math.min(x1, x2) - thickness;
        float minY = Math.min(y1, y2) - thickness;
        float minZ = Math.min(z1, z2) - thickness;

        float maxX = Math.max(x1, x2) + thickness;
        float maxY = Math.max(y1, y2) + thickness;
        float maxZ = Math.max(z1, z2) + thickness;

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Color (Cyber Cyan: R=0, G=1, B=1)
        float r = 0.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 1.0f;

        // Draw 6 faces of the beam box

        // Front
        vertex(buffer, matrix, minX, maxY, minZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, maxX, maxY, minZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, maxX, minY, minZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, minX, minY, minZ, r, g, b, a, light, overlay);

        // Back
        vertex(buffer, matrix, minX, minY, maxZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, maxX, minY, maxZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, maxX, maxY, maxZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, minX, maxY, maxZ, r, g, b, a, light, overlay);

        // Top
        vertex(buffer, matrix, minX, maxY, minZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, minX, maxY, maxZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, maxX, maxY, maxZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, maxX, maxY, minZ, r, g, b, a, light, overlay);

        // Bottom
        vertex(buffer, matrix, minX, minY, minZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, maxX, minY, minZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, maxX, minY, maxZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, minX, minY, maxZ, r, g, b, a, light, overlay);

        // Left
        vertex(buffer, matrix, minX, minY, minZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, minX, minY, maxZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, minX, maxY, maxZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, minX, maxY, minZ, r, g, b, a, light, overlay);

        // Right
        vertex(buffer, matrix, maxX, maxY, minZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, maxX, maxY, maxZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, maxX, minY, maxZ, r, g, b, a, light, overlay);
        vertex(buffer, matrix, maxX, minY, minZ, r, g, b, a, light, overlay);
    }

    private void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, float r, float g, float b, float a, int light, int overlay) {
        buffer.vertex(matrix, x, y, z)
                .color(r, g, b, a)
                .texture(0, 0) // No texture
                .overlay(overlay)
                .light(light)
                .normal(0, 1, 0)
                .next();
    }
}