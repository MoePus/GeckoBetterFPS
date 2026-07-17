package com.moepus.gbf.molang.compiler;

import com.eliotlash.mclib.math.IValue;
import com.moepus.gbf.molang.compiler.node.CachedAstNode;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public final class BytecodeGen {
    private static final Path MOLANG_CACHE_DIR = Path.of("cache", "molang");
    private static final String GENERATED_CLASS_PREFIX =
            BytecodeGen.class.getPackageName().replace('.', '/') + "/MolangCompiled_";
    private static final ConcurrentMap<AstShape, CompiledFactory> COMPILED_CLASSES = new ConcurrentHashMap<>();
    private static final AtomicLong ORDINAL = new AtomicLong();

    static {
        clearCacheDir();
    }

    private BytecodeGen() {
    }

    static IValue compile(CompiledAst ast) throws Exception {
        CompiledFactory factory = factoryFor(ast.root());

        return factory.bind(ast.references());
    }

    private static CompiledFactory factoryFor(AstNode root) throws Exception {
        AstShape shape = AstShape.of(root);

        try {
            return COMPILED_CLASSES.computeIfAbsent(shape, ignored -> generateUnchecked(root));
        } catch (CompilationFailure failure) {
            throw failure.compilationCause;
        }
    }

    private static CompiledFactory generateUnchecked(AstNode root) {
        try {
            return generateFactory(root);
        } catch (Exception exception) {
            throw new CompilationFailure(exception);
        }
    }

    private static CompiledFactory generateFactory(AstNode root) throws Exception {
        String className = GENERATED_CLASS_PREFIX + ORDINAL.getAndIncrement();
        Context context = new Context(className, Type.getType(IValue.class));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, className, null,
                Type.getInternalName(Object.class), new String[]{Type.getInternalName(IValue.class)});
        writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "references",
                Type.getDescriptor(Object[].class), null, null).visitEnd();
        generateConstructor(writer, context);
        generateGet(writer, context, root);
        writer.visitEnd();

        byte[] bytes = writer.toByteArray();
        dumpClass(className, bytes);

        Class<?> generatedClass = MethodHandles.lookup().defineHiddenClass(bytes, true).lookupClass();
        Constructor<?> constructor = generatedClass.getConstructor(Object[].class);

        return references -> (IValue) constructor.newInstance((Object) references.toArray());
    }

    private static void dumpClass(String className, byte[] bytes) {
        try {
            Files.createDirectories(MOLANG_CACHE_DIR);
            Files.write(MOLANG_CACHE_DIR.resolve(className.substring(className.lastIndexOf('/') + 1) + ".class"), bytes);
        } catch (IOException ignored) {
        }
    }

    private static void clearCacheDir() {
        if (!Files.exists(MOLANG_CACHE_DIR))
            return;

        try (Stream<Path> paths = Files.walk(MOLANG_CACHE_DIR)) {
            paths.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(MOLANG_CACHE_DIR))
                    .forEach(BytecodeGen::deleteCacheEntry);
        } catch (IOException ignored) {
        }
    }

    private static void deleteCacheEntry(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private static void generateConstructor(ClassWriter writer, Context context) {
        InstructionAdapter method = new InstructionAdapter(writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object[].class)), null, null));
        Label start = new Label();
        Label end = new Label();

        method.visitCode();
        method.visitLabel(start);
        method.load(0, InstructionAdapter.OBJECT_TYPE);
        method.invokespecial(Type.getInternalName(Object.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);
        method.load(0, InstructionAdapter.OBJECT_TYPE);
        method.load(1, InstructionAdapter.OBJECT_TYPE);
        method.putfield(context.className(), "references", Type.getDescriptor(Object[].class));
        method.areturn(Type.VOID_TYPE);
        method.visitLabel(end);
        method.visitLocalVariable("this", "L" + context.className() + ";", null, start, end, 0);
        method.visitLocalVariable("references", Type.getDescriptor(Object[].class), null, start, end, 1);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void generateGet(ClassWriter writer, Context context, AstNode root) {
        InstructionAdapter method = new InstructionAdapter(writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "get",
                Type.getMethodDescriptor(Type.DOUBLE_TYPE), null, null));
        Label start = new Label();
        Label end = new Label();
        context.resetLocals(1);

        method.visitCode();
        method.visitLabel(start);
        root.emitReturn(context, method);
        method.visitLabel(end);
        method.visitLocalVariable("this", "L" + context.className() + ";", null, start, end, 0);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    @FunctionalInterface
    private interface CompiledFactory {
        IValue bind(List<IValue> references) throws Exception;
    }

    private static final class CompilationFailure extends RuntimeException {
        private final Exception compilationCause;

        private CompilationFailure(Exception cause) {
            super(cause);
            this.compilationCause = cause;
        }
    }

    public static final class Context {
        private final String className;
        private final Type iValueType;
        private final Map<CachedAstNode, Integer> cachedLocals = new IdentityHashMap<>();
        private int nextLocal = 1;

        private Context(String className, Type iValueType) {
            this.className = className;
            this.iValueType = iValueType;
        }

        public String className() {
            return this.className;
        }

        public Context childScope() {
            Context child = new Context(this.className, this.iValueType);
            child.nextLocal = this.nextLocal;

            return child;
        }

        public void mergeLocals(Context child) {
            this.nextLocal = Math.max(this.nextLocal, child.nextLocal);
        }

        public void emitReference(int referenceIndex, InstructionAdapter method) {
            emitReferenceLoad(referenceIndex, method);
        }

        public int cachedLocal(CachedAstNode node, InstructionAdapter method) {
            Integer existing = this.cachedLocals.get(node);

            if (existing != null)
                return existing;

            int local = this.nextLocal;
            this.nextLocal += Type.DOUBLE_TYPE.getSize();
            this.cachedLocals.put(node, local);
            node.operand().emit(this, method);
            method.store(local, Type.DOUBLE_TYPE);

            return local;
        }

        private void resetLocals(int nextLocal) {
            this.cachedLocals.clear();
            this.nextLocal = nextLocal;
        }

        private void emitReferenceLoad(int referenceIndex, InstructionAdapter method) {
            method.load(0, InstructionAdapter.OBJECT_TYPE);
            method.getfield(this.className, "references", Type.getDescriptor(Object[].class));
            method.iconst(referenceIndex);
            method.aload(InstructionAdapter.OBJECT_TYPE);
            method.checkcast(this.iValueType);
            method.invokeinterface(this.iValueType.getInternalName(), "get",
                    Type.getMethodDescriptor(Type.DOUBLE_TYPE));
        }
    }
}
