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
package org.teavm.model.optimization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.model.BasicBlock;
import org.teavm.model.FieldReference;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.analysis.EscapeAnalysis;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.util.PhiUpdater;

public class ScalarReplacement implements MethodOptimization {
    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        boolean changed = false;
        while (performOnce(context, program)) {
            changed = true;
        }
        return changed;
    }

    private boolean performOnce(MethodOptimizationContext context, Program program) {
        List<Map<FieldReference, Variable>> fieldMappings = new ArrayList<>(
                Collections.nCopies(program.variableCount(), null));

        MethodReference methodReference = context.getMethod().getReference();
        EscapeAnalysis escapeAnalysis = new EscapeAnalysis();
        escapeAnalysis.analyze(program, methodReference);
        boolean canPerform = false;
        for (int i = 0; i < fieldMappings.size(); ++i) {
            FieldReference[] fields = escapeAnalysis.getFields(i);
            if (!escapeAnalysis.escapes(i) && fields != null) {
                Variable instanceVar = program.variableAt(i);
                Map<FieldReference, Variable> fieldMapping = new LinkedHashMap<>();
                for (FieldReference field : fields) {
                    Variable var = program.createVariable();
                    if (instanceVar.getDebugName() != null) {
                        var.setDebugName(instanceVar.getDebugName() + "$" + field.getFieldName());
                    }
                    if (instanceVar.getLabel() != null) {
                        var.setLabel(instanceVar.getLabel() + "$" + field.getFieldName());
                    }
                    fieldMapping.put(field, var);
                }
                fieldMappings.set(i, fieldMapping);
                canPerform = true;
            }
        }
        if (!canPerform) {
            return false;
        }

        ScalarReplacementVisitor visitor = new ScalarReplacementVisitor(escapeAnalysis, fieldMappings);
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                instruction.acceptVisitor(visitor);
            }
            List<Phi> additionalPhis = new ArrayList<>();
            for (int i = 0; i < block.getPhis().size(); ++i) {
                Phi phi = block.getPhis().get(i);
                if (escapeAnalysis.escapes(phi.getReceiver().getIndex())) {
                    continue;
                }

                FieldReference[] fields = escapeAnalysis.getFields(phi.getReceiver().getIndex());
                if (fields == null) {
                    continue;
                }

                for (FieldReference field : fields) {
                    boolean allIncomingsInitialized = true;
                    for (Incoming incoming : phi.getIncomings()) {
                        if (fieldMappings.get(incoming.getValue().getIndex()).get(field) == null) {
                            allIncomingsInitialized = false;
                        }
                    }

                    if (!allIncomingsInitialized) {
                        continue;
                    }
                    Phi phiReplacement = new Phi();
                    phiReplacement.setReceiver(fieldMappings.get(phi.getReceiver().getIndex()).get(field));

                    for (Incoming incoming : phi.getIncomings()) {
                        Incoming incomingReplacement = new Incoming();
                        incomingReplacement.setSource(incoming.getSource());
                        incomingReplacement.setValue(fieldMappings.get(incoming.getValue().getIndex()).get(field));
                        phiReplacement.getIncomings().add(incomingReplacement);
                    }

                    additionalPhis.add(phiReplacement);
                }
                block.getPhis().remove(i--);
            }
            block.getPhis().addAll(additionalPhis);
        }

        Variable[] arguments = new Variable[methodReference.parameterCount() + 1];
        for (int i = 0; i < arguments.length; ++i) {
            arguments[i] = program.variableAt(i);
        }
        new PhiUpdater().updatePhis(program, arguments);

        return true;
    }

    static class ScalarReplacementVisitor extends AbstractInstructionVisitor {
        private EscapeAnalysis escapeAnalysis;
        private List<Map<FieldReference, Variable>> fieldMappings;

        public ScalarReplacementVisitor(EscapeAnalysis escapeAnalysis,
                List<Map<FieldReference, Variable>> fieldMappings) {
            this.escapeAnalysis = escapeAnalysis;
            this.fieldMappings = fieldMappings;
        }

        @Override
        public void visit(ConstructInstruction insn) {
            int var = insn.getReceiver().getIndex();
            if (!escapeAnalysis.escapes(var) && escapeAnalysis.getFields(var) != null) {
                for (FieldReference fieldRef : escapeAnalysis.getFields(var)) {
                    ValueType fieldType = escapeAnalysis.getFieldType(fieldRef);
                    Variable receiver = fieldMappings.get(insn.getReceiver().getIndex()).get(fieldRef);
                    Instruction initializer = generateDefaultValue(fieldType, receiver);
                    initializer.setLocation(initializer.getLocation());
                    insn.insertPrevious(initializer);
                }
                insn.delete();
            }
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            if (insn.getInstance() != null && !escapeAnalysis.escapes(insn.getInstance().getIndex())) {
                Variable var = fieldMappings.get(insn.getInstance().getIndex()).get(insn.getField());
                AssignInstruction assignment = new AssignInstruction();
                assignment.setReceiver(insn.getReceiver());
                assignment.setAssignee(var);
                assignment.setLocation(insn.getLocation());
                insn.replace(assignment);
            }
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            if (insn.getInstance() != null && !escapeAnalysis.escapes(insn.getInstance().getIndex())) {
                Variable var = fieldMappings.get(insn.getInstance().getIndex()).get(insn.getField());
                AssignInstruction assignment = new AssignInstruction();
                assignment.setReceiver(var);
                assignment.setAssignee(insn.getValue());
                assignment.setLocation(insn.getLocation());
                insn.replace(assignment);
            }
        }

        @Override
        public void visit(AssignInstruction insn) {
            if (escapeAnalysis.escapes(insn.getAssignee().getIndex())) {
                return;
            }

            FieldReference[] fields = escapeAnalysis.getFields(insn.getReceiver().getIndex());
            if (fields == null) {
                return;
            }
            for (FieldReference field : fields) {
                Variable assignee = fieldMappings.get(insn.getAssignee().getIndex()).get(field);
                if (assignee == null) {
                    continue;
                }
                Variable receiver = fieldMappings.get(insn.getReceiver().getIndex()).get(field);
                AssignInstruction assignment = new AssignInstruction();
                assignment.setReceiver(receiver);
                assignment.setAssignee(assignee);
                assignment.setLocation(insn.getLocation());
                insn.insertPrevious(assignment);
            }
            insn.delete();
        }

        private Instruction generateDefaultValue(ValueType type, Variable receiver) {
            if (type instanceof ValueType.Primitive) {
                switch (((ValueType.Primitive) type).getKind()) {
                    case BOOLEAN:
                    case BYTE:
                    case SHORT:
                    case CHARACTER:
                    case INTEGER: {
                        IntegerConstantInstruction insn = new IntegerConstantInstruction();
                        insn.setReceiver(receiver);
                        return insn;
                    }
                    case LONG: {
                        LongConstantInstruction insn = new LongConstantInstruction();
                        insn.setReceiver(receiver);
                        return insn;
                    }
                    case FLOAT: {
                        FloatConstantInstruction insn = new FloatConstantInstruction();
                        insn.setReceiver(receiver);
                        return insn;
                    }
                    case DOUBLE: {
                        DoubleConstantInstruction insn = new DoubleConstantInstruction();
                        insn.setReceiver(receiver);
                        return insn;
                    }
                }
            }
            NullConstantInstruction insn = new NullConstantInstruction();
            insn.setReceiver(receiver);
            return insn;
        }
    }
}
