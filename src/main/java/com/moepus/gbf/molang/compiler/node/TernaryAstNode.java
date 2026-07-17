package com.moepus.gbf.molang.compiler.node;

import com.moepus.gbf.molang.compiler.AstNode;
import com.moepus.gbf.molang.compiler.BytecodeGen;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public record TernaryAstNode(AstNode condition, AstNode ifTrue, AstNode ifFalse) implements AstNode {
    @Override
    public double eval() {
        return this.condition.eval() != 0 ? this.ifTrue.eval() : this.ifFalse.eval();
    }

    @Override
    public AstNode[] children() {
        return new AstNode[]{this.condition, this.ifTrue, this.ifFalse};
    }

    @Override
    public void emit(BytecodeGen.Context context, InstructionAdapter method) {
        Label falseLabel = new Label();
        Label end = new Label();
        BytecodeGen.Context conditionScope = context.childScope();
        BytecodeGen.Context trueScope = context.childScope();
        BytecodeGen.Context falseScope = context.childScope();

        this.condition.emit(conditionScope, method);
        method.dconst(0);
        method.cmpl(Type.DOUBLE_TYPE);
        method.visitJumpInsn(Opcodes.IFEQ, falseLabel);
        this.ifTrue.emit(trueScope, method);
        method.goTo(end);
        method.visitLabel(falseLabel);
        this.ifFalse.emit(falseScope, method);
        method.visitLabel(end);
        context.mergeLocals(conditionScope);
        context.mergeLocals(trueScope);
        context.mergeLocals(falseScope);
    }

    public TernaryAstNode withChildren(AstNode condition, AstNode ifTrue, AstNode ifFalse) {
        return condition == this.condition && ifTrue == this.ifTrue && ifFalse == this.ifFalse
                ? this
                : new TernaryAstNode(condition, ifTrue, ifFalse);
    }
}
