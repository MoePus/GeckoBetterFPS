package com.moepus.gbf.renderer.plan;

public interface BoneRenderPlanProvider {
    default BoneRenderPlan gbf$getBoneRenderPlan() {
        return BoneRenderPlan.FULL;
    }
}
