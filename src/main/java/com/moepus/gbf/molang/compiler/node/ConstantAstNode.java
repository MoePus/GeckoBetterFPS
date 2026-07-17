package com.moepus.gbf.molang.compiler.node;

import com.moepus.gbf.molang.compiler.AstNode;
import com.moepus.gbf.molang.compiler.BytecodeGen;
import org.objectweb.asm.commons.InstructionAdapter;

public record ConstantAstNode(double value) implements AstNode {
    @Override
    public double eval() {
        return this.value;
    }

    @Override
    public AstNode[] children() {
        return new AstNode[0];
    }

    @Override
    public void emit(BytecodeGen.Context context, InstructionAdapter method) {
        method.dconst(this.value);
    }
}
