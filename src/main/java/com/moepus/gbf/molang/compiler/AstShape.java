package com.moepus.gbf.molang.compiler;

import com.moepus.gbf.molang.compiler.node.BinaryAstNode;
import com.moepus.gbf.molang.compiler.node.CachedAstNode;
import com.moepus.gbf.molang.compiler.node.ConstantAstNode;
import com.moepus.gbf.molang.compiler.node.DelegateAstNode;
import com.moepus.gbf.molang.compiler.node.ReferenceAstNode;
import com.moepus.gbf.molang.compiler.node.TernaryAstNode;
import com.moepus.gbf.molang.compiler.node.UnaryAstNode;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Structural key used to hash-cons generated classes without retaining parsed mclib trees. */
sealed interface AstShape {
    static AstShape of(AstNode root) {
        return new Factory().shape(root);
    }

    record Constant(long bits) implements AstShape {
    }

    record Reference(int index) implements AstShape {
    }

    record Binary(BinaryAstNode.BinaryOp op, AstShape left, AstShape right) implements AstShape {
    }

    record Unary(UnaryAstNode.UnaryOp op, AstShape operand) implements AstShape {
    }

    record Ternary(AstShape condition, AstShape ifTrue, AstShape ifFalse) implements AstShape {
    }

    record Delegate(DelegateAstNode.DelegateOp op, List<AstShape> arguments) implements AstShape {
        public Delegate {
            arguments = List.copyOf(arguments);
        }
    }

    record CachedDefinition(int id, AstShape operand) implements AstShape {
    }

    record CachedReference(int id) implements AstShape {
    }

    final class Factory {
        private final Map<CachedAstNode, Integer> cachedNodes = new IdentityHashMap<>();

        private AstShape shape(AstNode node) {
            if (node instanceof ConstantAstNode constant)
                return new Constant(Double.doubleToLongBits(constant.value()));
            if (node instanceof ReferenceAstNode reference)
                return new Reference(reference.referenceIndex());
            if (node instanceof BinaryAstNode binary)
                return new Binary(binary.op(), shape(binary.left()), shape(binary.right()));
            if (node instanceof UnaryAstNode unary)
                return new Unary(unary.op(), shape(unary.operand()));
            if (node instanceof TernaryAstNode ternary)
                return new Ternary(shape(ternary.condition()), shape(ternary.ifTrue()), shape(ternary.ifFalse()));
            if (node instanceof DelegateAstNode delegate)
                return new Delegate(delegate.op(), Arrays.stream(delegate.args()).map(this::shape).toList());
            if (node instanceof CachedAstNode cached)
                return cachedShape(cached);

            throw new IllegalArgumentException("Unknown AST node: " + node.getClass().getName());
        }

        private AstShape cachedShape(CachedAstNode node) {
            Integer existing = this.cachedNodes.get(node);

            if (existing != null)
                return new CachedReference(existing);

            int id = this.cachedNodes.size();
            this.cachedNodes.put(node, id);

            return new CachedDefinition(id, shape(node.operand()));
        }
    }
}
