package com.moepus.gbf.molang.compiler;

@FunctionalInterface
public interface AstPass {
    AstNode apply(AstNode node);
}
