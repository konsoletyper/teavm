/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.model.optimization;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.teavm.common.OptionalPredicate;
import org.teavm.dependency.DependencyInfo;
import org.teavm.dependency.MethodDependencyInfo;
import org.teavm.dependency.ValueDependencyInfo;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

public class Devirtualization {
    private DependencyInfo dependency;
    private ClassHierarchy hierarchy;
    private Set<MethodReference> virtualMethods = new HashSet<>();
    private Set<? extends MethodReference> readonlyVirtualMethods = Collections.unmodifiableSet(virtualMethods);

    public Devirtualization(DependencyInfo dependency, ClassHierarchy hierarchy) {
        this.dependency = dependency;
        this.hierarchy = hierarchy;
    }

    public void apply(MethodHolder method) {
        MethodDependencyInfo methodDep = dependency.getMethod(method.getReference());
        if (methodDep == null) {
            return;
        }
        Program program = method.getProgram();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                if (!(insn instanceof InvokeInstruction)) {
                    continue;
                }
                InvokeInstruction invoke = (InvokeInstruction) insn;
                if (invoke.getType() != InvocationType.VIRTUAL) {
                    continue;
                }
                ValueDependencyInfo var = methodDep.getVariable(invoke.getInstance().getIndex());
                Set<MethodReference> implementations = getImplementations(var.getTypes(),
                        invoke.getMethod());
                if (implementations.size() == 1) {
                    invoke.setType(InvocationType.SPECIAL);
                    invoke.setMethod(implementations.iterator().next());
                } else {
                    virtualMethods.addAll(implementations);
                }
            }
        }
    }

    private Set<MethodReference> getImplementations(String[] classNames, MethodReference ref) {
        return implementations(hierarchy, dependency, classNames, ref);
    }

    public static Set<MethodReference> implementations(ClassHierarchy hierarchy, DependencyInfo dependency,
            String[] classNames, MethodReference ref) {
        OptionalPredicate<String> isSuperclass = hierarchy.getSuperclassPredicate(ref.getClassName());
        Set<MethodReference> methods = new LinkedHashSet<>();
        for (String className : classNames) {
            if (className.startsWith("[")) {
                className = "java.lang.Object";
            }
            ClassReader cls = hierarchy.getClassSource().get(className);
            if (cls == null || !isSuperclass.test(cls.getName(), false)) {
                continue;
            }
            MethodDependencyInfo methodDep = dependency.getMethodImplementation(new MethodReference(
                    className, ref.getDescriptor()));
            if (methodDep != null) {
                methods.add(methodDep.getReference());
            }
        }
        return methods;
    }

    public Set<? extends MethodReference> getVirtualMethods() {
        return readonlyVirtualMethods;
    }
}
