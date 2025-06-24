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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

public class Devirtualization {
    static final boolean shouldLog = System.getProperty("org.teavm.logDevirtualization", "false").equals("true");
    private DependencyInfo dependency;
    private ClassHierarchy hierarchy;
    private Set<MethodReference> virtualMethods = new HashSet<>();
    private Set<? extends MethodReference> readonlyVirtualMethods = Collections.unmodifiableSet(virtualMethods);
    private Map<ValueDependencyInfo, Map<MethodReference, Set<MethodReference>>> implementationCache =
            new HashMap<>();
    private Map<ValueDependencyInfo, Map<ValueType, Optional<ValueType>>> castCache = new HashMap<>();
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
                    applyToInvoke(methodDep, program, (InvokeInstruction) insn);
                } else if (insn instanceof CastInstruction) {
                    applyToCast(methodDep, (CastInstruction) insn);
                }
            }
        }

        if (shouldLog) {
            System.out.println("DEVIRTUALIZATION complete for " + method.getReference());
        }
    }

    private void applyToInvoke(MethodDependencyInfo methodDep, Program program, InvokeInstruction invoke) {
        if (invoke.getType() != InvocationType.VIRTUAL) {
            return;
        }
        ValueDependencyInfo var = methodDep.getVariable(invoke.getInstance().getIndex());
        Set<MethodReference> implementations = getImplementations(var, invoke.getMethod());
        if (implementations.size() == 1) {
            MethodReference resolvedImplementation = implementations.iterator().next();
            if (shouldLog) {
                System.out.print("DIRECT CALL " + invoke.getMethod() + " resolved to "
                        + resolvedImplementation.getClassName());
                if (invoke.getLocation() != null) {
                    System.out.print(" at " + invoke.getLocation().getFileName() + ":"
                            + invoke.getLocation().getLine());
                }
                System.out.println();
            }
            if (!resolvedImplementation.getClassName().equals(invoke.getMethod().getClassName())) {
                var cast = new CastInstruction();
                cast.setValue(invoke.getInstance());
                cast.setTargetType(ValueType.object(resolvedImplementation.getClassName()));
                cast.setWeak(true);
                cast.setReceiver(program.createVariable());
                cast.setLocation(invoke.getLocation());
                invoke.insertPrevious(cast);
                invoke.setInstance(cast.getReceiver());
            }
            invoke.setType(InvocationType.SPECIAL);
            invoke.setMethod(resolvedImplementation);
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
        var failType = getCastFailType(var, cast.getTargetType());

        if (failType != null) {
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
            cast.setWeak(true);
            eliminatedCasts++;
        }
    }

    private ValueType getCastFailType(ValueDependencyInfo node, ValueType targetType) {
        if (dependency.isPrecise()) {
            return computeCastFailType(node, targetType);
        } else {
            var byType = castCache.computeIfAbsent(node, n -> new HashMap<>());
            return byType.computeIfAbsent(targetType, t -> Optional.ofNullable(computeCastFailType(node, t)))
                    .orElse(null);
        }
    }

    private ValueType computeCastFailType(ValueDependencyInfo node, ValueType targetType) {
        for (var type : node.getTypes()) {
            if (castCanFail(type, targetType)) {
                return type;
            }
        }
        return null;
    }

    private boolean castCanFail(ValueType type, ValueType target) {
        return !hierarchy.isSuperType(target, type, false);
    }

    private Set<MethodReference> getImplementations(ValueDependencyInfo value, MethodReference ref) {
        if (dependency.isPrecise()) {
            return implementations(hierarchy, dependency, value.getTypes(), ref);
        } else {
            var map = implementationCache.computeIfAbsent(value, v -> new HashMap<>());
            return map.computeIfAbsent(ref, m -> implementations(hierarchy, dependency, value.getTypes(), m));
        }
    }

    public static Set<MethodReference> implementations(ClassHierarchy hierarchy, DependencyInfo dependency,
            ValueType[] types, MethodReference ref) {
        var isSuperclass = hierarchy.getSuperclassPredicate(ref.getClassName());
        Set<MethodReference> methods = new LinkedHashSet<>();
        var arrayEncountered = false;
        for (var type : types) {
            String className;
            if (type instanceof ValueType.Array) {
                if (arrayEncountered) {
                    continue;
                }
                arrayEncountered = true;
                className = "java.lang.Object";
            } else if (type instanceof ValueType.Object) {
                className = ((ValueType.Object) type).getClassName();
            } else {
                continue;
            }
            if (!isSuperclass.test(ValueType.object(className), false)) {
                continue;
            }
            ClassReader cls = hierarchy.getClassSource().get(className);
            if (cls == null) {
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
