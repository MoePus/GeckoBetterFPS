package com.moepus.gbf.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.loading.json.raw.Cube;
import software.bernie.geckolib.loading.json.raw.ModelProperties;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.util.RenderUtils;

@Mixin(value = BakedModelFactory.Builtin.class, remap = false)
public abstract class BakedModelFactoryBuiltinMixin implements BakedModelFactory {
    /**
     * @author MoePus
     * @reason Precompute rotations
     */
    @Overwrite(remap = false)
    public GeoCube constructCube(Cube cube, ModelProperties properties, GeoBone bone) {
        boolean mirror = cube.mirror() == Boolean.TRUE;
        double inflate = cube.inflate() != null ? cube.inflate() / 16f : (bone.getInflate() == null ? 0 : bone.getInflate() / 16f);
        Vec3 size = RenderUtils.arrayToVec(cube.size());
        Vec3 origin = RenderUtils.arrayToVec(cube.origin());
        Vec3 rotation = RenderUtils.arrayToVec(cube.rotation());
        Vec3 pivot = RenderUtils.arrayToVec(cube.pivot());
        origin = new Vec3(-(origin.x + size.x) / 16d, origin.y / 16d, origin.z / 16d);
        Vec3 vertexSize = size.multiply(1 / 16d, 1 / 16d, 1 / 16d);

        pivot = pivot.multiply(-1, 1, 1);
        GeoQuad[] quads = buildQuads(cube.uv(), new BakedModelFactory.VertexSet(origin, vertexSize, inflate), cube, (float) properties.textureWidth(), (float) properties.textureHeight(), mirror);

        if (rotation.distanceToSqr(Vec3.ZERO) > 1e-5) {
            rotation = new Vec3(Math.toRadians(-rotation.x), Math.toRadians(-rotation.y), Math.toRadians(rotation.z));
        } else {
            rotation = Vec3.ZERO;
        }

        return new GeoCube(quads, pivot, rotation, size, inflate, mirror);
    }
}
