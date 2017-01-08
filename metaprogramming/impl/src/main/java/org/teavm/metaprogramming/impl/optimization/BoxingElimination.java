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
package org.teavm.metaprogramming.impl.optimization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.teavm.common.DisjointSet;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.PrimitiveType;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.util.UsageExtractor;

public class BoxingElimination {
    private static Set<String> wrapperClasses = Stream.of(Boolean.class, Byte.class, Short.class,
            Character.class, Integer.class, Long.class, Float.class, Double.class)
            .map(Class::getName)
            .collect(Collectors.toSet());
    private DisjointSet set = new DisjointSet();
    private Program program;
    private List<Wrapper> wrappers = new ArrayList<>();

    public void optimize(Program program) {
        this.program = program;
        initDisjointSet();
        findBoxedValues();
        removeInstructions();
    }

    private void initDisjointSet() {
        for (int i = 0; i < program.variableCount(); ++i) {
            set.create();
        }
    }

    private void findBoxedValues() {
        UsageExtractor use = new UsageExtractor();

        wrappers.addAll(Collections.nCopies(program.variableCount(), null));

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                if (insn instanceof AssignInstruction) {
                    AssignInstruction assign = (AssignInstruction) insn;
                    union(assign.getReceiver().getIndex(), assign.getAssignee().getIndex());
                    continue;
                } else if (insn instanceof CastInstruction) {
                    CastInstruction cast = (CastInstruction) insn;
                    Wrapper targetWrapper = new Wrapper();
                    if (cast.getTargetType() instanceof ValueType.Object) {
                        targetWrapper.expectedType = ((ValueType.Object) cast.getTargetType()).getClassName();
                    } else {
                        targetWrapper.state = WrapperState.REFUTED;
                    }
                    update(cast.getReceiver().getIndex(), targetWrapper);
                    union(cast.getValue().getIndex(), cast.getReceiver().getIndex());
                    continue;
                } else if (insn instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction) insn;
                    MethodReference method = invoke.getMethod();
                    if (wrapperClasses.contains(method.getClassName()) && method.getName().endsWith("Value")
                            && method.parameterCount() == 0 && method.getReturnType() instanceof ValueType.Primitive) {
                        continue;
                    }
                    if (method.getName().equals("valueOf") && wrapperClasses.contains(method.getClassName())
                            && invoke.getReceiver() != null) {
                        Wrapper wrapper = new Wrapper();
                        wrapper.state = WrapperState.PROVEN;
                        update(invoke.getReceiver().getIndex(), wrapper);
                        continue;
                    }
                }

