package com.moepus.gbf.mixin;

import com.moepus.gbf.animation.AnimationControllerExtension;
import com.moepus.gbf.animation.AnimationQueueManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.EasingType;
import software.bernie.geckolib.core.keyframe.BoneAnimationQueue;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

@Mixin(value = AnimationController.class, remap = false)
public abstract class AnimationControllerMixin<T extends GeoAnimatable> implements AnimationControllerExtension {
    @Shadow @Final protected Map<String, BoneAnimationQueue> boneAnimationQueues;
    @Shadow @Final protected T animatable;
    @Shadow protected boolean isJustStarting;
    @Shadow protected Function<T, EasingType> overrideEasingTypeFunction;

    @Unique
    private final AnimationQueueManager gbf$queueManager = new AnimationQueueManager();

    /**
     * @author MoePus
     * @reason Reuse the queue layout instead of rebuilding it every animation frame.
     */
    @Overwrite(remap = false)
    private void createInitialQueues(Collection<CoreGeoBone> bones) {
        this.gbf$queueManager.prepare(this.boneAnimationQueues, bones);
    }

    @Redirect(method = {"process", "processCurrentAnimation"}, at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;",
            ordinal = 0
    ), slice = @Slice(from = @At(
            value = "FIELD",
            target = "Lsoftware/bernie/geckolib/core/animation/AnimationController;boneAnimationQueues:Ljava/util/Map;",
            opcode = Opcodes.GETFIELD
    )), remap = false)
    private Object gbf$trackActiveQueue(Map<?, ?> map, Object key) {
        Object value = map.get(key);

        if (value instanceof BoneAnimationQueue queue) {
            this.gbf$queueManager.activate(queue);
        }

        return value;
    }

    @Override
    public void gbf$beginFrame() {
        this.gbf$queueManager.beginFrame();
    }

    @Override
    public ObjectArrayList<BoneAnimationQueue> gbf$getActiveQueues() {
        return this.gbf$queueManager.activeQueues();
    }

    @Override
    public void gbf$setJustStarting(boolean justStarting) {
        this.isJustStarting = justStarting;
    }

    @Override
    public EasingType gbf$getEasingType() {
        return this.overrideEasingTypeFunction.apply(this.animatable);
    }
}
