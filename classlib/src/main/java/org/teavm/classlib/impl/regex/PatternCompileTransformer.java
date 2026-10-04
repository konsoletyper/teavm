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
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

/**
 * Replaces calls to {@link Pattern#compile(String)} and {@link Pattern#compile(String, int)} with constant
 * arguments by code that directly constructs the compiled pattern. The pattern is compiled at build time
 * by running classlib's regex engine in the compiler's JVM, and the resulting object graph is turned into
 * calls to factory methods by {@link TPatternWriter} and {@link PatternProgramBuilder}.
 */
public class PatternCompileTransformer implements ClassHolderTransformer {
    private static final MethodReference COMPILE = new MethodReference(Pattern.class, "compile",
            String.class, Pattern.class);
    private static final MethodReference COMPILE_WITH_FLAGS = new MethodReference(Pattern.class, "compile",
            String.class, int.class, Pattern.class);
    private static final String FACTORY_CLASS = "java.util.regex.PatternFactory";

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        var factories = new HashMap<PatternKey, MethodReference>();
        for (var method : List.copyOf(cls.getMethods())) {
            var program = method.getProgram();
            if (program != null && hasCompileCalls(program)) {
                transformProgram(cls, program, factories, context);
            }
        }
    }

    private boolean hasCompileCalls(Program program) {
        for (var block : program.getBasicBlocks()) {
            for (var instruction : block) {
                if (instruction instanceof InvokeInstruction && isCompile((InvokeInstruction) instruction)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCompile(InvokeInstruction invoke) {
        return invoke.getMethod().equals(COMPILE) || invoke.getMethod().equals(COMPILE_WITH_FLAGS);
    }

    private void transformProgram(ClassHolder cls, Program program, Map<PatternKey, MethodReference> factories,
            ClassHolderTransformerContext context) {
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
                    if (isCompile(invoke)) {
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
            if (args.size() > 1) {
                var flagsConstant = intConstantsByClass[varSet.find(args.get(1).getIndex())];
                if (flagsConstant == null) {
                    continue;
                }
                flags = flagsConstant;
            }

            var key = new PatternKey(source, flags);
            MethodReference factory;
            if (factories.containsKey(key)) {
                factory = factories.get(key);
            } else {
                factory = createFactory(cls, key, context);
                factories.put(key, factory);
            }
            if (factory == null) {
                continue;
            }

            var replacement = new InvokeInstruction();
            replacement.setType(InvocationType.SPECIAL);
            replacement.setMethod(factory);
            replacement.setReceiver(invoke.getReceiver());
            replacement.setLocation(invoke.getLocation());
            invoke.replace(replacement);
        }
    }

    private MethodReference createFactory(ClassHolder cls, PatternKey key, ClassHolderTransformerContext context) {
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

        var name = "patternLiteral$";
        int index = 0;
        while (cls.getMethod(new MethodReference(cls.getName(), name + index,
                ValueType.object(Pattern.class.getName())).getDescriptor()) != null) {
            ++index;
        }
        var method = new MethodHolder(name + index, ValueType.object(Pattern.class.getName()));
        method.setLevel(AccessLevel.PRIVATE);
        method.getModifiers().add(ElementModifier.STATIC);
        method.getModifiers().add(ElementModifier.SYNTHETIC);
        method.setProgram(program);
        cls.addMethod(method);
        return method.getReference();
    }

    private record PatternKey(String source, int flags) {
    }
}
