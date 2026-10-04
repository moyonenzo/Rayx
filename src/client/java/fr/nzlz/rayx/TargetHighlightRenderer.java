package fr.nzlz.rayx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public final class TargetHighlightRenderer {

    private static final int RESCAN_INTERVAL_TICKS = 10;

    private static final double OUTLINE_EPSILON = 0.002D;

    private static final float GLOW_OUTER_ALPHA = 0.10F;
    private static final float GLOW_OUTER_WIDTH = 7.0F;

    private static final float GLOW_INNER_ALPHA = 0.20F;
    private static final float GLOW_INNER_WIDTH = 4.0F;

    private static final float LINE_ALPHA = 1.0F;
    private static final float LINE_WIDTH = 2.0F;

    private static final Map<BlockPos, TargetBlock> HIGHLIGHTS =
            new HashMap<>();

    private static int ticksUntilRescan = 0;

    private static BlockPos lastScanCenter;

    private TargetHighlightRenderer() {
    }

    public static void initialize() {

        ClientTickEvents.END_CLIENT_TICK.register(
                TargetHighlightRenderer::tick
        );

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(
                TargetHighlightRenderer::render
        );
    }

    private static void tick(Minecraft client) {

        if (client.level == null || client.player == null) {
            HIGHLIGHTS.clear();
            lastScanCenter = null;
            ticksUntilRescan = 0;
            return;
        }

        BlockPos currentCenter =
                client.player.blockPosition();

        boolean playerMoved =
                lastScanCenter == null
                        || !lastScanCenter.equals(currentCenter);

        if (playerMoved || ticksUntilRescan <= 0) {
            rescan(client);
            ticksUntilRescan = RESCAN_INTERVAL_TICKS;
        } else {
            ticksUntilRescan--;
        }
    }

    private static void rescan(Minecraft client) {

        HIGHLIGHTS.clear();

        Map<Block, TargetBlock> targetsByBlock =
                TargetBlocks.getEnabledByBlock();

        if (targetsByBlock.isEmpty()) {
            lastScanCenter =
                    client.player.blockPosition();
            return;
        }

        int radius =
                TargetBlocks.getHighlightRadius();

        BlockPos center =
                client.player.blockPosition();

        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();

        int radiusSquared =
                radius * radius;

        int minX = centerX - radius;
        int maxX = centerX + radius;

        int minY = Math.max(
                client.level.getMinY(),
                centerY - radius
        );

        int maxY = Math.min(
                client.level.getMaxY(),
                centerY + radius
        );

        int minZ = centerZ - radius;
        int maxZ = centerZ + radius;

        BlockPos.MutableBlockPos mutable =
                new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {

            int dx = x - centerX;
            int dxSquared = dx * dx;

            for (int y = minY; y <= maxY; y++) {

                int dy = y - centerY;

                int distanceXY =
                        dxSquared + dy * dy;

                if (distanceXY > radiusSquared) {
                    continue;
                }

                for (int z = minZ; z <= maxZ; z++) {

                    int dz = z - centerZ;

                    if (distanceXY + dz * dz
                            > radiusSquared) {
                        continue;
                    }

                    mutable.set(x, y, z);

                    BlockState state =
                            client.level.getBlockState(
                                    mutable
                            );

                    TargetBlock target =
                            targetsByBlock.get(
                                    state.getBlock()
                            );

                    if (target == null) {
                        continue;
                    }

                    HIGHLIGHTS.put(
                            new BlockPos(x, y, z),
                            target
                    );
                }
            }
        }

        lastScanCenter = center;
    }

    private static void render(
            LevelRenderContext context
    ) {
        if (HIGHLIGHTS.isEmpty()) {
            return;
        }

        SubmitNodeCollector collector =
                context.submitNodeCollector();

        PoseStack poseStack =
                context.poseStack();

        Minecraft client =
                Minecraft.getInstance();

        var camera =
                client.gameRenderer.mainCamera();

        var cameraPos =
                camera.position();

        double cameraX =
                cameraPos.x;

        double cameraY =
                cameraPos.y;

        double cameraZ =
                cameraPos.z;

        /*
         * Outer glow
         */
        collector.submitCustomGeometry(
                poseStack,
                RayxRenderTypes.TARGET_BLOCK_GLOW,
                (pose, vertexConsumer) -> {

                    Matrix4f matrix =
                            pose.pose();

                    for (Map.Entry<BlockPos, TargetBlock> entry
                            : HIGHLIGHTS.entrySet()) {

                        BlockPos pos =
                                entry.getKey();

                        TargetBlock target =
                                entry.getValue();

                        float red =
                                ((target.color() >> 16) & 0xFF)
                                        / 255.0F;

                        float green =
                                ((target.color() >> 8) & 0xFF)
                                        / 255.0F;

                        float blue =
                                (target.color() & 0xFF)
                                        / 255.0F;

                        drawOutline(
                                matrix,
                                vertexConsumer,
                                pos,
                                cameraX,
                                cameraY,
                                cameraZ,
                                red,
                                green,
                                blue,
                                GLOW_OUTER_ALPHA,
                                GLOW_OUTER_WIDTH
                        );
                    }
                }
        );

        /*
         * Inner glow
         */
        collector.submitCustomGeometry(
                poseStack,
                RayxRenderTypes.TARGET_BLOCK_GLOW,
                (pose, vertexConsumer) -> {

                    Matrix4f matrix =
                            pose.pose();

                    for (Map.Entry<BlockPos, TargetBlock> entry
                            : HIGHLIGHTS.entrySet()) {

                        BlockPos pos =
                                entry.getKey();

                        TargetBlock target =
                                entry.getValue();

                        float red =
                                ((target.color() >> 16) & 0xFF)
                                        / 255.0F;

                        float green =
                                ((target.color() >> 8) & 0xFF)
                                        / 255.0F;

                        float blue =
                                (target.color() & 0xFF)
                                        / 255.0F;

                        drawOutline(
                                matrix,
                                vertexConsumer,
                                pos,
                                cameraX,
                                cameraY,
                                cameraZ,
                                red,
                                green,
                                blue,
                                GLOW_INNER_ALPHA,
                                GLOW_INNER_WIDTH
                        );
                    }
                }
        );

        /*
         * Sharp core outline
         */
        collector.submitCustomGeometry(
                poseStack,
                RayxRenderTypes.TARGET_BLOCK_LINES,
                (pose, vertexConsumer) -> {

                    Matrix4f matrix =
                            pose.pose();

                    for (Map.Entry<BlockPos, TargetBlock> entry
                            : HIGHLIGHTS.entrySet()) {

                        BlockPos pos =
                                entry.getKey();

                        TargetBlock target =
                                entry.getValue();

                        float red =
                                ((target.color() >> 16) & 0xFF)
                                        / 255.0F;

                        float green =
                                ((target.color() >> 8) & 0xFF)
                                        / 255.0F;

                        float blue =
                                (target.color() & 0xFF)
                                        / 255.0F;

                        drawOutline(
                                matrix,
                                vertexConsumer,
                                pos,
                                cameraX,
                                cameraY,
                                cameraZ,
                                red,
                                green,
                                blue,
                                LINE_ALPHA,
                                LINE_WIDTH
                        );
                    }
                }
        );
    }

    private static void drawOutline(
            Matrix4f matrix,
            VertexConsumer buffer,
            BlockPos pos,
            double cameraX,
            double cameraY,
            double cameraZ,
            float red,
            float green,
            float blue,
            float alpha,
            float lineWidth
    ) {

        double minX =
                pos.getX()
                        - cameraX
                        - OUTLINE_EPSILON;

        double minY =
                pos.getY()
                        - cameraY
                        - OUTLINE_EPSILON;

        double minZ =
                pos.getZ()
                        - cameraZ
                        - OUTLINE_EPSILON;

        double maxX =
                pos.getX()
                        + 1.0D
                        - cameraX
                        + OUTLINE_EPSILON;

        double maxY =
                pos.getY()
                        + 1.0D
                        - cameraY
                        + OUTLINE_EPSILON;

        double maxZ =
                pos.getZ()
                        + 1.0D
                        - cameraZ
                        + OUTLINE_EPSILON;

        // Bottom

        line(
                matrix,
                buffer,
                minX, minY, minZ,
                maxX, minY, minZ,
                red, green, blue,
                alpha, lineWidth
        );

        line(
                matrix,
                buffer,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                red, green, blue,
                alpha, lineWidth
        );

        line(
                matrix,
                buffer,
                maxX, minY, maxZ,
                minX, minY, maxZ,
                red, green, blue,
                alpha, lineWidth
        );

        line(
                matrix,
                buffer,
                minX, minY, maxZ,
                minX, minY, minZ,
                red, green, blue,
                alpha, lineWidth
        );

        // Top

        line(
                matrix,
                buffer,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                red, green, blue,
                alpha, lineWidth
        );

        line(
                matrix,
                buffer,
                maxX, maxY, minZ,
                maxX, maxY, maxZ,
                red, green, blue,
                alpha, lineWidth
        );

        line(
                matrix,
                buffer,
                maxX, maxY, maxZ,
                minX, maxY, maxZ,
                red, green, blue,
                alpha, lineWidth
        );

        line(
                matrix,
                buffer,
                minX, maxY, maxZ,
                minX, maxY, minZ,
                red, green, blue,
                alpha, lineWidth
        );

        // Vertical edges

        line(
                matrix,
                buffer,
                minX, minY, minZ,
                minX, maxY, minZ,
                red, green, blue,
                alpha, lineWidth
        );

        line(
                matrix,
                buffer,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                red, green, blue,
                alpha, lineWidth
        );

        line(
                matrix,
                buffer,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                red, green, blue,
                alpha, lineWidth
        );

        line(
                matrix,
                buffer,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                red, green, blue,
                alpha, lineWidth
        );
    }

    private static void line(
            Matrix4f matrix,
            VertexConsumer buffer,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            float red,
            float green,
            float blue,
            float alpha,
            float lineWidth
    ) {
        float normalX = (float) (x2 - x1);
        float normalY = (float) (y2 - y1);
        float normalZ = (float) (z2 - z1);

        buffer
                .addVertex(
                        matrix,
                        (float) x1,
                        (float) y1,
                        (float) z1
                )
                .setColor(
                        red,
                        green,
                        blue,
                        alpha
                )
                .setNormal(
                        normalX,
                        normalY,
                        normalZ
                )
                .setLineWidth(
                        lineWidth
                );

        buffer
                .addVertex(
                        matrix,
                        (float) x2,
                        (float) y2,
                        (float) z2
                )
                .setColor(
                        red,
                        green,
                        blue,
                        alpha
                )
                .setNormal(
                        normalX,
                        normalY,
                        normalZ
                )
                .setLineWidth(
                        lineWidth
                );
    }
}