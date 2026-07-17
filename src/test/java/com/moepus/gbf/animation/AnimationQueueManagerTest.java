package com.moepus.gbf.animation;

import org.junit.jupiter.api.Test;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.keyframe.BoneAnimationQueue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationQueueManagerTest {
    @Test
    void acceptsSameBoneInstances() {
        CoreGeoBone head = bone("head");
        CoreGeoBone body = bone("body");
        Map<String, BoneAnimationQueue> queues = Map.of(
                "head", new BoneAnimationQueue(head),
                "body", new BoneAnimationQueue(body));

        assertTrue(AnimationQueueManager.hasSameBoneLayout(queues, List.of(head, body)));
    }

    @Test
    void rejectsDifferentBoneWithSameName() {
        CoreGeoBone oldBone = bone("head");
        CoreGeoBone newBone = bone("head");
        Map<String, BoneAnimationQueue> queues = Map.of("head", new BoneAnimationQueue(oldBone));

        assertFalse(AnimationQueueManager.hasSameBoneLayout(queues, List.of(newBone)));
    }

    @Test
    void rejectsDifferentMapSize() {
        CoreGeoBone head = bone("head");
        Map<String, BoneAnimationQueue> queues = Map.of("head", new BoneAnimationQueue(head));

        assertFalse(AnimationQueueManager.hasSameBoneLayout(queues, List.of(head, bone("body"))));
    }

    private static CoreGeoBone bone(String name) {
        return (CoreGeoBone)Proxy.newProxyInstance(
                CoreGeoBone.class.getClassLoader(),
                new Class<?>[]{CoreGeoBone.class},
                (proxy, method, args) -> method.getName().equals("getName") ? name : defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == float.class) {
            return 0f;
        }

        return null;
    }
}
