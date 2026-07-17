package com.moepus.gbf.renderer.plan;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

@FunctionalInterface
public interface BoneRenderPlan {
    BoneRenderPlan FULL = bone -> BoneVisit.RENDER;

    default void prepare(BakedGeoModel model, boolean hasPerBoneLayers) {}

    BoneVisit classify(GeoBone bone);
}
