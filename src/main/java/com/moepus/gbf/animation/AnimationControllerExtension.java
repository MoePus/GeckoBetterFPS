package com.moepus.gbf.animation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import software.bernie.geckolib.core.animation.EasingType;
import software.bernie.geckolib.core.keyframe.BoneAnimationQueue;

public interface AnimationControllerExtension {
    void gbf$beginFrame();

    ObjectArrayList<BoneAnimationQueue> gbf$getActiveQueues();

    void gbf$setJustStarting(boolean justStarting);

    EasingType gbf$getEasingType();
}
