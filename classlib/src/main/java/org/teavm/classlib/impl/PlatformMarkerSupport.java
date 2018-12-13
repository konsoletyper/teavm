/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.classlib.impl;

import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.PlatformMarker;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.MemberReader;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.optimization.ConstantConditionElimination;
import org.teavm.model.optimization.GlobalValueNumbering;
import org.teavm.model.optimization.UnreachableBasicBlockEliminator;

public class PlatformMarkerSupport implements ClassHolderTransformer {
    private String[] tags;

    public PlatformMarkerSupport(String[] tags) {
        this.tags = tags;
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        for (MethodHolder method : cls.getMethods()) {
            if (method.getProgram() != null) {
                transformProgram(method.getReference(), method.getProgram(), context.getHierarchy(),
                        context.getDiagnostics());
            }
        }
    }

    private void transformProgram(MethodReference containingMethod, Program program,
            ClassHierarchy hierarchy, Diagnostics diagnostics) {
        boolean hasChanges = false;

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                Variable receiver;
                MarkerKind kind;
                if (instruction instanceof InvokeInstruction) {
                    MethodReference methodRef = ((InvokeInstruction) instruction).getMethod();
                    MethodReader method = hierarchy.resolve(methodRef);
                    if (method == null) {
                        continue;
                    }
                    kind = isMarker(method);
                    if (kind == null) {
                        continue;
                    }

                    if (!method.hasModifier(ElementModifier.STATIC)) {
                        diagnostics.error(new CallLocation(containingMethod, instruction.getLocation()),
                                "Method '{{m0}}' is marked with '{{c1}}' and should be static",
                                methodRef, PlatformMarker.class.getName());
                    }
                    if (method.getResultType() != ValueType.BOOLEAN) {
                        diagnostics.error(new CallLocation(containingMethod, instruction.getLocation()),
                                "Method '{{m0}}' is marked with '{{c1}}' and should return boolean",
                                methodRef, PlatformMarker.class.getName());
                        continue;
                    }

                    receiver = ((InvokeInstruction) instruction).getReceiver();
                } else if (instruction instanceof GetFieldInstruction) {
                    FieldReference fieldRef = ((GetFieldInstruction) instruction).getField();
                    FieldReader field = hierarchy.resolve(fieldRef);
                    if (field == null) {
                        continue;
                    }
                    kind = isMarker(field);
                    if (kind == null) {
                        continue;
                    }

                    if (!field.hasModifier(ElementModifier.STATIC)) {
                        diagnostics.error(new CallLocation(containingMethod, instruction.getLocation()),
                                "Field '{{f0}}' is marked with '{{c1}}' and should be static",
                                fieldRef, PlatformMarker.class.getName());
                    }
                    if (field.getType() != ValueType.BOOLEAN) {
                        diagnostics.error(new CallLocation(containingMethod, instruction.getLocation()),
                                "Field '{{f0}}' is marked with '{{c1}}' and should be boolean",
                                fieldRef, PlatformMarker.class.getName());
                        continue;
                    }

                    receiver = ((GetFieldInstruction) instruction).getReceiver();
                } else {
                    continue;
                }

                hasChanges = true;
                if (receiver == null) {
                    instruction.delete();
                } else {
                    IntegerConstantInstruction trueResult = new IntegerConstantInstruction();
                    trueResult.setReceiver(receiver);
                    trueResult.setConstant(kind == MarkerKind.TRUE ? 1 : 0);
                    trueResult.setLocation(instruction.getLocation());
                    instruction.replace(trueResult);
                }
            }
        }

        if (hasChanges) {
            boolean changed;
            do {
                changed = new GlobalValueNumbering(true).optimize(program)
                        | new ConstantConditionElimination().optimize(containingMethod.getDescriptor(), program);
                new UnreachableBasicBlockEliminator().optimize(program);
            } while (changed);
        }
    }

    private MarkerKind isMarker(MemberReader member) {
        AnnotationReader annot = member.getAnnotations().get(PlatformMarker.class.getName());
        if (annot == null) {
            return null;
        }
        AnnotationValue value = annot.getValue("value");
        if (value == null) {
            return MarkerKind.TRUE;
        }
        String tagToMatch = value.getString();
        for (String tag : tags) {
            if (tag.equals(tagToMatch)) {
                return MarkerKind.TRUE;
            }
        }
        return MarkerKind.FALSE;
    }

    enum MarkerKind {
        TRUE,
        FALSE
    }
}
