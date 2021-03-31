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
import org.teavm.model.ValueType;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

public class Devirtualization {
    static final boolean shouldLog = System.getProperty("org.teavm.logDevirtualization", "false").equals("true");
    private DependencyInfo dependency;
    private ClassHierarchy hierarchy;
    private Set<MethodReference> virtualMethods = new HashSet<>();
    private Set<? extends MethodReference> readonlyVirtualMethods = Collections.unmodifiableSet(virtualMethods);
    private int virtualCallSites;
    private int directCallSites;
    private int remainingCasts;
    private int eliminatedCasts;

    public Devirtualization(DependencyInfo dependency, ClassHierarchy hierarchy) {
        this.dependency = dependency;
        this.hierarchy = hierarchy;
    }

    public int getVirtualCallSites() {
        return virtualCallSites;
    }

    public int getDirectCallSites() {
        return directCallSites;
    }

    public int getRemainingCasts() {
        return remainingCasts;
    }

    public int getEliminatedCasts() {
        return eliminatedCasts;
    }

    public void apply(MethodHolder method) {
        MethodDependencyInfo methodDep = dependency.getMethod(method.getReference());
        if (methodDep == null) {
            return;
        }
        Program program = method.getProgram();

        if (shouldLog) {
            System.out.println("DEVIRTUALIZATION running at " + method.getReference());
        }

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                if (insn instanceof InvokeInstruction) {
                    applyToInvoke(methodDep, (InvokeInstruction) insn);
                } else if (insn instanceof CastInstruction) {
                    applyToCast(methodDep, (CastInstruction) insn);
                }
            }
        }

        if (shouldLog) {
            System.out.println("DEVIRTUALIZATION complete for " + method.getReference());
        }
    }

    private void applyToInvoke(MethodDependencyInfo methodDep, InvokeInstruction invoke) {
        if (invoke.getType() != InvocationType.VIRTUAL) {
            return;
        }
        ValueDependencyInfo var = methodDep.getVariable(invoke.getInstance().getIndex());
        Set<MethodReference> implementations = getImplementations(var.getTypes(),
                invoke.getMethod());
        if (implementations.size() == 1) {
            MethodReference resolvedImplementaiton = implementations.iterator().next();
            if (shouldLog) {
                System.out.print("DIRECT CALL " + invoke.getMethod() + " resolved to "
                        + resolvedImplementaiton.getClassName());
                if (invoke.getLocation() != null) {
                    System.out.print(" at " + invoke.getLocation().getFileName() + ":"
                            + invoke.getLocation().getLine());
                }
                System.out.println();
            }
            invoke.setType(InvocationType.SPECIAL);
            invoke.setMethod(resolvedImplementaiton);
            directCallSites++;
        } else {
            virtualMethods.addAll(implementations);
            if (shouldLog) {
                System.out.print("VIRTUAL CALL " + invoke.getMethod() + " resolved to [");
                boolean first = true;
                for (MethodReference impl : implementations) {
                    if (!first) {
                        System.out.print(", ");
                    }
                    first = false;
                    System.out.print(impl.getClassName());
                }
                System.out.print("]");
                if (invoke.getLocation() != null) {
                    System.out.print(" at " + invoke.getLocation().getFileName() + ":"
                            + invoke.getLocation().getLine());
                }
                System.out.println();
            }
            virtualCallSites++;
        }
    }

    private void applyToCast(MethodDependencyInfo methodDep, CastInstruction cast) {
        ValueDependencyInfo var = methodDep.getVariable(cast.getValue().getIndex());
        if (var == null) {
            return;
        }
        boolean canFail = false;
        String failType = null;
        for (String type : var.getTypes()) {
            if (castCanFail(type, cast.getTargetType())) {
                failType = type;
                canFail = true;
            }
        }

        if (canFail) {
            if (shouldLog) {
                System.out.print("REMAINING CAST to " + cast.getTargetType() + " (example is " + failType + ")");
                if (cast.getLocation() != null) {
                    System.out.print(" at " + cast.getLocation().getFileName() + ":"
                            + cast.getLocation().getLine());
                }
                System.out.println();
            }
            remainingCasts++;
        } else {
            if (shouldLog) {
                System.out.print("ELIMINATED CAST to " + cast.getTargetType());
                if (cast.getLocation() != null) {
                    System.out.print(" at " + cast.getLocation().getFileName() + ":"
                            + cast.getLocation().getLine());
                }
                System.out.println();
            }
            AssignInstruction assign = new AssignInstruction();
            assign.setAssignee(cast.getValue());
            assign.setReceiver(cast.getReceiver());
            assign.setLocation(cast.getLocation());
            cast.replace(assign);
            eliminatedCasts++;
        }
    }

    private boolean castCanFail(String type, ValueType target) {
        if (type.startsWith("[")) {
            ValueType valueType = ValueType.parse(type);
            if (hierarchy.isSuperType(target, valueType, false)) {
                return false;
            }
        } else if (target instanceof ValueType.Object) {
            String targetClassName = ((ValueType.Object) target).getClassName();
            if (hierarchy.isSuperType(targetClassName, type, false)) {
                return false;
            }
        }
        return true;
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
