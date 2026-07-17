package com.moepus.gbf.animation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.keyframe.BoneAnimationQueue;

import java.util.Collection;
import java.util.Map;

public final class AnimationQueueManager {
    private final ObjectArrayList<BoneAnimationQueue> activeQueues = new ObjectArrayList<>();

    public void beginFrame() {
        this.activeQueues.clear();
    }

    public void prepare(Map<String, BoneAnimationQueue> queues, Collection<CoreGeoBone> bones) {
        beginFrame();

        if (!hasSameBoneLayout(queues, bones)) {
            rebuild(queues, bones);
            return;
        }

        for (CoreGeoBone bone : bones) {
            reset(queues.get(bone.getName()));
        }
    }

    public ObjectArrayList<BoneAnimationQueue> activeQueues() {
        return this.activeQueues;
    }

    public void activate(BoneAnimationQueue queue) {
        ((BoneAnimationQueueExtension)(Object)queue).gbf$activate();
    }

    static boolean hasSameBoneLayout(Map<String, BoneAnimationQueue> queues, Collection<CoreGeoBone> bones) {
        if (queues.size() != bones.size()) {
            return false;
        }

        for (CoreGeoBone bone : bones) {
            BoneAnimationQueue queue = queues.get(bone.getName());

            if (queue == null || queue.bone() != bone) {
                return false;
            }
        }

        return true;
    }

    private void rebuild(Map<String, BoneAnimationQueue> queues, Collection<CoreGeoBone> bones) {
        queues.clear();

        for (CoreGeoBone bone : bones) {
            BoneAnimationQueue queue = new BoneAnimationQueue(bone);
            reset(queue);
            queues.put(bone.getName(), queue);
        }
    }

    private void reset(BoneAnimationQueue queue) {
        ((BoneAnimationQueueExtension)(Object)queue).gbf$reset(this.activeQueues);
    }
}
