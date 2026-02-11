package com.moepus.gbf.mixin;

import com.moepus.gbf.renderer.IrisCubeRenderer;
import com.moepus.gbf.renderer.SodiumCubeRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtils;

@Mixin(value = GeoRenderer.class, remap = false)
public interface GeoRendererMixin {
    @Unique
    default void gbf$createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        Vector3f transformed = new Vector3f();
        for (GeoVertex vertex : quad.vertices()) {
            vertex.position().mulPosition(poseState, transformed);
            buffer.vertex(transformed.x(), transformed.y(), transformed.z(), red, green, blue, alpha, vertex.texU(), vertex.texV(), packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
        }
    }

    /**
     * @author MoePus
     * @reason Faster Rendering
     */
    @Overwrite(remap = false)
    default void renderCube(PoseStack poseStack, GeoCube cube, VertexConsumer buffer, int packedLight,
                            int packedOverlay, float red, float green, float blue, float alpha) {
        Vec3 rotation = cube.rotation();
        if (!rotation.equals(Vec3.ZERO)) {
            Vec3 pivot = cube.pivot();
            poseStack.rotateAround(
                    (new Quaternionf()).rotationZYX((float) rotation.z(), (float) rotation.y(), (float) rotation.x()),
                    (float) (pivot.x() / 16.0F), (float) (pivot.y() / 16.0F), (float) (pivot.z() / 16.0F));
        }

        VertexBufferWriter writer = VertexBufferWriter.tryOf(buffer);
        if (writer != null) {
            if (buffer instanceof BufferBuilder bb) {
                if (bb.format == DefaultVertexFormat.NEW_ENTITY) {
                    SodiumCubeRenderer.renderCube(poseStack, cube, writer, packedLight, packedOverlay, red, green, blue, alpha);
                } else {
                    IrisCubeRenderer.renderCube(poseStack, cube, writer, packedLight, packedOverlay, red, green, blue, alpha);
                }
                return;
            } else if (buffer instanceof SodiumBufferBuilder sbb) {
                if (sbb.getOriginalBufferBuilder().format == DefaultVertexFormat.NEW_ENTITY) {
                    SodiumCubeRenderer.renderCube(poseStack, cube, writer, packedLight, packedOverlay, red, green, blue, alpha);
                    return;
                }
            }
        }

        Matrix3f normalisedPoseState = poseStack.last().normal();
        Matrix4f poseState = poseStack.last().pose();
        Vector3f normal = new Vector3f();
        for (GeoQuad quad : cube.quads()) {
            if (quad == null)
                continue;

            quad.normal().mul(normalisedPoseState, normal);

            RenderUtils.fixInvertedFlatCube(cube, normal);
            gbf$createVerticesOfQuad(quad, poseState, normal, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }
}
