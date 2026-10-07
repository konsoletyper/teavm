/*
 *  Copyright 2026 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.impl.regex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.teavm.classlib.java.util.regex.TPattern;
import org.teavm.classlib.java.util.regex.TPatternWriter;
import org.teavm.common.DisjointSet;
import org.teavm.model.AccessLevel;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

/**
 * Replaces calls to {@link Pattern#compile(String)} and {@link Pattern#compile(String, int)} with constant
 * arguments by code that directly constructs the compiled pattern. The pattern is compiled at build time
 * by running classlib's regex engine in the compiler's JVM, and the resulting object graph is turned into
 * calls to factory methods by {@link TPatternWriter} and {@link PatternProgramBuilder}.
 *
 * <p>Also handles class library methods that compile a regular expression passed as argument, like
 * {@link String#split(String)} or {@link String#replaceAll(String, String)}. When regular expression
 * is a constant, such calls are expanded into their implementation that takes pre-compiled pattern.
 * Unlike {@code Pattern.compile}, which must return a new instance every time, these methods don't expose
 * pattern instance, so it is created once and cached in a static field.
 */
public class PatternCompileTransformer implements ClassHolderTransformer {
    private static final MethodReference COMPILE = new MethodReference(Pattern.class, "compile",
            String.class, Pattern.class);
    private static final MethodReference COMPILE_WITH_FLAGS = new MethodReference(Pattern.class, "compile",
            String.class, int.class, Pattern.class);
    private static final MethodReference MATCHER = new MethodReference(Pattern.class, "matcher",
            CharSequence.class, Matcher.class);
    private static final MethodReference PATTERN_SPLIT = new MethodReference(Pattern.class, "split",
            CharSequence.class, String[].class);
    private static final MethodReference PATTERN_SPLIT_WITH_LIMIT = new MethodReference(Pattern.class, "split",
            CharSequence.class, int.class, String[].class);
    private static final MethodReference MATCHER_MATCHES = new MethodReference(Matcher.class, "matches",
            boolean.class);
    private static final MethodReference MATCHER_REPLACE_ALL = new MethodReference(Matcher.class, "replaceAll",
            String.class, String.class);
    private static final MethodReference MATCHER_REPLACE_FIRST = new MethodReference(Matcher.class,
            "replaceFirst", String.class, String.class);
    private static final MethodReference STRING_SPLIT = new MethodReference(String.class, "split",
            String.class, String[].class);
    private static final MethodReference STRING_SPLIT_WITH_LIMIT = new MethodReference(String.class, "split",
            String.class, int.class, String[].class);
    private static final MethodReference STRING_SPLIT_BY_CHAR = new MethodReference(String.class.getName(),
            "splitByChar", ValueType.CHARACTER, ValueType.INTEGER, ValueType.arrayOf(ValueType.object(
                    String.class.getName())));
    private static final String FACTORY_CLASS = "java.util.regex.PatternFactory";
    private static final ValueType PATTERN_TYPE = ValueType.object(Pattern.class.getName());

