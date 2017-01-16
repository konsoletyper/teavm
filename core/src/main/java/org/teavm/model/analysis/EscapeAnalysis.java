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
package org.teavm.model.analysis;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntDeque;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.teavm.common.DisjointSet;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.model.util.InstructionTransitionExtractor;
import org.teavm.model.util.LivenessAnalyzer;
import org.teavm.model.util.UsageExtractor;

public class EscapeAnalysis {
    private ClassReaderSource classSource;
    private int[] definitionClasses;
    private boolean[] escapingVars;
    private FieldReference[][] fields;

    public EscapeAnalysis(ClassReaderSource classSource) {
        this.classSource = classSource;
    }

    public void analyze(Program program, MethodReference methodReference) {
        InstructionEscapeVisitor visitor = new InstructionEscapeVisitor(program.variableCount(), classSource);
        for (int i = 0; i <= methodReference.parameterCount(); ++i) {
            visitor.escapingVars[i] = true;
        }

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction insn : block) {
                insn.acceptVisitor(visitor);
            }
            if (block.getExceptionVariable() != null) {
                visitor.escapingVars[block.getExceptionVariable().getIndex()] = true;
            }
        }


        definitionClasses = visitor.definitionClasses.pack(program.variableCount());
        escapingVars = new boolean[program.variableCount()];
        for (int i = 0; i < program.variableCount(); ++i) {
            if (visitor.escapingVars[i]) {
                escapingVars[definitionClasses[i]] = true;
            }
        }
        analyzePhis(program);

        fields = packFields(visitor.fields);
    }

    public boolean escapes(int var) {
        return escapingVars[definitionClasses[var]];
    }

    public FieldReference[] getFields(int var) {
        FieldReference[] varFields = fields[definitionClasses[var]];
        return varFields != null ? varFields.clone() : null;
    }

    private void analyzePhis(Program program) {
        LivenessAnalyzer livenessAnalyzer = new LivenessAnalyzer();
        livenessAnalyzer.analyze(program);

        GraphBuilder graphBuilder = new GraphBuilder(program.variableCount());
        IntDeque queue = new IntArrayDeque();
        for (BasicBlock block : program.getBasicBlocks()) {
            IntSet sharedIncomingVars = new IntOpenHashSet();
            BitSet usedVars = getUsedVarsInBlock(livenessAnalyzer, block);
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    int var = incoming.getValue().getIndex();
                    graphBuilder.addEdge(var, phi.getReceiver().getIndex());
                    if (escapingVars[definitionClasses[var]] || !sharedIncomingVars.add(var) || usedVars.get(var)) {
                        queue.addLast(var);
                    }
                }
            }
        }
        Graph graph = graphBuilder.build();

        IntSet visited = new IntOpenHashSet();
        while (!queue.isEmpty()) {
            int var = queue.removeFirst();
            if (visited.add(var)) {
                escapingVars[definitionClasses[var]] = true;
                for (int successor : graph.outgoingEdges(var)) {
                    queue.addLast(successor);
                }
            }
        }
    }

    private BitSet getUsedVarsInBlock(LivenessAnalyzer liveness, BasicBlock block) {
        BitSet usedVars = new BitSet();
        InstructionTransitionExtractor transitionExtractor = new InstructionTransitionExtractor();
        block.getLastInstruction().acceptVisitor(transitionExtractor);
        for (BasicBlock successor : transitionExtractor.getTargets()) {
            usedVars.or(liveness.liveIn(successor.getIndex()));
        }
        for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
            usedVars.or(liveness.liveIn(tryCatch.getHandler().getIndex()));
        }

        UsageExtractor useExtractor = new UsageExtractor();
        for (Instruction instruction : block) {
            instruction.acceptVisitor(useExtractor);
            for (Variable variable : useExtractor.getUsedVariables()) {
                usedVars.set(variable.getIndex());
            }
        }
        return usedVars;
    }

    private FieldReference[][] packFields(List<Set<FieldReference>> fields) {
        List<Set<FieldReference>> joinedFields = new ArrayList<>(Collections.nCopies(fields.size(), null));

        for (int i = 0; i < fields.size(); ++i) {
            if (fields.get(i) == null) {
                continue;
            }
            int j = definitionClasses[i];
            Set<FieldReference> fieldSet = joinedFields.get(j);
            if (fieldSet == null) {
                fieldSet = new LinkedHashSet<>();
                joinedFields.set(j, fieldSet);
            }
            fieldSet.addAll(fields.get(i));
        }

        FieldReference[][] packedFields = new FieldReference[fields.size()][];
        for (int i = 0; i < packedFields.length; ++i) {
            if (joinedFields.get(i) != null) {
                packedFields[i] = joinedFields.get(i).toArray(new FieldReference[0]);
            }
        }
        return packedFields;
    }

    private static class InstructionEscapeVisitor extends AbstractInstructionVisitor {
        ClassReaderSource classSource;
        DisjointSet definitionClasses;
        boolean[] escapingVars;
        List<Set<FieldReference>> fields;

        public InstructionEscapeVisitor(int variableCount, ClassReaderSource classSource) {
            this.classSource = classSource;
            fields = new ArrayList<>(Collections.nCopies(variableCount, null));
            definitionClasses = new DisjointSet();
            for (int i = 0; i < variableCount; ++i) {
                definitionClasses.create();
            }
            escapingVars = new boolean[variableCount];
        }

        @Override
        public void visit(ConstructInstruction insn) {
            ClassReader cls = classSource.get(insn.getType());
            if (cls == null) {
                escapingVars[insn.getReceiver().getIndex()] = true;
            }

            while (cls != null) {
                for (FieldReader field : cls.getFields()) {
                    addField(insn.getReceiver(), field.getReference());
                }
                cls = cls.getParent() != null ? classSource.get(cls.getParent()) : null;
            }
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            escapingVars[insn.getReceiver().getIndex()] = true;
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            escapingVars[insn.getReceiver().getIndex()] = true;
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            escapingVars[insn.getReceiver().getIndex()] = true;
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
            escapingVars[insn.getArray().getIndex()] = true;
            escapingVars[insn.getReceiver().getIndex()] = true;
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            definitionClasses.union(insn.getReceiver().getIndex(), insn.getArray().getIndex());
        }

        @Override
        public void visit(AssignInstruction insn) {
            definitionClasses.union(insn.getReceiver().getIndex(), insn.getAssignee().getIndex());
        }

        @Override
        public void visit(CastInstruction insn) {
            escapingVars[insn.getReceiver().getIndex()] = true;
            escapingVars[insn.getValue().getIndex()] = true;
        }

        @Override
        public void visit(ExitInstruction insn) {
            if (insn.getValueToReturn() != null) {
                escapingVars[insn.getValueToReturn().getIndex()] = true;
            }
        }

        @Override
        public void visit(RaiseInstruction insn) {
            escapingVars[insn.getException().getIndex()] = true;
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            escapingVars[insn.getReceiver().getIndex()] = true;
            addField(insn.getInstance(), insn.getField());
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            escapingVars[insn.getValue().getIndex()] = true;
            addField(insn.getInstance(), insn.getField());
        }

        private void addField(Variable instance, FieldReference field) {
            if (instance == null) {
                return;
            }
            Set<FieldReference> fieldSet = fields.get(instance.getIndex());
            if (fieldSet == null) {
                fieldSet = new LinkedHashSet<>();
                fields.set(instance.getIndex(), fieldSet);
            }
            fieldSet.add(field);
        }

        @Override
        public void visit(GetElementInstruction insn) {
            escapingVars[insn.getReceiver().getIndex()] = true;
        }

        @Override
        public void visit(PutElementInstruction insn) {
            escapingVars[insn.getValue().getIndex()] = true;
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (insn.getInstance() != null) {
                escapingVars[insn.getInstance().getIndex()] = true;
            }
            for (Variable arg : insn.getArguments()) {
                escapingVars[arg.getIndex()] = true;
            }
            if (insn.getReceiver() != null) {
                escapingVars[insn.getReceiver().getIndex()] = true;
            }
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            escapingVars[insn.getValue().getIndex()] = true;
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            definitionClasses.union(insn.getValue().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {
            escapingVars[insn.getObjectRef().getIndex()] = true;
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
            escapingVars[insn.getObjectRef().getIndex()] = true;
        }

        @Override
        public void visit(BranchingInstruction insn) {
            switch (insn.getCondition()) {
                case NULL:
                case NOT_NULL:
                    escapingVars[insn.getOperand().getIndex()] = true;
                    break;
                default:
                    break;
            }
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
            switch (insn.getCondition()) {
                case REFERENCE_EQUAL:
                case REFERENCE_NOT_EQUAL:
                    escapingVars[insn.getFirstOperand().getIndex()] = true;
                    escapingVars[insn.getSecondOperand().getIndex()] = true;
                    break;
                default:
                    break;
            }
        }
    }
}
