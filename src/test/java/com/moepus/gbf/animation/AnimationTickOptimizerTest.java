package com.moepus.gbf.animation;

import org.junit.jupiter.api.Test;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimationTickOptimizerTest {
    private static final double RESET_TIME = 5;

    @Test
    void settledTransformsDoNotWriteBoneValues() {
        CountingBone bone = new CountingBone();
        BoneSnapshot saved = settledSnapshot(bone);

        reset(bone, saved, 10);

        assertEquals(0, bone.totalWrites());
    }

    @Test
    void initialInProgressStateStillStartsReset() {
        CountingBone bone = new CountingBone();
        BoneSnapshot saved = BoneSnapshot.copy(bone.getInitialSnapshot());

        reset(bone, saved, 10);

        assertEquals(9, bone.totalWrites());
    }

    @Test
    void staleCustomTransformIsRestored() {
        CountingBone bone = new CountingBone();
        BoneSnapshot saved = settledSnapshot(bone);
        bone.setRotX(1);
        bone.resetStateChanges();
        bone.clearWrites();

        reset(bone, saved, 10);

        assertEquals(3, bone.rotationWrites);
        assertEquals(0, bone.getRotX());
    }

    @Test
    void activeResetContinuesInterpolating() {
        CountingBone bone = new CountingBone();
        BoneSnapshot saved = BoneSnapshot.copy(bone.getInitialSnapshot());
        saved.updateRotation(1, 1, 1);
        saved.updateOffset(1, 1, 1);
        saved.updateScale(2, 2, 2);
        saved.stopRotAnim(0);
        saved.stopPosAnim(0);
        saved.stopScaleAnim(0);

        reset(bone, saved, 2.5);

        assertEquals(0.5f, bone.getRotX());
        assertEquals(0.5f, bone.getPosX());
        assertEquals(1.5f, bone.getScaleX());
        assertEquals(9, bone.totalWrites());
    }

    @Test
    void completedResetWritesExactInitialValues() {
        CountingBone bone = new CountingBone();
        BoneSnapshot saved = BoneSnapshot.copy(bone.getInitialSnapshot());
        saved.updateRotation(1, 1, 1);
        saved.updateOffset(1, 1, 1);
        saved.updateScale(2, 2, 2);
        saved.stopRotAnim(0);
        saved.stopPosAnim(0);
        saved.stopScaleAnim(0);

        reset(bone, saved, 10);

        assertEquals(0, bone.getRotX());
        assertEquals(0, bone.getPosX());
        assertEquals(1, bone.getScaleX());
        assertEquals(0, saved.getRotX());
        assertEquals(0, saved.getOffsetX());
        assertEquals(1, saved.getScaleX());
    }

    private static BoneSnapshot settledSnapshot(CountingBone bone) {
        BoneSnapshot saved = BoneSnapshot.copy(bone.getInitialSnapshot());
        saved.stopRotAnim(0);
        saved.stopPosAnim(0);
        saved.stopScaleAnim(0);

        return saved;
    }

    private static void reset(CountingBone bone, BoneSnapshot saved, double animTime) {
        AnimationTickOptimizer optimizer = new AnimationTickOptimizer();
        optimizer.configureReset(animTime, RESET_TIME);
        optimizer.resetBones(List.of(bone), Map.of(bone.getName(), saved));
    }

    private static final class CountingBone extends GeoBone {
        private int rotationWrites;
        private int positionWrites;
        private int scaleWrites;

        private CountingBone() {
            super(null, "bone", false, null, false, false);
            saveInitialSnapshot();
        }

        @Override
        public void setRotX(float value) {
            this.rotationWrites++;
            super.setRotX(value);
        }

        @Override
        public void setRotY(float value) {
            this.rotationWrites++;
            super.setRotY(value);
        }

        @Override
        public void setRotZ(float value) {
            this.rotationWrites++;
            super.setRotZ(value);
        }

        @Override
        public void setPosX(float value) {
            this.positionWrites++;
            super.setPosX(value);
        }

        @Override
        public void setPosY(float value) {
            this.positionWrites++;
            super.setPosY(value);
        }

        @Override
        public void setPosZ(float value) {
            this.positionWrites++;
            super.setPosZ(value);
        }

        @Override
        public void setScaleX(float value) {
            this.scaleWrites++;
            super.setScaleX(value);
        }

        @Override
        public void setScaleY(float value) {
            this.scaleWrites++;
            super.setScaleY(value);
        }

        @Override
        public void setScaleZ(float value) {
            this.scaleWrites++;
            super.setScaleZ(value);
        }

        private int totalWrites() {
            return this.rotationWrites + this.positionWrites + this.scaleWrites;
        }

        private void clearWrites() {
            this.rotationWrites = 0;
            this.positionWrites = 0;
            this.scaleWrites = 0;
        }
    }
}
