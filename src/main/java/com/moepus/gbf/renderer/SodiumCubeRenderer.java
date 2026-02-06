package com.moepus.gbf.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtils;

public class SodiumCubeRenderer {
    private static final long SCRATCH_BUFFER = MemoryUtil.nmemAlignedAlloc(64, 4 * EntityVertex.STRIDE);

    static private void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexBufferWriter writer, int packedLight, int packedOverlay, int color) {
        if (quad.vertices().length != 4)
            return;

        Vector3f transformed = new Vector3f();
        long buffer = SCRATCH_BUFFER;
        long ptr = buffer;
        int inormal = NormI8.pack(normal);

        for (GeoVertex vertex : quad.vertices()) {
            vertex.position().mulPosition(poseState, transformed);
            EntityVertex.write(ptr, transformed.x(), transformed.y(), transformed.z(), color,
                    vertex.texU(), vertex.texV(),
                    packedOverlay, packedLight, inormal);
            ptr += EntityVertex.STRIDE;
        }
        writer.push(null, buffer, quad.vertices().length, EntityVertex.FORMAT);
    }

    static public void renderCube(PoseStack poseStack, GeoCube cube, VertexBufferWriter writer, int packedLight,
                                  int packedOverlay, float red, float green, float blue, float alpha) {
        Matrix3f normalisedPoseState = poseStack.last().normal();
        Matrix4f poseState = poseStack.last().pose();
        Vector3f normal = new Vector3f();
        int color = ColorABGR.pack((int) (red * 255), (int) (green * 255), (int) (blue * 255), (int) (alpha * 255));
        for (GeoQuad quad : cube.quads()) {
            if (quad == null)
                continue;

            quad.normal().mul(normalisedPoseState, normal);

            RenderUtils.fixInvertedFlatCube(cube, normal);
            createVerticesOfQuad(quad, poseState, normal, writer, packedLight, packedOverlay, color);
        }
    }
}
