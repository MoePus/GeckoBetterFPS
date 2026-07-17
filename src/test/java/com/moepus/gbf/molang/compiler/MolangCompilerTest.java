package com.moepus.gbf.molang.compiler;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.Group;
import com.eliotlash.mclib.math.IValue;
import com.eliotlash.mclib.math.Negate;
import com.eliotlash.mclib.math.Operation;
import com.eliotlash.mclib.math.Operator;
import com.eliotlash.mclib.math.Ternary;
import com.eliotlash.mclib.math.Variable;
import com.eliotlash.mclib.math.functions.limit.Clamp;
import com.eliotlash.mclib.math.functions.limit.Min;
import com.eliotlash.mclib.math.functions.utility.Lerp;
import com.moepus.gbf.molang.compiler.node.BinaryAstNode;
import com.moepus.gbf.molang.compiler.node.ConstantAstNode;
import com.moepus.gbf.molang.compiler.node.ReferenceAstNode;
import com.moepus.gbf.molang.compiler.pass.ConstantFolding;
import com.moepus.gbf.molang.compiler.pass.StrengthReduction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib.core.molang.LazyVariable;
import software.bernie.geckolib.core.molang.MolangParser;
import software.bernie.geckolib.core.molang.functions.SinDegrees;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MolangCompilerTest {
    @Test
    void structurallyEqualTreesShareGeneratedClassAndKeepTheirOwnReferences() throws IOException {
        Variable firstVariable = new Variable("first", 2);
        Variable secondVariable = new Variable("second", 5);
        long before = dumpedClassCount();

        IValue firstCompiled = MolangCompiler.compile(addOne(firstVariable));
        long afterFirst = dumpedClassCount();
        IValue secondCompiled = MolangCompiler.compile(addOne(secondVariable));

        assertSame(firstCompiled.getClass(), secondCompiled.getClass());
        assertEquals(afterFirst, dumpedClassCount());
        assertTrue(afterFirst >= before);
        assertEquals(3, firstCompiled.get());
        assertEquals(6, secondCompiled.get());

        firstVariable.set(10);
        secondVariable.set(20);
        assertEquals(11, firstCompiled.get());
        assertEquals(21, secondCompiled.get());
    }

    @Test
    void nestedOperatorUsesTheActualRootOperation() {
        IValue nested = new Operator(Operation.MUL,
                new Group(new Operator(Operation.ADD, new Constant(1), new Constant(2))),
                new Constant(3));

        assertEquivalent(nested);
    }

    @Test
    void actualMolangParserTreesMatchTheInterpreter() throws Exception {
        String[] expressions = {
                "(1 + 2) * 3",
                "!1",
                "math.min(3, 2, 1)",
                "math.clamp(5, 0, 10)",
                "math.lerp(2, 6, 0.25)",
                "math.sin(90)",
                "5 / 0",
                "5 % 0"
        };

        for (String expression : expressions) {
            assertEquivalent(MolangParser.INSTANCE.parse(expression));
        }
    }

    @Test
    void numericConstantsAndMathPiFoldToOneConstant() throws Exception {
        IValue numericExpression = new Operator(Operation.ADD,
                new Constant((double) 1.5F), new Constant(2.25D));
        IValue piExpression = MolangParser.INSTANCE.parse("math.pi + 1.5 * 2 + 0.25");

        assertInstanceOf(Constant.class, MolangCompiler.compile(numericExpression));
        IValue compiledPi = MolangCompiler.compile(piExpression);
        assertInstanceOf(Constant.class, compiledPi);
        assertSameDouble(piExpression.get(), compiledPi.get());
    }

    @Test
    void constantFoldingIsNotBlockedByStrengthReduction() {
        AstNode multiplication = new BinaryAstNode(BinaryAstNode.BinaryOp.MUL,
                new ConstantAstNode(1.5F), new ConstantAstNode(2.0D));
        AstNode strengthFirst = ConstantFolding.optimize(StrengthReduction.optimize(multiplication));
        AstNode foldingFirst = StrengthReduction.optimize(ConstantFolding.optimize(multiplication));

        assertEquals(new ConstantAstNode(3.0), strengthFirst);
        assertEquals(strengthFirst, foldingFirst);
    }

    @Test
    void adjacentConstantMultipliersAreCombined() {
        AstNode radiansScale = new BinaryAstNode(BinaryAstNode.BinaryOp.MUL,
                new ConstantAstNode(1.0 / 180.0), new ConstantAstNode(Math.PI));
        AstNode degrees = new BinaryAstNode(BinaryAstNode.BinaryOp.MUL,
                new ReferenceAstNode(0), new ConstantAstNode(180.0));
        AstNode expression = new BinaryAstNode(BinaryAstNode.BinaryOp.MUL, degrees, radiansScale);

        assertEquals(new BinaryAstNode(BinaryAstNode.BinaryOp.MUL,
                new ReferenceAstNode(0), new ConstantAstNode(Math.PI)), AstOptimizer.optimize(expression));
    }

    @Test
    void trigBytecodeUsesOnePrecomputedRadiansConstant() throws Exception {
        Variable angle = new Variable("angle", 0.25);
        IValue expression = new SinDegrees(new IValue[]{
                new Operator(Operation.MUL, angle, new Constant(180))
        }, "math.sin");
        IValue compiled = MolangCompiler.compile(expression);
        BytecodeConstants constants = readGeneratedConstants(compiled);

        assertFalse(constants.readsMathPi());
        assertTrue(constants.doubles().contains(Math.PI));
        assertFalse(constants.doubles().contains(180.0));
        assertFalse(constants.doubles().contains(1.0 / 180.0 * Math.PI));
        assertSameDouble(expression.get(), compiled.get());
    }

    @Test
    void booleanNegateIsNotArithmeticNegative() {
        Variable value = new Variable("value", 1);
        IValue interpreted = new Negate(value);
        IValue compiled = MolangCompiler.compile(interpreted);

        assertNotSame(interpreted, compiled);
        assertEquals(0, compiled.get());
        value.set(0);
        assertEquals(1, compiled.get());
    }

    @Test
    void allMclibOperationsMatchTheInterpreter() {
        for (Operation operation : Operation.values()) {
            IValue expression = new Operator(operation,
                    new Variable("left", 5), new Variable("right", 2));
            assertEquivalent(expression);
        }

        assertEquivalent(new Operator(Operation.DIV,
                new Variable("left", 5), new Variable("zero", 0)));
        assertEquivalent(new Operator(Operation.MOD,
                new Variable("left", 5), new Variable("zero", 0)));
    }

    @Test
    void nanComparisonsMatchTheInterpreter() {
        Operation[] comparisons = {
                Operation.LESS, Operation.LESS_THAN, Operation.GREATER, Operation.GREATER_THAN,
                Operation.EQUALS, Operation.NOT_EQUALS
        };

        for (Operation operation : comparisons) {
            IValue expression = new Operator(operation,
                    new Variable("nan", Double.NaN), new Variable("zero", 0));
            assertEquivalent(expression);
        }
    }

    @Test
    void ordinaryReferencesAreEvaluatedEveryTimeTheyAppear() {
        AtomicInteger calls = new AtomicInteger();
        LazyVariable variable = new LazyVariable("query.dynamic", calls::incrementAndGet);
        IValue expression = new Operator(Operation.ADD, variable, variable);
        IValue compiled = MolangCompiler.compile(expression);

        assertNotSame(expression, compiled);
        calls.set(0);
        assertEquals(3, compiled.get());
        assertEquals(2, calls.get());
    }

    @Test
    void unsafeIeee754IdentitiesAreNotEliminated() {
        assertEquivalent(new Operator(Operation.MUL,
                new Variable("nan", Double.NaN), new Constant(0)));
        assertEquivalent(new Operator(Operation.ADD,
                new Variable("negative_zero", -0.0), new Constant(0.0)));
        assertEquivalent(new Operator(Operation.POW,
                new Variable("negative_infinity", Double.NEGATIVE_INFINITY), new Constant(0.5)));
    }

    @Test
    void supportedFunctionsMatchMclibEdgeSemantics() throws Exception {
        CountingValue ignored = new CountingValue(1);
        assertEquivalent(new Min(new IValue[]{new Constant(3), new Constant(2), ignored}, "math.min"));
        assertEquals(0, ignored.calls());

        assertEquivalent(new Clamp(new IValue[]{
                new Constant(5), new Constant(Double.NaN), new Constant(10)
        }, "math.clamp"));

        CountingValue start = new CountingValue(2);
        CountingValue end = new CountingValue(6);
        CountingValue position = new CountingValue(0.25);
        IValue lerp = new Lerp(new IValue[]{start, end, position}, "math.lerp");
        IValue compiledLerp = MolangCompiler.compile(lerp);
        assertEquals(3, compiledLerp.get());
        assertEquals(1, start.calls());
        assertEquals(1, end.calls());
        assertEquals(1, position.calls());
    }

    @Test
    void ternaryOnlyEvaluatesTheSelectedBranch() {
        Variable condition = new Variable("condition", 1);
        CountingValue ifTrue = new CountingValue(4);
        CountingValue ifFalse = new CountingValue(8);
        IValue compiled = MolangCompiler.compile(new Ternary(condition, ifTrue, ifFalse));

        assertEquals(4, compiled.get());
        assertEquals(1, ifTrue.calls());
        assertEquals(0, ifFalse.calls());

        condition.set(0);
        assertEquals(8, compiled.get());
        assertEquals(1, ifTrue.calls());
        assertEquals(1, ifFalse.calls());
    }

    private static IValue addOne(Variable variable) {
        return new Operator(Operation.ADD, variable, new Constant(1));
    }

    private static void assertEquivalent(IValue interpreted) {
        IValue compiled = MolangCompiler.compile(interpreted);

        assertNotSame(interpreted, compiled);
        assertSameDouble(interpreted.get(), compiled.get());
    }

    private static void assertSameDouble(double expected, double actual) {
        if (Double.isNaN(expected)) {
            assertTrue(Double.isNaN(actual));
            return;
        }

        assertFalse(Double.isNaN(actual));
        assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(actual));
    }

    private static long dumpedClassCount() throws IOException {
        Path cache = Path.of("cache", "molang");

        if (!Files.isDirectory(cache))
            return 0;

        try (var classes = Files.list(cache)) {
            return classes.filter(path -> path.getFileName().toString().endsWith(".class")).count();
        }
    }

    private static BytecodeConstants readGeneratedConstants(IValue compiled) throws IOException {
        String binaryName = compiled.getClass().getName();
        int hiddenSuffix = binaryName.indexOf('/');

        if (hiddenSuffix >= 0)
            binaryName = binaryName.substring(0, hiddenSuffix);

        Path classFile = Path.of("cache", "molang",
                binaryName.substring(binaryName.lastIndexOf('.') + 1) + ".class");
        List<Double> doubles = new ArrayList<>();
        boolean[] readsMathPi = {false};
        ClassReader reader = new ClassReader(Files.readAllBytes(classFile));

        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Double number)
                            doubles.add(number);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        if (opcode == Opcodes.GETSTATIC && owner.equals("java/lang/Math") && name.equals("PI"))
                            readsMathPi[0] = true;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return new BytecodeConstants(List.copyOf(doubles), readsMathPi[0]);
    }

    private record BytecodeConstants(List<Double> doubles, boolean readsMathPi) {
    }

    private static final class CountingValue implements IValue {
        private final double value;
        private int calls;

        private CountingValue(double value) {
            this.value = value;
        }

        @Override
        public double get() {
            this.calls++;

            return this.value;
        }

        private int calls() {
            return this.calls;
        }
    }
}
