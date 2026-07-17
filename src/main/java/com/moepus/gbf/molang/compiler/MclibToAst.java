package com.moepus.gbf.molang.compiler;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.Group;
import com.eliotlash.mclib.math.IValue;
import com.eliotlash.mclib.math.Negate;
import com.eliotlash.mclib.math.Negative;
import com.eliotlash.mclib.math.Operation;
import com.eliotlash.mclib.math.Operator;
import com.eliotlash.mclib.math.Ternary;
import com.eliotlash.mclib.math.Variable;
import com.eliotlash.mclib.math.functions.Function;
import com.moepus.gbf.molang.compiler.node.BinaryAstNode;
import com.moepus.gbf.molang.compiler.node.ConstantAstNode;
import com.moepus.gbf.molang.compiler.node.DelegateAstNode;
import com.moepus.gbf.molang.compiler.node.ReferenceAstNode;
import com.moepus.gbf.molang.compiler.node.TernaryAstNode;
import com.moepus.gbf.molang.compiler.node.UnaryAstNode;
import software.bernie.geckolib.core.molang.expressions.MolangValue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class MclibToAst {
    private static final String PI_VARIABLE_NAME = "math.pi";
    private static final Field GROUP_VALUE_FIELD = accessibleField(Group.class, "value");
    private static final Field FUNCTION_ARGS_FIELD = accessibleField(Function.class, "args");
    private static final Map<Operation, BinaryAstNode.BinaryOp> BINARY_OPERATIONS = Map.ofEntries(
            Map.entry(Operation.ADD, BinaryAstNode.BinaryOp.ADD),
            Map.entry(Operation.SUB, BinaryAstNode.BinaryOp.SUB),
            Map.entry(Operation.MUL, BinaryAstNode.BinaryOp.MUL),
            Map.entry(Operation.DIV, BinaryAstNode.BinaryOp.DIV),
            Map.entry(Operation.MOD, BinaryAstNode.BinaryOp.MOD),
            Map.entry(Operation.POW, BinaryAstNode.BinaryOp.POW),
            Map.entry(Operation.AND, BinaryAstNode.BinaryOp.AND),
            Map.entry(Operation.OR, BinaryAstNode.BinaryOp.OR),
            Map.entry(Operation.LESS, BinaryAstNode.BinaryOp.LT),
            Map.entry(Operation.LESS_THAN, BinaryAstNode.BinaryOp.LTE),
            Map.entry(Operation.GREATER, BinaryAstNode.BinaryOp.GT),
            Map.entry(Operation.GREATER_THAN, BinaryAstNode.BinaryOp.GTE),
            Map.entry(Operation.EQUALS, BinaryAstNode.BinaryOp.EQ),
            Map.entry(Operation.NOT_EQUALS, BinaryAstNode.BinaryOp.NE));

    private final Map<IValue, AstNode> nodes = new IdentityHashMap<>();
    private final Map<IValue, Integer> referenceIndexes = new IdentityHashMap<>();
    private final List<IValue> references = new ArrayList<>();

    private MclibToAst() {
    }

    static CompiledAst convert(IValue value) {
        MclibToAst converter = new MclibToAst();

        return new CompiledAst(converter.nodeFor(value), List.copyOf(converter.references));
    }

    private AstNode nodeFor(IValue value) {
        return this.nodes.computeIfAbsent(value, this::createNode);
    }

    private AstNode createNode(IValue value) {
        if (value.getClass() == MolangValue.class)
            return nodeFor(((MolangValue) value).getValueHolder());
        if (value instanceof Constant)
            return new ConstantAstNode(value.get());
        if (value instanceof Variable variable)
            return variableNode(variable);
        if (value instanceof Operator operator)
            return binaryNode(operator);
        if (value instanceof Negative negative)
            return new UnaryAstNode(UnaryAstNode.UnaryOp.NEGATE, nodeFor(negative.value));
        if (value instanceof Negate negate)
            return new UnaryAstNode(UnaryAstNode.UnaryOp.BOOLEAN_NEGATE, nodeFor(negate.value));
        if (value instanceof Ternary ternary)
            return new TernaryAstNode(nodeFor(ternary.condition), nodeFor(ternary.ifTrue), nodeFor(ternary.ifFalse));
        if (value instanceof Group group)
            return groupNode(group);
        if (value instanceof Function function)
            return functionNode(value, function);

        return new ReferenceAstNode(reference(value));
    }

    private AstNode variableNode(Variable variable) {
        if (PI_VARIABLE_NAME.equals(variable.getName())
                && Double.doubleToRawLongBits(variable.get()) == Double.doubleToRawLongBits(Math.PI))
            return new ConstantAstNode(Math.PI);

        return new ReferenceAstNode(reference(variable));
    }

    private AstNode binaryNode(Operator operator) {
        BinaryAstNode.BinaryOp operation = BINARY_OPERATIONS.get(operator.operation);

        if (operation == null)
            return new ReferenceAstNode(reference(operator));

        return new BinaryAstNode(operation, nodeFor(operator.a), nodeFor(operator.b));
    }

    private AstNode groupNode(Group group) {
        Object child = fieldValue(GROUP_VALUE_FIELD, group);

        return child instanceof IValue value ? nodeFor(value) : new ReferenceAstNode(reference(group));
    }

    private AstNode functionNode(IValue value, Function function) {
        DelegateAstNode.DelegateOp operation = DelegateAstNode.DelegateOp.byName(function.getName());
        List<IValue> arguments = functionArguments(function);

        if (operation == null || arguments.size() < operation.minArgs())
            return new ReferenceAstNode(reference(value));

        AstNode[] compiledArguments = arguments.stream()
                .limit(operation.minArgs())
                .map(this::nodeFor)
                .toArray(AstNode[]::new);

        if (operation == DelegateAstNode.DelegateOp.SIN || operation == DelegateAstNode.DelegateOp.COS)
            compiledArguments[0] = radiansNode(compiledArguments[0]);

        return new DelegateAstNode(operation, compiledArguments);
    }

    private static AstNode radiansNode(AstNode degrees) {
        AstNode scale = new BinaryAstNode(BinaryAstNode.BinaryOp.MUL,
                new ConstantAstNode(1.0 / 180.0), new ConstantAstNode(Math.PI));

        return new BinaryAstNode(BinaryAstNode.BinaryOp.MUL, degrees, scale);
    }

    private int reference(IValue value) {
        return this.referenceIndexes.computeIfAbsent(value, ignored -> {
            this.references.add(value);

            return this.references.size() - 1;
        });
    }

    private static List<IValue> functionArguments(Function function) {
        Object arguments = fieldValue(FUNCTION_ARGS_FIELD, function);

        if (!(arguments instanceof IValue[] values))
            return List.of();

        return Arrays.stream(values).filter(value -> value != null).toList();
    }

    private static Field accessibleField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);

            return field;
        } catch (NoSuchFieldException | RuntimeException ignored) {
            return null;
        }
    }

    private static Object fieldValue(Field field, Object owner) {
        if (field == null)
            return null;

        try {
            return field.get(owner);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return null;
        }
    }
}
