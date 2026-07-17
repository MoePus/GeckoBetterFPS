package com.moepus.gbf.molang.compiler.node;

import com.moepus.gbf.molang.compiler.AstNode;
import com.moepus.gbf.molang.compiler.BytecodeGen;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public record BinaryAstNode(BinaryOp op, AstNode left, AstNode right) implements AstNode {
    private static final double EQUALITY_EPSILON = 0.00001;

    @Override
    public double eval() {
        return this.op.eval(this.left.eval(), this.right.eval());
    }

    @Override
    public AstNode[] children() {
        return new AstNode[]{this.left, this.right};
    }

    @Override
    public void emit(BytecodeGen.Context context, InstructionAdapter method) {
        this.left.emit(context, method);
        this.right.emit(context, method);
        this.op.emit(method);
    }

    public BinaryAstNode withChildren(AstNode left, AstNode right) {
        return left == this.left && right == this.right ? this : new BinaryAstNode(this.op, left, right);
    }

    public static double divide(double a, double b) {
        return a / (b == 0 ? 1 : b);
    }

    public static double logicalAnd(double a, double b) {
        return a != 0 && b != 0 ? 1 : 0;
    }

    public static double logicalOr(double a, double b) {
        return a == 0 && b == 0 ? 0 : 1;
    }

    private static boolean approximatelyEqual(double a, double b) {
        return Math.abs(a - b) < EQUALITY_EPSILON;
    }

    public enum BinaryOp {
        ADD {
            @Override
            public double eval(double a, double b) {
                return a + b;
            }

            @Override
            void emit(InstructionAdapter method) {
                method.add(Type.DOUBLE_TYPE);
            }
        },
        SUB {
            @Override
            public double eval(double a, double b) {
                return a - b;
            }

            @Override
            void emit(InstructionAdapter method) {
                method.sub(Type.DOUBLE_TYPE);
            }
        },
        MUL {
            @Override
            public double eval(double a, double b) {
                return a * b;
            }

            @Override
            void emit(InstructionAdapter method) {
                method.mul(Type.DOUBLE_TYPE);
            }
        },
        DIV {
            @Override
            public double eval(double a, double b) {
                return divide(a, b);
            }

            @Override
            void emit(InstructionAdapter method) {
                emitBinaryHelper(method, "divide");
            }
        },
        MOD {
            @Override
            public double eval(double a, double b) {
                return a % b;
            }

            @Override
            void emit(InstructionAdapter method) {
                method.rem(Type.DOUBLE_TYPE);
            }
        },
        POW {
            @Override
            public double eval(double a, double b) {
                return Math.pow(a, b);
            }

            @Override
            void emit(InstructionAdapter method) {
                method.invokestatic(Type.getInternalName(Math.class), "pow", binaryDoubleDescriptor(), false);
            }
        },
        AND {
            @Override
            public double eval(double a, double b) {
                return logicalAnd(a, b);
            }

            @Override
            void emit(InstructionAdapter method) {
                emitBinaryHelper(method, "logicalAnd");
            }
        },
        OR {
            @Override
            public double eval(double a, double b) {
                return logicalOr(a, b);
            }

            @Override
            void emit(InstructionAdapter method) {
                emitBinaryHelper(method, "logicalOr");
            }
        },
        LT {
            @Override
            public double eval(double a, double b) {
                return a < b ? 1 : 0;
            }

            @Override
            void emit(InstructionAdapter method) {
                emitComparison(method, Opcodes.IFLT, true);
            }
        },
        LTE {
            @Override
            public double eval(double a, double b) {
                return a <= b ? 1 : 0;
            }

            @Override
            void emit(InstructionAdapter method) {
                emitComparison(method, Opcodes.IFLE, true);
            }
        },
        GT {
            @Override
            public double eval(double a, double b) {
                return a > b ? 1 : 0;
            }

            @Override
            void emit(InstructionAdapter method) {
                emitComparison(method, Opcodes.IFGT, false);
            }
        },
        GTE {
            @Override
            public double eval(double a, double b) {
                return a >= b ? 1 : 0;
            }

            @Override
            void emit(InstructionAdapter method) {
                emitComparison(method, Opcodes.IFGE, false);
            }
        },
        EQ {
            @Override
            public double eval(double a, double b) {
                return approximatelyEqual(a, b) ? 1 : 0;
            }

            @Override
            void emit(InstructionAdapter method) {
                emitEqualityOperands(method);
                emitComparison(method, Opcodes.IFLT, true);
            }
        },
        NE {
            @Override
            public double eval(double a, double b) {
                return approximatelyEqual(a, b) ? 0 : 1;
            }

            @Override
            void emit(InstructionAdapter method) {
                emitEqualityOperands(method);
                emitComparison(method, Opcodes.IFGE, true);
            }
        };

        public abstract double eval(double a, double b);

        abstract void emit(InstructionAdapter method);

        private static void emitBinaryHelper(InstructionAdapter method, String name) {
            method.invokestatic(Type.getInternalName(BinaryAstNode.class), name, binaryDoubleDescriptor(), false);
        }

        private static String binaryDoubleDescriptor() {
            return Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE);
        }

        private static void emitEqualityOperands(InstructionAdapter method) {
            method.sub(Type.DOUBLE_TYPE);
            method.invokestatic(Type.getInternalName(Math.class), "abs",
                    Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE), false);
            method.dconst(EQUALITY_EPSILON);
        }

        private static void emitComparison(InstructionAdapter method, int opcode, boolean nanIsGreater) {
            Label trueLabel = new Label();
            Label end = new Label();

            if (nanIsGreater)
                method.cmpg(Type.DOUBLE_TYPE);
            else
                method.cmpl(Type.DOUBLE_TYPE);

            method.visitJumpInsn(opcode, trueLabel);
            method.dconst(0);
            method.goTo(end);
            method.visitLabel(trueLabel);
            method.dconst(1);
            method.visitLabel(end);
        }
    }
}
