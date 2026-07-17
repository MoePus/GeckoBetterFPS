package com.moepus.gbf.mixin;

import com.moepus.gbf.renderer.plan.BoneRenderPlan;
import com.moepus.gbf.renderer.plan.BoneRenderPlanProvider;
import com.moepus.gbf.renderer.plan.SelectedBoneRenderPlan;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

@Mixin(value = GeoArmorRenderer.class, remap = false)
public abstract class GeoArmorRendererMixin<T extends Item & GeoItem> implements BoneRenderPlanProvider {
    @Shadow protected GeoBone head;
    @Shadow protected GeoBone body;
    @Shadow protected GeoBone rightArm;
    @Shadow protected GeoBone leftArm;
    @Shadow protected GeoBone rightLeg;
    @Shadow protected GeoBone leftLeg;
    @Shadow protected GeoBone rightBoot;
    @Shadow protected GeoBone leftBoot;

    @Shadow protected abstract void setAllBonesVisible(boolean visible);
    @Shadow protected abstract void setBoneVisible(GeoBone bone, boolean visible);

    @Unique
    private final SelectedBoneRenderPlan gbf$boneRenderPlan = new SelectedBoneRenderPlan(true);

    @Override
    public BoneRenderPlan gbf$getBoneRenderPlan() {
        return this.gbf$boneRenderPlan;
    }

    /**
     * @author MoePus
     * @reason Select only the armor subtrees visible for the current equipment slot.
     */
    @Overwrite(remap = false)
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        HumanoidModel<?> model = (HumanoidModel<?>)(Object)this;
        this.gbf$boneRenderPlan.disable();
        setAllBonesVisible(false);

        switch (currentSlot) {
            case HEAD -> gbf$selectVisible(this.head, model.head.visible);
            case CHEST -> {
                gbf$selectVisible(this.body, model.body.visible);
                gbf$selectVisible(this.rightArm, model.rightArm.visible);
                gbf$selectVisible(this.leftArm, model.leftArm.visible);
            }
            case LEGS -> {
                gbf$selectVisible(this.rightLeg, model.rightLeg.visible);
                gbf$selectVisible(this.leftLeg, model.leftLeg.visible);
            }
            case FEET -> {
                gbf$selectVisible(this.rightBoot, model.rightLeg.visible);
                gbf$selectVisible(this.leftBoot, model.leftLeg.visible);
            }
        }
    }

    /**
     * @author MoePus
     * @reason Select only the armor subtree corresponding to the current entity bone.
     */
    @Overwrite(remap = false)
    public void applyBoneVisibilityByPart(EquipmentSlot currentSlot, ModelPart currentPart, HumanoidModel<?> model) {
        ((HumanoidModel<?>)(Object)this).setAllVisible(false);
        currentPart.visible = true;
        GeoBone bone = gbf$findBone(currentSlot, currentPart, model);

        if (bone == null) {
            this.gbf$boneRenderPlan.disable();
            return;
        }

        bone.setHidden(false);
        this.gbf$boneRenderPlan.selectOnly(bone);
    }

    /**
     * @author MoePus
     * @reason Do not retain a subtree selection across armor render calls.
     */
    @Overwrite(remap = false)
    public void doPostRenderCleanup() {
        this.gbf$boneRenderPlan.disable();
    }

    @Unique
    private void gbf$selectVisible(GeoBone bone, boolean visible) {
        setBoneVisible(bone, visible);

        if (visible) {
            this.gbf$boneRenderPlan.include(bone);
        }
    }

    @Unique
    private GeoBone gbf$findBone(EquipmentSlot slot, ModelPart part, HumanoidModel<?> model) {
        if (part == model.hat || part == model.head) {
            return this.head;
        }
        if (part == model.body) {
            return this.body;
        }
        if (part == model.leftArm) {
            return this.leftArm;
        }
        if (part == model.rightArm) {
            return this.rightArm;
        }
        if (part == model.leftLeg) {
            return slot == EquipmentSlot.FEET ? this.leftBoot : this.leftLeg;
        }
        if (part == model.rightLeg) {
            return slot == EquipmentSlot.FEET ? this.rightBoot : this.rightLeg;
        }

        return null;
    }
}
