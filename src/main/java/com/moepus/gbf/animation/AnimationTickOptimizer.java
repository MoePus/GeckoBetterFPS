package com.moepus.gbf.animation;

import com.eliotlash.mclib.utils.Interpolations;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.EasingType;
import software.bernie.geckolib.core.keyframe.AnimationPoint;
import software.bernie.geckolib.core.keyframe.BoneAnimationQueue;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.Collection;
import java.util.Map;

public final class AnimationTickOptimizer {
    private double animTime;
    private double resetTickLength;
    private double inverseResetTickLength;

    public Map<String, BoneSnapshot> updateBoneSnapshots(Collection<CoreGeoBone> bones,
                                                         Map<String, BoneSnapshot> snapshots) {
        for (CoreGeoBone bone : bones) {
            if (!snapshots.containsKey(bone.getName())) {
                snapshots.put(bone.getName(), BoneSnapshot.copy(bone.getInitialSnapshot()));
            }
        }

        return snapshots;
    }

    public void applyActiveQueues(AnimationController<?> controller, Map<String, BoneSnapshot> snapshots) {
        AnimationControllerExtension extension = (AnimationControllerExtension)controller;
        ObjectArrayList<BoneAnimationQueue> queues = extension.gbf$getActiveQueues();

        for (int i = 0, size = queues.size(); i < size; i++) {
            BoneAnimationQueue queue = queues.get(i);
            applyQueue(queue, snapshots.get(queue.bone().getName()), extension.gbf$getEasingType());
        }
    }

    public void configureReset(double animTime, double resetTickLength) {
        this.animTime = animTime;
        this.resetTickLength = resetTickLength;
        this.inverseResetTickLength = resetTickLength == 0 ? 0 : 1 / resetTickLength;
    }

    public void resetBones(Collection<CoreGeoBone> bones, Map<String, BoneSnapshot> snapshots) {
        for (CoreGeoBone bone : bones) {
            boolean resetRotation = !bone.hasRotationChanged();
            boolean resetPosition = !bone.hasPositionChanged();
            boolean resetScale = !bone.hasScaleChanged();

            if (resetRotation || resetPosition || resetScale) {
                BoneSnapshot saved = snapshots.get(bone.getName());
                BoneSnapshot initial = bone.getInitialSnapshot();

                if (resetRotation) {
                    resetRotation(bone, initial, saved);
                }
                if (resetPosition) {
                    resetPosition(bone, initial, saved);
                }
                if (resetScale) {
                    resetScale(bone, initial, saved);
                }
            }

            bone.resetStateChanges();
        }
    }

    private void applyQueue(BoneAnimationQueue queue, BoneSnapshot snapshot, EasingType easingType) {
        CoreGeoBone bone = queue.bone();
        BoneSnapshot initial = bone.getInitialSnapshot();
        AnimationPoint rotX = queue.rotationXQueue().poll();
        AnimationPoint rotY = queue.rotationYQueue().poll();
        AnimationPoint rotZ = queue.rotationZQueue().poll();
        AnimationPoint posX = queue.positionXQueue().poll();
        AnimationPoint posY = queue.positionYQueue().poll();
        AnimationPoint posZ = queue.positionZQueue().poll();
        AnimationPoint scaleX = queue.scaleXQueue().poll();
        AnimationPoint scaleY = queue.scaleYQueue().poll();
        AnimationPoint scaleZ = queue.scaleZQueue().poll();

        if (rotX != null && rotY != null && rotZ != null) {
            bone.setRotX((float)EasingType.lerpWithOverride(rotX, easingType) + initial.getRotX());
            bone.setRotY((float)EasingType.lerpWithOverride(rotY, easingType) + initial.getRotY());
            bone.setRotZ((float)EasingType.lerpWithOverride(rotZ, easingType) + initial.getRotZ());
            snapshot.updateRotation(bone.getRotX(), bone.getRotY(), bone.getRotZ());
            snapshot.startRotAnim();
            bone.markRotationAsChanged();
        }
        if (posX != null && posY != null && posZ != null) {
            bone.setPosX((float)EasingType.lerpWithOverride(posX, easingType));
            bone.setPosY((float)EasingType.lerpWithOverride(posY, easingType));
            bone.setPosZ((float)EasingType.lerpWithOverride(posZ, easingType));
            snapshot.updateOffset(bone.getPosX(), bone.getPosY(), bone.getPosZ());
            snapshot.startPosAnim();
            bone.markPositionAsChanged();
        }
        if (scaleX != null && scaleY != null && scaleZ != null) {
            bone.setScaleX((float)EasingType.lerpWithOverride(scaleX, easingType));
            bone.setScaleY((float)EasingType.lerpWithOverride(scaleY, easingType));
            bone.setScaleZ((float)EasingType.lerpWithOverride(scaleZ, easingType));
            snapshot.updateScale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
            snapshot.startScaleAnim();
            bone.markScaleAsChanged();
        }
    }

