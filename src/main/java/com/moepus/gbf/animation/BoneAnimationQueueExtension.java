package com.moepus.gbf.animation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import software.bernie.geckolib.core.keyframe.BoneAnimationQueue;

public interface BoneAnimationQueueExtension {
    void gbf$reset(ObjectArrayList<BoneAnimationQueue> activeQueues);

    void gbf$activate();
}
