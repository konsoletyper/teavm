/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.dependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.teavm.cache.IncrementalDependencyRegistration;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.optimization.UnreachableBasicBlockEliminator;
import org.teavm.model.transformation.ClassInitInsertion;
import org.teavm.model.util.BasicBlockSplitter;
import org.teavm.model.util.ModelUtils;

class DependencyClassSource implements ClassHolderSource {
    private DependencyAgent agent;
    private ClassReaderSource innerSource;
    ClassHierarchy innerHierarchy;
    private Diagnostics diagnostics;
    private IncrementalDependencyRegistration dependencyRegistration;
    private Map<String, ClassHolder> generatedClasses = new LinkedHashMap<>();
    private List<ClassHolderTransformer> transformers = new ArrayList<>();
    boolean obfuscated;
    boolean strict;
    Map<String, Optional<ClassHolder>> cache = new LinkedHashMap<>(1000, 0.5f);
    private ReferenceResolver referenceResolver;
    private ClassInitInsertion classInitInsertion;
    private String entryPoint;
    Map<MethodReference, BootstrapMethodSubstitutor> bootstrapMethodSubstitutors = new HashMap<>();
    private boolean disposed;

    DependencyClassSource(DependencyAgent agent, ClassReaderSource innerSource, Diagnostics diagnostics,
            IncrementalDependencyRegistration dependencyRegistration, String[] platformTags) {
        this.agent = agent;
        this.innerSource = innerSource;
        this.diagnostics = diagnostics;
        innerHierarchy = new ClassHierarchy(innerSource);
        this.dependencyRegistration = dependencyRegistration;
        referenceResolver = new ReferenceResolver(this, platformTags);
        classInitInsertion = new ClassInitInsertion(this);
    }

    public ReferenceResolver getReferenceResolver() {
        return referenceResolver;
    }

    @Override
    public ClassHolder get(String name) {
        var result = cache.get(name);
        if (result == null) {
            var cls = findClass(name);
            result = Optional.ofNullable(cls);
            cache.put(name, result);
            if (cls != null) {
                transformClass(cls);
            }
        }
        return result.orElse(null);
    }

    public void submit(ClassHolder cls) {
        if (innerSource.get(cls.getName()) != null || generatedClasses.containsKey(cls.getName())) {
            throw new IllegalArgumentException("Class " + cls.getName() + " is already defined");
        }
        if (!transformers.isEmpty()) {
            cls = ModelUtils.copyClass(cls);
        }
        generatedClasses.put(cls.getName(), cls);
        for (MethodHolder method : cls.getMethods()) {
            if (method.getProgram() != null && method.getProgram().basicBlockCount() > 0) {
                new UnreachableBasicBlockEliminator().optimize(method.getProgram());
            }
        }
        cache.remove(cls.getName());
    }

    private void transformClass(ClassHolder cls) {
        if (!disposed) {
            for (var method : cls.getMethods()) {
                processInvokeDynamic(method);
            }
        }
        if (!transformers.isEmpty()) {
            for (var transformer : transformers) {
                transformer.transformClass(cls, transformContext);
            }
        }
        for (var method : cls.getMethods()) {
            if (method.getProgram() != null) {
                var program = method.getProgram();
                method.setProgramSupplier(m -> {
                    referenceResolver.resolve(m, program);
                    classInitInsertion.apply(m, program);
                    return program;
                });
            }
        }
    }

    private ClassHolder findClass(String name) {
        ClassReader cls = innerSource.get(name);
        if (cls != null) {
            return ModelUtils.copyClass(cls);
        }
        return generatedClasses.get(name);
    }

    Collection<String> getGeneratedClassNames() {
        return generatedClasses.keySet();
    }

    public boolean isGeneratedClass(String className) {
        return generatedClasses.containsKey(className);
    }

    public void addTransformer(ClassHolderTransformer transformer) {
        transformers.add(transformer);
    }

    void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public void dispose() {
        transformers.clear();
        bootstrapMethodSubstitutors.clear();
        disposed = true;
    }

    private void processInvokeDynamic(MethodHolder method) {
        Program program = method.getProgram();
        if (program == null) {
            return;
        }

        ProgramEmitter pe = ProgramEmitter.create(program, innerHierarchy);
        BasicBlockSplitter splitter = new BasicBlockSplitter(program);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                if (!(insn instanceof InvokeDynamicInstruction)) {
                    continue;
                }
                block = insn.getBasicBlock();

                InvokeDynamicInstruction indy = (InvokeDynamicInstruction) insn;
                MethodReference bootstrapMethod = new MethodReference(indy.getBootstrapMethod().getClassName(),
                        indy.getBootstrapMethod().getName(), indy.getBootstrapMethod().signature());
                BootstrapMethodSubstitutor substitutor = bootstrapMethodSubstitutors.get(bootstrapMethod);
                if (substitutor == null) {
                    NullConstantInstruction nullInsn = new NullConstantInstruction();
                    nullInsn.setReceiver(indy.getReceiver());
                    nullInsn.setLocation(indy.getLocation());
                    insn.replace(nullInsn);
                    CallLocation location = new CallLocation(method.getReference(), insn.getLocation());
                    diagnostics.error(location, "Substitutor for bootstrap method {{m0}} was not found",
                            bootstrapMethod);
                    continue;
                }

                BasicBlock splitBlock = splitter.split(block, insn);

                pe.enter(block);
                pe.setCurrentLocation(indy.getLocation());
                insn.delete();

                List<ValueEmitter> arguments = new ArrayList<>();
                for (int k = 0; k < indy.getArguments().size(); ++k) {
                    arguments.add(pe.var(indy.getArguments().get(k), indy.getMethod().parameterType(k)));
                }
                DynamicCallSite callSite = new DynamicCallSite(
                        method.getReference(), indy.getMethod(),
                        indy.getInstance() != null ? pe.var(indy.getInstance(),
                                ValueType.object(method.getOwnerName())) : null,
                        arguments, indy.getBootstrapMethod(), indy.getBootstrapArguments(),
                        agent);
                ValueEmitter result = substitutor.substitute(callSite, pe);
                if (result.getVariable() != null && result.getVariable() != indy.getReceiver()
                        && indy.getReceiver() != null) {
                    AssignInstruction assign = new AssignInstruction();
                    assign.setAssignee(result.getVariable());
                    assign.setReceiver(indy.getReceiver());
                    pe.addInstruction(assign);
                }
                pe.jump(splitBlock);
            }
        }
        splitter.fixProgram();
    }

    final ClassHolderTransformerContext transformContext = new ClassHolderTransformerContext() {
        @Override
        public ClassHierarchy getHierarchy() {
            return innerHierarchy;
        }

        @Override
        public Diagnostics getDiagnostics() {
            return diagnostics;
        }

        @Override
        public IncrementalDependencyRegistration getIncrementalCache() {
            return dependencyRegistration;
        }

        @Override
        public boolean isObfuscated() {
            return obfuscated;
        }

        @Override
        public boolean isStrict() {
            return strict;
        }

        @Override
        public void submit(ClassHolder cls) {
            DependencyClassSource.this.submit(cls);
        }

        @Override
        public String getEntryPoint() {
            return entryPoint;
        }
    };
}
