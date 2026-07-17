package com.moepus.gbf.mixin;

import com.eliotlash.mclib.math.IValue;
import com.moepus.gbf.molang.compiler.MolangCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import software.bernie.geckolib.core.molang.MolangParser;

@Mixin(value = MolangParser.class, remap = false)
public abstract class MolangParserMixin {
    @ModifyArg(
            method = "parseOneLine",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/core/molang/expressions/MolangValue;<init>(Lcom/eliotlash/mclib/math/IValue;Z)V",
                    remap = false),
            index = 0,
            remap = false)
    private static IValue gbf$compileReturn(IValue value) {
        return MolangCompiler.compile(value);
    }

    @ModifyArg(
            method = "parseOneLine",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/core/molang/expressions/MolangVariableHolder;<init>(Lcom/eliotlash/mclib/math/Variable;Lcom/eliotlash/mclib/math/IValue;)V",
                    remap = false),
            index = 1,
            remap = false)
    private static IValue gbf$compileAssignment(IValue value) {
        return MolangCompiler.compile(value);
    }

    @ModifyArg(
            method = "parseOneLine",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/core/molang/expressions/MolangValue;<init>(Lcom/eliotlash/mclib/math/IValue;)V",
                    remap = false),
            index = 0,
            remap = false)
    private static IValue gbf$compileValue(IValue value) {
        return MolangCompiler.compile(value);
    }
}
