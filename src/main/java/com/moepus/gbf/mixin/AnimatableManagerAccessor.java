package com.moepus.gbf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import software.bernie.geckolib.core.animation.AnimatableManager;

@Mixin(value = AnimatableManager.class, remap = false)
public interface AnimatableManagerAccessor {
    @Invoker(value = "finishFirstTick", remap = false)
    void gbf$finishFirstTick();
}
