package com.moepus.gbf.molang.compiler;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.IValue;
import com.moepus.gbf.molang.compiler.node.ConstantAstNode;

/**
 * Converts mclib's interpreted value tree to an optimized AST and emits JVM bytecode for it.
 */
public final class MolangCompiler {
    private MolangCompiler() {
    }

    public static IValue compile(IValue value) {
        try {
            CompiledAst converted = MclibToAst.convert(value);
            AstNode optimized = AstOptimizer.optimize(converted.root());

            if (optimized instanceof ConstantAstNode constant) {
                return new Constant(constant.value());
            }

            return BytecodeGen.compile(new CompiledAst(optimized, converted.references()));
        } catch (Exception | LinkageError ignored) {
            return value;
        }
    }
}
