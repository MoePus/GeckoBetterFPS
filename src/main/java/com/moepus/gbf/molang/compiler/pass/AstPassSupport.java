package com.moepus.gbf.molang.compiler.pass;

import com.moepus.gbf.molang.compiler.AstNode;
import com.moepus.gbf.molang.compiler.node.BinaryAstNode;
import com.moepus.gbf.molang.compiler.node.CachedAstNode;
import com.moepus.gbf.molang.compiler.node.DelegateAstNode;
import com.moepus.gbf.molang.compiler.node.TernaryAstNode;
import com.moepus.gbf.molang.compiler.node.UnaryAstNode;

import java.util.Arrays;
import java.util.function.UnaryOperator;

final class AstPassSupport {
    private AstPassSupport() {
    }

    static AstNode mapChildren(AstNode node, UnaryOperator<AstNode> mapper) {
        if (node instanceof BinaryAstNode binary)
            return binary.withChildren(mapper.apply(binary.left()), mapper.apply(binary.right()));

        if (node instanceof CachedAstNode cached)
            return cached.withOperand(mapper.apply(cached.operand()));

        if (node instanceof UnaryAstNode unary)
            return unary.withOperand(mapper.apply(unary.operand()));

        if (node instanceof DelegateAstNode delegate)
            return delegate.withArgs(Arrays.stream(delegate.args()).map(mapper).toArray(AstNode[]::new));

        if (node instanceof TernaryAstNode ternary)
            return ternary.withChildren(mapper.apply(ternary.condition()), mapper.apply(ternary.ifTrue()),
                    mapper.apply(ternary.ifFalse()));

        return node;
    }
}
