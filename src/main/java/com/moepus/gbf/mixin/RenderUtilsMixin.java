package com.moepus.gbf.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.util.RenderUtils;

@Mixin(value = RenderUtils.class, remap = false)
public abstract class RenderUtilsMixin {
    /**
     * @author MoePus
     * @reason Faster Rendering
     */
    @Overwrite(remap = false)
    public static void rotateMatrixAroundBone(PoseStack poseStack, CoreGeoBone bone) {
        if (bone.getRotZ() != 0 || bone.getRotY() != 0 || bone.getRotX() != 0) {
            poseStack.mulPose(new Quaternionf().rotationZYX(bone.getRotZ(), bone.getRotY(), bone.getRotX()));
        }
    }

    /**
     * @author MoePus
     * @reason Faster Rendering
     */
    @Overwrite(remap = false)
    public static Matrix4f translateMatrix(Matrix4f matrix, Vector3f vector) {
        return matrix.m30(matrix.m30() + vector.x).m31(matrix.m31() + vector.y).m32(matrix.m32() + vector.z);
    }
}