    /**
     * Methods that take regular expression as the first argument, mapped to their implementation
     * in terms of pre-compiled pattern. Should be kept in sync with actual implementation of these methods.
     */
    private static final Map<MethodReference, Expansion> CONSUMERS = Map.of(
            new MethodReference(String.class, "matches", String.class, boolean.class),
            (e, invoke) -> e.call(MATCHER_MATCHES, e.call(MATCHER, e.pattern, invoke.getInstance())),
            STRING_SPLIT,
            (e, invoke) -> e.call(PATTERN_SPLIT, e.pattern, invoke.getInstance()),
            STRING_SPLIT_WITH_LIMIT,
            (e, invoke) -> e.call(PATTERN_SPLIT_WITH_LIMIT, e.pattern, invoke.getInstance(),
                    invoke.getArguments().get(1)),
            new MethodReference(String.class, "replaceAll", String.class, String.class, String.class),
            (e, invoke) -> e.call(MATCHER_REPLACE_ALL, e.call(MATCHER, e.pattern, invoke.getInstance()),
                    invoke.getArguments().get(1)),
            new MethodReference(String.class, "replaceFirst", String.class, String.class, String.class),
            (e, invoke) -> e.call(MATCHER_REPLACE_FIRST, e.call(MATCHER, e.pattern, invoke.getInstance()),
                    invoke.getArguments().get(1)),
            new MethodReference(Pattern.class, "matches", String.class, CharSequence.class, boolean.class),
            (e, invoke) -> e.call(MATCHER_MATCHES, e.call(MATCHER, e.pattern, invoke.getArguments().get(1)))
    );

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        var state = new ClassState(cls, context);
        for (var method : List.copyOf(cls.getMethods())) {
            var program = method.getProgram();
            if (program != null && hasCandidateCalls(program)) {
                transformProgram(program, state);
            }
        }
    }

    private boolean hasCandidateCalls(Program program) {
        for (var block : program.getBasicBlocks()) {
            for (var instruction : block) {
                if (instruction instanceof InvokeInstruction && isCandidate((InvokeInstruction) instruction)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCandidate(InvokeInstruction invoke) {
        var method = invoke.getMethod();
        return method.equals(COMPILE) || method.equals(COMPILE_WITH_FLAGS) || CONSUMERS.containsKey(method);
    }

    private void transformProgram(Program program, ClassState state) {
        var varSet = new DisjointSet();
        for (int i = 0; i < program.variableCount(); i++) {
            varSet.create();
        }
        var stringConstants = new String[program.variableCount()];
        var intConstants = new Integer[program.variableCount()];
        var invocations = new ArrayList<InvokeInstruction>();
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (instruction instanceof StringConstantInstruction constant) {
                    stringConstants[constant.getReceiver().getIndex()] = constant.getConstant();
                } else if (instruction instanceof IntegerConstantInstruction constant) {
                    intConstants[constant.getReceiver().getIndex()] = constant.getConstant();
                } else if (instruction instanceof AssignInstruction assign) {
                    varSet.union(assign.getAssignee().getIndex(), assign.getReceiver().getIndex());
                } else if (instruction instanceof InvokeInstruction invoke) {
                    if (isCandidate(invoke)) {
                        invocations.add(invoke);
                    }
                }
            }
        }

        var stringConstantsByClass = new String[varSet.size()];
        var intConstantsByClass = new Integer[varSet.size()];
        for (int i = 0; i < program.variableCount(); i++) {
            int varClass = varSet.find(i);
            if (stringConstants[i] != null) {
                stringConstantsByClass[varClass] = stringConstants[i];
            }
            if (intConstants[i] != null) {
                intConstantsByClass[varClass] = intConstants[i];
            }
        }

        for (var invoke : invocations) {
            var args = invoke.getArguments();
            var source = stringConstantsByClass[varSet.find(args.get(0).getIndex())];
            if (source == null) {
                continue;
            }
            int flags = 0;
            if (invoke.getMethod().equals(COMPILE_WITH_FLAGS)) {
                var flagsConstant = intConstantsByClass[varSet.find(args.get(1).getIndex())];
                if (flagsConstant == null) {
                    continue;
                }
                flags = flagsConstant;
            }

            if (tryExpandSplitByChar(program, invoke, source)) {
                continue;
            }

            var key = new PatternKey(source, flags);
            var expansion = CONSUMERS.get(invoke.getMethod());
            if (expansion == null) {
                var factory = state.factory(key);
                if (factory == null) {
                    continue;
                }
                var replacement = new InvokeInstruction();
                replacement.setType(InvocationType.SPECIAL);
                replacement.setMethod(factory);
                replacement.setReceiver(invoke.getReceiver());
                replacement.setLocation(invoke.getLocation());
                invoke.replace(replacement);
            } else {
                var cachedFactory = state.cachedFactory(key);
                if (cachedFactory == null) {
                    continue;
                }
                var emitter = new Emitter(program);
                emitter.pattern = emitter.call(cachedFactory, null);
                expansion.expand(emitter, invoke);
                var last = (InvokeInstruction) emitter.instructions.get(emitter.instructions.size() - 1);
                last.setReceiver(invoke.getReceiver());
                for (var instruction : emitter.instructions) {
                    instruction.setLocation(invoke.getLocation());
                }
                invoke.insertPreviousAll(emitter.instructions);
                invoke.delete();
            }
        }
    }

    /**
     * Expands {@code String.split} by a single character (see {@link SplitFastPath#singleChar(String)})
     * into a call of the fast path method, avoiding instantiation of the pattern.
     */
    private boolean tryExpandSplitByChar(Program program, InvokeInstruction invoke, String source) {
        var method = invoke.getMethod();
        if (!method.equals(STRING_SPLIT) && !method.equals(STRING_SPLIT_WITH_LIMIT)) {
            return false;
        }
        int ch = SplitFastPath.singleChar(source);
        if (ch < 0) {
            return false;
        }

        var emitter = new Emitter(program);
        var chVar = emitter.intConstant(ch);
        var limitVar = method.equals(STRING_SPLIT_WITH_LIMIT)
                ? invoke.getArguments().get(1)
                : emitter.intConstant(0);
        emitter.call(STRING_SPLIT_BY_CHAR, invoke.getInstance(), chVar, limitVar);
        var split = (InvokeInstruction) emitter.instructions.get(emitter.instructions.size() - 1);
        split.setReceiver(invoke.getReceiver());
        for (var instruction : emitter.instructions) {
            instruction.setLocation(invoke.getLocation());
        }
        invoke.insertPreviousAll(emitter.instructions);
        invoke.delete();
        return true;
    }

    private static class ClassState {
        final ClassHolder cls;
        final ClassHolderTransformerContext context;
        final Map<PatternKey, MethodReference> factories = new HashMap<>();
        final Map<PatternKey, MethodReference> cachedFactories = new HashMap<>();

        ClassState(ClassHolder cls, ClassHolderTransformerContext context) {
            this.cls = cls;
            this.context = context;
        }

        MethodReference factory(PatternKey key) {
            if (factories.containsKey(key)) {
                return factories.get(key);
            }
            var factory = createFactory(cls, key, context);
            factories.put(key, factory);
            return factory;
        }

        MethodReference cachedFactory(PatternKey key) {
            if (cachedFactories.containsKey(key)) {
                return cachedFactories.get(key);
            }
            var factory = factory(key);
            var cachedFactory = factory != null ? createCachedFactory(cls, factory) : null;
            cachedFactories.put(key, cachedFactory);
            return cachedFactory;
        }
    }

    private static MethodReference createCachedFactory(ClassHolder cls, MethodReference factory) {
        var fieldName = uniqueFieldName(cls, "patternLiteralCache$");
        var field = new FieldHolder(fieldName);
        field.setType(PATTERN_TYPE);
        field.setLevel(AccessLevel.PRIVATE);
        field.getModifiers().add(ElementModifier.STATIC);
        field.getModifiers().add(ElementModifier.SYNTHETIC);
        cls.addField(field);

        var program = new Program();
        program.createVariable();
        var block = program.createBasicBlock();
        var cachedBlock = program.createBasicBlock();
        var createBlock = program.createBasicBlock();

        var get = new GetFieldInstruction();
        get.setField(field.getReference());
        get.setFieldType(PATTERN_TYPE);
        get.setReceiver(program.createVariable());
        block.add(get);
        var branch = new BranchingInstruction(BranchingCondition.NOT_NULL);
        branch.setOperand(get.getReceiver());
        branch.setConsequent(cachedBlock);
        branch.setAlternative(createBlock);
        block.add(branch);

        var exitCached = new ExitInstruction();
        exitCached.setValueToReturn(get.getReceiver());
        cachedBlock.add(exitCached);

        var create = new InvokeInstruction();
        create.setType(InvocationType.SPECIAL);
        create.setMethod(factory);
        create.setReceiver(program.createVariable());
        createBlock.add(create);
        var put = new PutFieldInstruction();
        put.setField(field.getReference());
        put.setFieldType(PATTERN_TYPE);
        put.setValue(create.getReceiver());
        createBlock.add(put);
        var exitCreated = new ExitInstruction();
        exitCreated.setValueToReturn(create.getReceiver());
        createBlock.add(exitCreated);

        var method = new MethodHolder(uniqueMethodName(cls, "cachedPatternLiteral$"), PATTERN_TYPE);
        method.setLevel(AccessLevel.PRIVATE);
        method.getModifiers().add(ElementModifier.STATIC);
        method.getModifiers().add(ElementModifier.SYNTHETIC);
        method.setProgram(program);
        cls.addMethod(method);
        return method.getReference();
    }

    private static String uniqueMethodName(ClassHolder cls, String prefix) {
        int index = 0;
        while (cls.getMethod(new MethodReference(cls.getName(), prefix + index, PATTERN_TYPE).getDescriptor())
                != null) {
            ++index;
        }
        return prefix + index;
    }

    private static String uniqueFieldName(ClassHolder cls, String prefix) {
        int index = 0;
        while (cls.getField(prefix + index) != null) {
            ++index;
        }
        return prefix + index;
    }

    private static MethodReference createFactory(ClassHolder cls, PatternKey key,
            ClassHolderTransformerContext context) {
        // Canonical equivalence relies on Unicode tables, which may differ between compiler's JVM
        // and TeaVM classlib, so leave these patterns to run time compilation.
        // Other Unicode-dependent cases are rejected by TPatternWriter.
        if ((key.flags & Pattern.CANON_EQ) != 0) {
            return null;
        }
        var factoryClass = context.getHierarchy().getClassSource().get(FACTORY_CLASS);
        if (factoryClass == null) {
            return null;
        }
        TPattern pattern;
        try {
            pattern = TPattern.compile(key.source, key.flags);
        } catch (RuntimeException e) {
            // Let pattern throw the same exception in run time
            return null;
        }

        var builder = new PatternProgramBuilder(factoryClass);
        if (!TPatternWriter.write(pattern, builder)) {
            return null;
        }
        var program = builder.complete(pattern);

        var method = new MethodHolder(uniqueMethodName(cls, "patternLiteral$"), PATTERN_TYPE);
        method.setLevel(AccessLevel.PRIVATE);
        method.getModifiers().add(ElementModifier.STATIC);
        method.getModifiers().add(ElementModifier.SYNTHETIC);
        method.setProgram(program);
        cls.addMethod(method);
        return method.getReference();
    }

    private record PatternKey(String source, int flags) {
    }

    private interface Expansion {
        void expand(Emitter emitter, InvokeInstruction invoke);
    }

    private static class Emitter {
        final Program program;
        final List<Instruction> instructions = new ArrayList<>();
        Variable pattern;

        Emitter(Program program) {
            this.program = program;
        }

        Variable call(MethodReference method, Variable instance, Variable... arguments) {
            var insn = new InvokeInstruction();
            insn.setType(instance != null ? InvocationType.VIRTUAL : InvocationType.SPECIAL);
            insn.setMethod(method);
            insn.setInstance(instance);
            insn.setArguments(arguments);
            if (method.getReturnType() != ValueType.VOID) {
                insn.setReceiver(program.createVariable());
            }
            instructions.add(insn);
            return insn.getReceiver();
        }

        Variable intConstant(int value) {
            var insn = new IntegerConstantInstruction();
            insn.setConstant(value);
            insn.setReceiver(program.createVariable());
            instructions.add(insn);
            return insn.getReceiver();
        }
    }
}
