package com.moepus.gbf.mixin;

import com.moepus.gbf.renderer.plan.BoneRenderPlan;
import com.moepus.gbf.renderer.plan.BoneRenderPlanProvider;
import com.moepus.gbf.renderer.plan.BoneVisit;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.List;

@Mixin(value = GeoRenderer.class, remap = false)
@SuppressWarnings("unchecked")
public interface GeoRendererBonePlanMixin<T extends GeoAnimatable> extends BoneRenderPlanProvider {
    /**
     * @author MoePus
     * @reason Skip top-level bones excluded by the active render plan.
     */
    @Overwrite(remap = false)
    default void actuallyRender(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType,
                                MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                float partialTick, int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha) {
        GeoRenderer<T> renderer = (GeoRenderer<T>)(Object)this;
        BoneRenderPlan plan = gbf$getBoneRenderPlan();
        List<GeoBone> bones = model.topLevelBones();
        plan.prepare(model, !renderer.getRenderLayers().isEmpty());
        renderer.updateAnimatedTextureFrame(animatable);

        for (int i = 0, size = bones.size(); i < size; i++) {
            GeoBone bone = bones.get(i);

            if (plan.classify(bone) != BoneVisit.SKIP_SUBTREE) {
                renderer.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                        partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            }
        }
    }

    /**
     * @author MoePus
     * @reason Apply the active render plan before allocating PoseStack entries or rendering layers.
     */
    @Overwrite(remap = false)
    default void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                                   MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                   float partialTick, int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha) {
        GeoRenderer<T> renderer = (GeoRenderer<T>)(Object)this;
        BoneVisit visit = gbf$getBoneRenderPlan().classify(bone);

        if (visit == BoneVisit.SKIP_SUBTREE) {
            return;
        }

        poseStack.pushPose();
        RenderUtils.prepMatrixForBone(poseStack, bone);

        if (visit == BoneVisit.RENDER) {
            renderer.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);

            if (!isReRender) {
                renderer.applyRenderLayersForBone(poseStack, animatable, bone, renderType, bufferSource, buffer,
                        partialTick, packedLight, packedOverlay);
            }
        }

        renderer.renderChildBones(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }

    /**
     * @author MoePus
     * @reason Skip excluded child branches before dispatching renderer-specific recursive hooks.
     */
    @Overwrite(remap = false)
    default void renderChildBones(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        GeoRenderer<T> renderer = (GeoRenderer<T>)(Object)this;
        BoneRenderPlan plan = gbf$getBoneRenderPlan();

        if (bone.isHidingChildren()) {
            return;
        }

        List<GeoBone> children = bone.getChildBones();
        for (int i = 0, size = children.size(); i < size; i++) {
            GeoBone child = children.get(i);

            if (plan.classify(child) != BoneVisit.SKIP_SUBTREE) {
                renderer.renderRecursively(poseStack, animatable, child, renderType, bufferSource, buffer, isReRender,
                        partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            }
        }
    }
}
