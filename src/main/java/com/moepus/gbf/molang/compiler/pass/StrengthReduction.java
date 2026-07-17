package com.moepus.gbf.molang.compiler.pass;

import com.moepus.gbf.molang.compiler.AstNode;
import com.moepus.gbf.molang.compiler.node.BinaryAstNode;
import com.moepus.gbf.molang.compiler.node.CachedAstNode;
import com.moepus.gbf.molang.compiler.node.ConstantAstNode;

public final class StrengthReduction {
    private StrengthReduction() {
    }

    public static AstNode optimize(AstNode node) {
        AstNode optimized = AstPassSupport.mapChildren(node, StrengthReduction::optimize);

        if (!(optimized instanceof BinaryAstNode binary))
            return optimized;

        if (binary.op() != BinaryAstNode.BinaryOp.MUL)
            return optimized;

        if (isConstant(binary.left(), 2))
            return addCached(binary.right());

        if (isConstant(binary.right(), 2))
            return addCached(binary.left());

        return optimized;
    }

    private static BinaryAstNode addCached(AstNode node) {
        CachedAstNode cached = new CachedAstNode(node);

        return new BinaryAstNode(BinaryAstNode.BinaryOp.ADD, cached, cached);
    }

    private static boolean isConstant(AstNode node, double value) {
        return node instanceof ConstantAstNode constant && constant.value() == value;
    }
}
