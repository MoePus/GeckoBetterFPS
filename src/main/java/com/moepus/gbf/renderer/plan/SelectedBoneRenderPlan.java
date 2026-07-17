package com.moepus.gbf.renderer.plan;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.List;

public final class SelectedBoneRenderPlan implements BoneRenderPlan {
    private final ObjectArrayList<GeoBone> selectedRoots = new ObjectArrayList<>(3);
    private final ObjectArrayList<GeoBone> preservedRoots = new ObjectArrayList<>(3);
    private final boolean preserveVisibleGeometry;
    private boolean enabled;
    private boolean fallbackToFull;

    public SelectedBoneRenderPlan() {
        this(false);
    }

    public SelectedBoneRenderPlan(boolean preserveVisibleGeometry) {
        this.preserveVisibleGeometry = preserveVisibleGeometry;
    }

    public void disable() {
        this.selectedRoots.clear();
        this.preservedRoots.clear();
        this.enabled = false;
        this.fallbackToFull = false;
    }

    public void selectOnly(GeoBone bone) {
        disable();
        include(bone);
    }

    public void include(GeoBone bone) {
        if (bone == null || containsIdentity(bone)) {
            return;
        }

        this.selectedRoots.add(bone);
        this.enabled = true;
    }

    @Override
    public void prepare(BakedGeoModel model, boolean hasPerBoneLayers) {
        this.preservedRoots.clear();
        this.fallbackToFull = false;

        if (!this.enabled || !this.preserveVisibleGeometry) {
            return;
        }
        if (hasPerBoneLayers) {
            this.fallbackToFull = true;
            return;
        }

        List<GeoBone> topLevelBones = model.topLevelBones();
        for (int i = 0, size = topLevelBones.size(); i < size; i++) {
            collectVisibleGeometry(topLevelBones.get(i));
        }
    }

    @Override
    public BoneVisit classify(GeoBone bone) {
        if (!this.enabled || this.fallbackToFull) {
            return BoneVisit.RENDER;
        }

        BoneVisit selectedVisit = classifyAgainst(this.selectedRoots, bone);

        if (selectedVisit == BoneVisit.RENDER) {
            return BoneVisit.RENDER;
        }

        BoneVisit preservedVisit = classifyAgainst(this.preservedRoots, bone);
        return preservedVisit.ordinal() > selectedVisit.ordinal() ? preservedVisit : selectedVisit;
    }

    private BoneVisit classifyAgainst(ObjectArrayList<GeoBone> roots, GeoBone bone) {
        BoneVisit result = BoneVisit.SKIP_SUBTREE;

        for (int i = 0, size = roots.size(); i < size; i++) {
            GeoBone selectedRoot = roots.get(i);

            if (isDescendantOrSelf(bone, selectedRoot)) {
                return BoneVisit.RENDER;
            }
            if (isDescendantOrSelf(selectedRoot, bone)) {
                result = BoneVisit.TRANSFORM_ONLY;
            }
        }

        return result;
    }

    private void collectVisibleGeometry(GeoBone bone) {
        if (classifyAgainst(this.selectedRoots, bone) == BoneVisit.RENDER) {
            return;
        }
        if (bone.isTrackingMatrices() || (!bone.isHidden() && !bone.getCubes().isEmpty())) {
            this.preservedRoots.add(bone);
            return;
        }
        if (bone.isHidingChildren()) {
            return;
        }

        List<GeoBone> children = bone.getChildBones();
        for (int i = 0, size = children.size(); i < size; i++) {
            collectVisibleGeometry(children.get(i));
        }
    }

    private boolean containsIdentity(GeoBone bone) {
        for (int i = 0, size = this.selectedRoots.size(); i < size; i++) {
            if (this.selectedRoots.get(i) == bone) {
                return true;
            }
        }

        return false;
    }

    private static boolean isDescendantOrSelf(GeoBone bone, GeoBone ancestor) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (current == ancestor) {
                return true;
            }
        }

        return false;
    }
}
