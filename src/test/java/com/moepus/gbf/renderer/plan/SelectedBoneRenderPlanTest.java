package com.moepus.gbf.renderer.plan;

import org.junit.jupiter.api.Test;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectedBoneRenderPlanTest {
    @Test
    void disabledPlanRendersEverything() {
        SelectedBoneRenderPlan plan = new SelectedBoneRenderPlan();

        assertEquals(BoneVisit.RENDER, plan.classify(bone(null, "root")));
    }

    @Test
    void selectsTargetPathAndSubtree() {
        GeoBone root = bone(null, "root");
        GeoBone selected = bone(root, "selected");
        GeoBone descendant = bone(selected, "descendant");
        GeoBone sibling = bone(root, "sibling");
        SelectedBoneRenderPlan plan = new SelectedBoneRenderPlan();
        plan.selectOnly(selected);

        assertEquals(BoneVisit.TRANSFORM_ONLY, plan.classify(root));
        assertEquals(BoneVisit.RENDER, plan.classify(selected));
        assertEquals(BoneVisit.RENDER, plan.classify(descendant));
        assertEquals(BoneVisit.SKIP_SUBTREE, plan.classify(sibling));
    }

    @Test
    void combinesMultipleSelectedSubtrees() {
        GeoBone root = bone(null, "root");
        GeoBone left = bone(root, "left");
        GeoBone right = bone(root, "right");
        GeoBone unused = bone(root, "unused");
        SelectedBoneRenderPlan plan = new SelectedBoneRenderPlan();
        plan.include(left);
        plan.include(right);

        assertEquals(BoneVisit.TRANSFORM_ONLY, plan.classify(root));
        assertEquals(BoneVisit.RENDER, plan.classify(left));
        assertEquals(BoneVisit.RENDER, plan.classify(right));
        assertEquals(BoneVisit.SKIP_SUBTREE, plan.classify(unused));
    }

    @Test
    void disableRestoresFullTraversal() {
        GeoBone root = bone(null, "root");
        GeoBone selected = bone(root, "selected");
        SelectedBoneRenderPlan plan = new SelectedBoneRenderPlan();
        plan.selectOnly(selected);
        plan.disable();

        assertEquals(BoneVisit.RENDER, plan.classify(root));
    }

    @Test
    void preservesVisibleGeometryOutsideSelectedSubtree() {
        GeoBone root = bone(null, "root");
        GeoBone selected = child(root, "selected");
        GeoBone hat = child(root, "hat");
        GeoBone hidden = child(root, "hidden");
        hat.getCubes().add(null);
        hidden.setHidden(true);
        SelectedBoneRenderPlan plan = new SelectedBoneRenderPlan(true);
        plan.selectOnly(selected);
        plan.prepare(new BakedGeoModel(List.of(root), null), false);

        assertEquals(BoneVisit.TRANSFORM_ONLY, plan.classify(root));
        assertEquals(BoneVisit.RENDER, plan.classify(selected));
        assertEquals(BoneVisit.RENDER, plan.classify(hat));
        assertEquals(BoneVisit.SKIP_SUBTREE, plan.classify(hidden));
    }

    @Test
    void doesNotRestoreGeometryHiddenByItsParent() {
        GeoBone root = bone(null, "root");
        GeoBone selected = child(root, "selected");
        GeoBone hiddenParent = child(root, "hidden_parent");
        GeoBone decoration = child(hiddenParent, "decoration");
        decoration.getCubes().add(null);
        hiddenParent.setHidden(true);
        SelectedBoneRenderPlan plan = new SelectedBoneRenderPlan(true);
        plan.selectOnly(selected);
        plan.prepare(new BakedGeoModel(List.of(root), null), false);

        assertEquals(BoneVisit.SKIP_SUBTREE, plan.classify(hiddenParent));
        assertEquals(BoneVisit.SKIP_SUBTREE, plan.classify(decoration));
    }

    @Test
    void preservesMatrixTrackingBonesWithoutGeometry() {
        GeoBone root = bone(null, "root");
        GeoBone selected = child(root, "selected");
        GeoBone attachment = child(root, "attachment");
        attachment.setTrackingMatrices(true);
        SelectedBoneRenderPlan plan = new SelectedBoneRenderPlan(true);
        plan.selectOnly(selected);
        plan.prepare(new BakedGeoModel(List.of(root), null), false);

        assertEquals(BoneVisit.RENDER, plan.classify(attachment));
    }

    @Test
    void fallsBackToFullTraversalForCustomLayers() {
        GeoBone root = bone(null, "root");
        GeoBone selected = child(root, "selected");
        GeoBone other = child(root, "other");
        SelectedBoneRenderPlan plan = new SelectedBoneRenderPlan(true);
        plan.selectOnly(selected);
        plan.prepare(new BakedGeoModel(List.of(root), null), true);

        assertEquals(BoneVisit.RENDER, plan.classify(other));
    }

    private static GeoBone child(GeoBone parent, String name) {
        GeoBone child = bone(parent, name);
        parent.getChildBones().add(child);

        return child;
    }

    private static GeoBone bone(GeoBone parent, String name) {
        return new GeoBone(parent, name, false, null, false, false);
    }
}
