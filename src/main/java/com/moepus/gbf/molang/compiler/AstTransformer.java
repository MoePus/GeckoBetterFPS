package com.moepus.gbf.molang.compiler;

@FunctionalInterface
public interface AstTransformer {
    AstNode transform(AstNode node);
}
