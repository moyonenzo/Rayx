package fr.nzlz.rayx;

import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Optional;

public final class RayxRenderTypes {

    public static final RenderType TARGET_BLOCK_LINES;

    static {
        RenderPipeline pipeline = RenderPipeline.builder(
                        RenderPipelines.LINES_SNIPPET
                )
                .withLocation("pipeline/rayx_target_block_lines")
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withDepthStencilState(Optional.empty())
                .build();

        TARGET_BLOCK_LINES = RenderType.create(
                "rayx_target_block_lines",
                RenderSetup.builder(pipeline)
                        .setLayeringTransform(
                                LayeringTransform.VIEW_OFFSET_Z_LAYERING
                        )
                        .createRenderSetup()
        );
    }

    private RayxRenderTypes() {
    }
}