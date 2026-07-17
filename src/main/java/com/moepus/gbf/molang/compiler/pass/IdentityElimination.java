package com.moepus.gbf.molang.compiler.pass;

import com.moepus.gbf.molang.compiler.AstNode;
import com.moepus.gbf.molang.compiler.node.BinaryAstNode;
import com.moepus.gbf.molang.compiler.node.ConstantAstNode;

public final class IdentityElimination {
    private IdentityElimination() {
    }

    public static AstNode optimize(AstNode node) {
        AstNode optimized = AstPassSupport.mapChildren(node, IdentityElimination::optimize);

        if (!(optimized instanceof BinaryAstNode binary))
            return optimized;

        ConstantAstNode left = binary.left() instanceof ConstantAstNode constant ? constant : null;
        ConstantAstNode right = binary.right() instanceof ConstantAstNode constant ? constant : null;

        return switch (binary.op()) {
            case SUB -> isPositiveZero(right) ? binary.left() : binary;
            case MUL -> {
                if (isOne(left)) yield binary.right();
                if (isOne(right)) yield binary.left();
                yield binary;
            }
            case DIV -> isOne(right) ? binary.left() : binary;
            case POW -> isOne(right) ? binary.left() : binary;
            default -> binary;
        };
    }

    private static boolean isPositiveZero(ConstantAstNode node) {
        return node != null && Double.doubleToRawLongBits(node.value()) == 0L;
    }

    private static boolean isOne(ConstantAstNode node) {
        return node != null && node.value() == 1;
    }
}
