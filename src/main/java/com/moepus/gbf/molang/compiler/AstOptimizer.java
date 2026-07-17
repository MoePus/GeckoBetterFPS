package com.moepus.gbf.molang.compiler;

import com.moepus.gbf.molang.compiler.pass.ConstantFolding;
import com.moepus.gbf.molang.compiler.pass.IdentityElimination;
import com.moepus.gbf.molang.compiler.pass.StrengthReduction;

public final class AstOptimizer {
    private static final int MAX_ITERATIONS = 8;

    private AstOptimizer() {
    }

    public static AstNode optimize(AstNode node) {
        AstNode optimized = node;
        AstNode previous;
        int iteration = 0;

        do {
            previous = optimized;
            optimized = StrengthReduction.optimize(optimized);
            optimized = ConstantFolding.optimize(optimized);
            optimized = IdentityElimination.optimize(optimized);
            iteration++;
        } while (iteration < MAX_ITERATIONS && !previous.equals(optimized));

        return optimized;
    }
}
