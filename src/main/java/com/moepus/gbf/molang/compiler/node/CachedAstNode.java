package com.moepus.gbf.molang.compiler.node;

import com.moepus.gbf.molang.compiler.AstNode;
import com.moepus.gbf.molang.compiler.BytecodeGen;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public final class CachedAstNode implements AstNode {
    private final AstNode operand;

    public CachedAstNode(AstNode operand) {
        this.operand = operand;
    }

    public AstNode operand() {
        return this.operand;
    }

    @Override
    public double eval() {
        return this.operand.eval();
    }

    @Override
    public AstNode[] children() {
        return new AstNode[]{this.operand};
    }

    @Override
    public void emit(BytecodeGen.Context context, InstructionAdapter method) {
        method.load(context.cachedLocal(this, method), Type.DOUBLE_TYPE);
    }

    public CachedAstNode withOperand(AstNode operand) {
        return operand == this.operand ? this : new CachedAstNode(operand);
    }
}
