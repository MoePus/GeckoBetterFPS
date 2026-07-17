package com.moepus.gbf.molang.compiler;

import com.eliotlash.mclib.math.IValue;

import java.util.List;

record CompiledAst(AstNode root, List<IValue> references) {
}