    private void resetRotation(CoreGeoBone bone, BoneSnapshot initial, BoneSnapshot saved) {
        if (isRotationSettled(bone, initial, saved)) {
            return;
        }
        if (saved.isRotAnimInProgress()) {
            saved.stopRotAnim(this.animTime);
        }

        double percentage = resetPercentage(saved.getLastResetRotationTick());
        float initialX = initial.getRotX();
        float initialY = initial.getRotY();
        float initialZ = initial.getRotZ();
        float lastX = saved.getRotX();
        float lastY = saved.getRotY();
        float lastZ = saved.getRotZ();

        if (percentage == 0) {
            if (lastX != initialX && isSuspectedCompletedRotation(lastX)) {
                lastX = initialX;
                percentage = 1;
            }
            if (lastY != initialY && isSuspectedCompletedRotation(lastY)) {
                lastY = initialY;
                percentage = 1;
            }
            if (lastZ != initialZ && isSuspectedCompletedRotation(lastZ)) {
                lastZ = initialZ;
                percentage = 1;
            }
        }

        if (percentage >= 1) {
            bone.setRotX(initialX);
            bone.setRotY(initialY);
            bone.setRotZ(initialZ);
            saved.updateRotation(initialX, initialY, initialZ);
            return;
        }

        bone.setRotX((float)Interpolations.lerp(lastX, initialX, percentage));
        bone.setRotY((float)Interpolations.lerp(lastY, initialY, percentage));
        bone.setRotZ((float)Interpolations.lerp(lastZ, initialZ, percentage));
    }

    private void resetPosition(CoreGeoBone bone, BoneSnapshot initial, BoneSnapshot saved) {
        if (isPositionSettled(bone, initial, saved)) {
            return;
        }
        if (saved.isPosAnimInProgress()) {
            saved.stopPosAnim(this.animTime);
        }

        double percentage = resetPercentage(saved.getLastResetPositionTick());
        float initialX = initial.getOffsetX();
        float initialY = initial.getOffsetY();
        float initialZ = initial.getOffsetZ();

        if (percentage >= 1) {
            bone.setPosX(initialX);
            bone.setPosY(initialY);
            bone.setPosZ(initialZ);
            saved.updateOffset(initialX, initialY, initialZ);
            return;
        }

        bone.setPosX((float)Interpolations.lerp(saved.getOffsetX(), initialX, percentage));
        bone.setPosY((float)Interpolations.lerp(saved.getOffsetY(), initialY, percentage));
        bone.setPosZ((float)Interpolations.lerp(saved.getOffsetZ(), initialZ, percentage));
    }

    private void resetScale(CoreGeoBone bone, BoneSnapshot initial, BoneSnapshot saved) {
        if (isScaleSettled(bone, initial, saved)) {
            return;
        }
        if (saved.isScaleAnimInProgress()) {
            saved.stopScaleAnim(this.animTime);
        }

        double percentage = resetPercentage(saved.getLastResetScaleTick());
        float initialX = initial.getScaleX();
        float initialY = initial.getScaleY();
        float initialZ = initial.getScaleZ();

        if (percentage >= 1) {
            bone.setScaleX(initialX);
            bone.setScaleY(initialY);
            bone.setScaleZ(initialZ);
            saved.updateScale(initialX, initialY, initialZ);
            return;
        }

        bone.setScaleX((float)Interpolations.lerp(saved.getScaleX(), initialX, percentage));
        bone.setScaleY((float)Interpolations.lerp(saved.getScaleY(), initialY, percentage));
        bone.setScaleZ((float)Interpolations.lerp(saved.getScaleZ(), initialZ, percentage));
    }

    private double resetPercentage(double lastResetTick) {
        return this.resetTickLength == 0
                ? 1
                : Math.min((this.animTime - lastResetTick) * this.inverseResetTickLength, 1);
    }

    private static boolean isRotationSettled(CoreGeoBone bone, BoneSnapshot initial, BoneSnapshot saved) {
        return !saved.isRotAnimInProgress()
                && sameFloat(saved.getRotX(), initial.getRotX())
                && sameFloat(saved.getRotY(), initial.getRotY())
                && sameFloat(saved.getRotZ(), initial.getRotZ())
                && sameFloat(bone.getRotX(), initial.getRotX())
                && sameFloat(bone.getRotY(), initial.getRotY())
                && sameFloat(bone.getRotZ(), initial.getRotZ());
    }

    private static boolean isPositionSettled(CoreGeoBone bone, BoneSnapshot initial, BoneSnapshot saved) {
        return !saved.isPosAnimInProgress()
                && sameFloat(saved.getOffsetX(), initial.getOffsetX())
                && sameFloat(saved.getOffsetY(), initial.getOffsetY())
                && sameFloat(saved.getOffsetZ(), initial.getOffsetZ())
                && sameFloat(bone.getPosX(), initial.getOffsetX())
                && sameFloat(bone.getPosY(), initial.getOffsetY())
                && sameFloat(bone.getPosZ(), initial.getOffsetZ());
    }

    private static boolean isScaleSettled(CoreGeoBone bone, BoneSnapshot initial, BoneSnapshot saved) {
        return !saved.isScaleAnimInProgress()
                && sameFloat(saved.getScaleX(), initial.getScaleX())
                && sameFloat(saved.getScaleY(), initial.getScaleY())
                && sameFloat(saved.getScaleZ(), initial.getScaleZ())
                && sameFloat(bone.getScaleX(), initial.getScaleX())
                && sameFloat(bone.getScaleY(), initial.getScaleY())
                && sameFloat(bone.getScaleZ(), initial.getScaleZ());
    }

    private static boolean sameFloat(float left, float right) {
        return Float.floatToRawIntBits(left) == Float.floatToRawIntBits(right);
    }

    private static boolean isSuspectedCompletedRotation(float lastRotation) {
        float rotations = Math.abs(lastRotation / (360f * ((float)Math.PI / 180f)));
        float partialRotation = 1 - (rotations - (int)rotations);

        return partialRotation == 1 || partialRotation < 0.026 * rotations;
    }
}
