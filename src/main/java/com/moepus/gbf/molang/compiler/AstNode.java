package com.moepus.gbf.molang.compiler;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public interface AstNode {
    double eval();

    AstNode[] children();

    default AstNode transform(AstTransformer transformer) {
        return transformer.transform(this);
    }

    void emit(BytecodeGen.Context context, InstructionAdapter method);

    default void emitReturn(BytecodeGen.Context context, InstructionAdapter method) {
        emit(context, method);
        method.areturn(Type.DOUBLE_TYPE);
    }
}
