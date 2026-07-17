package com.moepus.gbf.molang.compiler.node;

import com.moepus.gbf.molang.compiler.AstNode;
import com.moepus.gbf.molang.compiler.BytecodeGen;
import org.objectweb.asm.commons.InstructionAdapter;

public record ReferenceAstNode(int referenceIndex) implements AstNode {
    @Override
    public double eval() {
        throw new UnsupportedOperationException("Reference-backed AST nodes cannot be folded directly");
    }

    @Override
    public AstNode[] children() {
        return new AstNode[0];
    }

    @Override
    public void emit(BytecodeGen.Context context, InstructionAdapter method) {
        context.emitReference(this.referenceIndex, method);
    }
}