                insn.acceptVisitor(use);
                for (Variable usedVar : use.getUsedVariables()) {
                    Wrapper wrapper = new Wrapper();
                    wrapper.state = WrapperState.REFUTED;
                    update(usedVar.getIndex(), wrapper);
                }
            }

            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    union(phi.getReceiver().getIndex(), incoming.getValue().getIndex());
                }
            }
        }
    }

    private void removeInstructions() {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                if (insn instanceof CastInstruction) {
                    CastInstruction cast = (CastInstruction) insn;
                    if (isProven(cast.getReceiver().getIndex())) {
                        AssignInstruction assign = new AssignInstruction();
                        assign.setReceiver(cast.getReceiver());
                        assign.setAssignee(cast.getValue());
                        insn.replace(assign);
                    }
                } else if (insn instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction) insn;
                    if (invoke.getReceiver() != null && isProven(invoke.getReceiver().getIndex())) {
                        AssignInstruction assign = new AssignInstruction();
                        assign.setReceiver(invoke.getReceiver());
                        assign.setAssignee(invoke.getArguments().get(0));
                        insn.replace(assign);
                    } else if (invoke.getInstance() != null && isProven(invoke.getInstance().getIndex())) {
                        AssignInstruction assign = new AssignInstruction();
                        assign.setReceiver(invoke.getReceiver());
                        assign.setAssignee(invoke.getInstance());
                        insn.replace(assign);
                    }
                }
            }
        }
    }

    private boolean isProven(int index) {
        index = set.find(index);
        Wrapper wrapper = wrappers.get(index);
        return wrapper != null && wrapper.state == WrapperState.PROVEN;
    }

    private int union(int a, int b) {
        Wrapper p = wrappers.get(set.find(a));
        Wrapper q = wrappers.get(set.find(b));
        int c = set.union(a, b);
        if (c >= wrappers.size()) {
            wrappers.addAll(Collections.nCopies(c - wrappers.size() + 1, null));
        }
        wrappers.set(c, union(p, q));
        return c;
    }

    private Wrapper union(Wrapper a, Wrapper b) {
        Wrapper wrapper;
        if (a == null) {
            wrapper = b;
        } else if (b == null) {
            wrapper = a;
        } else {
            if (a.state == WrapperState.REFUTED || b.state == WrapperState.REFUTED) {
                a.state = WrapperState.REFUTED;
            } else if (a.state == WrapperState.PROVEN || b.state == WrapperState.PROVEN) {
                a.state = WrapperState.PROVEN;
            }
            if (a.state != WrapperState.REFUTED) {
                if (a.expectedType == null) {
                    a.expectedType = b.expectedType;
                } else if (b.expectedType != null) {
                    if (isSubtype(b.expectedType, a.expectedType)) {
                        a.expectedType = b.expectedType;
                    } else if (!isSubtype(a.expectedType, b.expectedType)) {
                        a.state = WrapperState.REFUTED;
                    }
                }
            }
            if (a.state != WrapperState.REFUTED) {
                if (a.type == null) {
                    a.type = b.type;
                } else if (b.type != null && b.type != a.type) {
                    a.state = WrapperState.REFUTED;
                }
                if (a.type != null && b.type != null && !isSubtype(a.type, a.expectedType)) {
                    a.state = WrapperState.REFUTED;
                }
            }
            wrapper = a;
        }
        return wrapper;
    }

    private void update(int index, Wrapper wrapper) {
        index = set.find(index);
        wrappers.set(index, union(wrappers.get(index), wrapper));
    }

    private boolean isSubtype(String subtype, String supertype) {
        if (subtype.equals(supertype)) {
            return true;
        }
        switch (supertype) {
            case "java.lang.Object":
                return true;
            case "java.lang.Number":
                switch (subtype) {
                    case "java.lang.Byte":
                    case "java.lang.Short":
                    case "java.lang.Integer":
                    case "java.lang.Long":
                    case "java.lang.Float":
                    case "java.lang.Double":
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    private boolean isSubtype(PrimitiveType subtype, String supertype) {
        switch (supertype) {
            case "java.lang.Object":
                return true;
            case "java.lang.Number":
                switch (subtype) {
                    case BYTE:
                    case SHORT:
                    case INTEGER:
                    case LONG:
                    case FLOAT:
                    case DOUBLE:
                        return true;
                    default:
                        return false;
                }
            default:
                switch (subtype) {
                    case BOOLEAN:
                        return supertype.equals("java.lang.Boolean");
                    case BYTE:
                        return supertype.equals("java.lang.Byte");
                    case SHORT:
                        return supertype.equals("java.lang.Short");
                    case CHARACTER:
                        return supertype.equals("java.lang.Character");
                    case INTEGER:
                        return supertype.equals("java.lang.Integer");
                    case LONG:
                        return supertype.equals("java.lang.Long");
                    case FLOAT:
                        return supertype.equals("java.lang.Float");
                    case DOUBLE:
                        return supertype.equals("java.lang.Double");
                    default:
                        return false;
                }
        }
    }

    static class Wrapper {
        PrimitiveType type;
        String expectedType = "java.lang.Object";
        WrapperState state = WrapperState.UNKNOWN;
    }

    enum WrapperState {
        PROVEN,
        REFUTED,
        UNKNOWN
    }
}
