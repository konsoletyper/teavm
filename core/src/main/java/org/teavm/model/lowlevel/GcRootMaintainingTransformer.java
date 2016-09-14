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
package org.teavm.model.lowlevel;

import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.teavm.common.Graph;
import org.teavm.interop.NoGC;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.LivenessAnalyzer;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.TypeInferer;
import org.teavm.model.util.UsageExtractor;
import org.teavm.model.util.VariableType;
import org.teavm.runtime.Mutator;

public class GcRootMaintainingTransformer {
    private ClassReaderSource classSource;

    public GcRootMaintainingTransformer(ClassReaderSource classSource) {
        this.classSource = classSource;
    }

    public void apply(Program program, MethodReader method) {
        if (!requiresGc(method.getReference())) {
            return;
        }
        List<IntObjectMap<BitSet>> liveInInformation = findCallSiteLiveIns(program, method);
        int maxDepth = putLiveInGcRoots(program, liveInInformation);
        if (maxDepth > 0) {
            addStackAllocation(program, maxDepth);
            addStackRelease(program, maxDepth);
        }
    }

    private List<IntObjectMap<BitSet>> findCallSiteLiveIns(Program program, MethodReader method) {
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        TypeInferer typeInferer = new TypeInferer();
        typeInferer.inferTypes(program, method.getReference());
        List<IntObjectMap<BitSet>> liveInInformation = new ArrayList<>();

        LivenessAnalyzer livenessAnalyzer = new LivenessAnalyzer();
        livenessAnalyzer.analyze(program);
        DefinitionExtractor defExtractor = new DefinitionExtractor();
        UsageExtractor useExtractor = new UsageExtractor();

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            IntObjectMap<BitSet> blockLiveIn = new IntObjectOpenHashMap<>();
            liveInInformation.add(blockLiveIn);
            BitSet currentLiveOut = new BitSet();
            for (int successor : cfg.outgoingEdges(i)) {
                currentLiveOut.or(livenessAnalyzer.liveIn(successor));
            }
            for (int j = block.getInstructions().size() - 1; j >= 0; --j) {
                Instruction insn = block.getInstructions().get(j);
                insn.acceptVisitor(defExtractor);
                insn.acceptVisitor(useExtractor);
                for (Variable usedVar : useExtractor.getUsedVariables()) {
                    currentLiveOut.set(usedVar.getIndex());
                }
                for (Variable definedVar : defExtractor.getDefinedVariables()) {
                    currentLiveOut.clear(definedVar.getIndex());
                }
                if (insn instanceof InvokeInstruction || insn instanceof InitClassInstruction
                        || insn instanceof ConstructInstruction || insn instanceof ConstructArrayInstruction
                        || insn instanceof CloneArrayInstruction || insn instanceof RaiseInstruction) {
                    if (insn instanceof InvokeInstruction && !requiresGc(((InvokeInstruction) insn).getMethod())) {
                        continue;
                    }

                    BitSet csLiveIn = (BitSet) currentLiveOut.clone();
                    for (int v = csLiveIn.nextSetBit(0); v >= 0; v = csLiveIn.nextSetBit(v + 1)) {
                        if (!isReference(typeInferer, v)) {
                            csLiveIn.clear(v);
                        }
                    }
                    csLiveIn.clear(0, method.parameterCount() + 1);
                    blockLiveIn.put(j, csLiveIn);
                }
            }
            if (block.getExceptionVariable() != null) {
                currentLiveOut.clear(block.getExceptionVariable().getIndex());
            }
        }

        return liveInInformation;
    }

    private int putLiveInGcRoots(Program program, List<IntObjectMap<BitSet>> liveInInformation) {
        int maxDepth = 0;
        for (IntObjectMap<BitSet> liveInsMap : liveInInformation) {
            for (ObjectCursor<BitSet> liveIns : liveInsMap.values()) {
                maxDepth = Math.max(maxDepth, liveIns.value.cardinality());
            }
        }

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            List<Instruction> instructions = block.getInstructions();
            IntObjectMap<BitSet> liveInsByIndex = liveInInformation.get(i);
            for (int j = instructions.size() - 1; j >= 0; --j) {
                BitSet liveIns = liveInsByIndex.get(j);
                if (liveIns == null) {
                    continue;
                }
                storeLiveIns(block, j, liveIns, maxDepth);
            }
        }
        return maxDepth;
    }

    private void storeLiveIns(BasicBlock block, int index, BitSet liveIns, int maxDepth) {
        Program program = block.getProgram();
        List<Instruction> instructions = block.getInstructions();
        Instruction callInstruction = instructions.get(index);
        List<Instruction> instructionsToAdd = new ArrayList<>();

        int slot = 0;
        for (int liveVar = liveIns.nextSetBit(0); liveVar >= 0; liveVar = liveIns.nextSetBit(liveVar + 1)) {
            Variable slotVar = program.createVariable();
            IntegerConstantInstruction slotConstant = new IntegerConstantInstruction();
            slotConstant.setReceiver(slotVar);
            slotConstant.setConstant(slot++);
            slotConstant.setLocation(callInstruction.getLocation());
            instructionsToAdd.add(slotConstant);

            InvokeInstruction registerInvocation = new InvokeInstruction();
            registerInvocation.setType(InvocationType.SPECIAL);
            registerInvocation.setMethod(new MethodReference(Mutator.class, "registerGcRoot", int.class,
                    Object.class, void.class));
            registerInvocation.getArguments().add(slotVar);
            registerInvocation.getArguments().add(program.variableAt(liveVar));
            instructionsToAdd.add(registerInvocation);
        }

        while (slot < maxDepth) {
            Variable slotVar = program.createVariable();
            IntegerConstantInstruction slotConstant = new IntegerConstantInstruction();
            slotConstant.setReceiver(slotVar);
            slotConstant.setConstant(slot++);
            slotConstant.setLocation(callInstruction.getLocation());
            instructionsToAdd.add(slotConstant);

            InvokeInstruction clearInvocation = new InvokeInstruction();
            clearInvocation.setType(InvocationType.SPECIAL);
            clearInvocation.setMethod(new MethodReference(Mutator.class, "removeGcRoot", int.class, void.class));
            clearInvocation.getArguments().add(slotVar);
            clearInvocation.setLocation(callInstruction.getLocation());
            instructionsToAdd.add(clearInvocation);

            ++slot;
        }

        instructions.addAll(index, instructionsToAdd);
    }

    private void addStackAllocation(Program program, int maxDepth) {
        BasicBlock block = program.basicBlockAt(0);
        List<Instruction> instructionsToAdd = new ArrayList<>();
        Variable sizeVariable = program.createVariable();

        IntegerConstantInstruction sizeConstant = new IntegerConstantInstruction();
        sizeConstant.setReceiver(sizeVariable);
        sizeConstant.setConstant(maxDepth);
        instructionsToAdd.add(sizeConstant);

        InvokeInstruction invocation = new InvokeInstruction();
        invocation.setType(InvocationType.SPECIAL);
        invocation.setMethod(new MethodReference(Mutator.class, "allocStack", int.class, void.class));
        invocation.getArguments().add(sizeVariable);
        instructionsToAdd.add(invocation);

        block.getInstructions().addAll(0, instructionsToAdd);
    }

    private void addStackRelease(Program program, int maxDepth) {
        List<BasicBlock> blocks = new ArrayList<>();
        boolean hasResult = false;
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            Instruction instruction = block.getLastInstruction();
            if (instruction instanceof ExitInstruction) {
                blocks.add(block);
                if (((ExitInstruction) instruction).getValueToReturn() != null) {
                    hasResult = true;
                }
            }
        }

        BasicBlock exitBlock;
        if (blocks.size() == 1) {
            exitBlock = blocks.get(0);
        } else {
            exitBlock = program.createBasicBlock();
            ExitInstruction exit = new ExitInstruction();
            exitBlock.getInstructions().add(exit);

            if (hasResult) {
                Phi phi = new Phi();
                phi.setReceiver(program.createVariable());
                exitBlock.getPhis().add(phi);
                exit.setValueToReturn(phi.getReceiver());

                for (BasicBlock block : blocks) {
                    ExitInstruction oldExit = (ExitInstruction) block.getLastInstruction();
                    Incoming incoming = new Incoming();
                    incoming.setSource(block);
                    incoming.setValue(oldExit.getValueToReturn());
                    phi.getIncomings().add(incoming);

                    JumpInstruction jumpToExit = new JumpInstruction();
                    jumpToExit.setTarget(exitBlock);
                    jumpToExit.setLocation(oldExit.getLocation());

                    block.getInstructions().set(block.getInstructions().size() - 1, jumpToExit);
                }
            }
        }

        List<Instruction> instructionsToAdd = new ArrayList<>();
        Variable sizeVariable = program.createVariable();

        IntegerConstantInstruction sizeConstant = new IntegerConstantInstruction();
        sizeConstant.setReceiver(sizeVariable);
        sizeConstant.setConstant(maxDepth);
        instructionsToAdd.add(sizeConstant);

        InvokeInstruction invocation = new InvokeInstruction();
        invocation.setType(InvocationType.SPECIAL);
        invocation.setMethod(new MethodReference(Mutator.class, "releaseStack", int.class, void.class));
        invocation.getArguments().add(sizeVariable);
        instructionsToAdd.add(invocation);

        exitBlock.getInstructions().addAll(exitBlock.getInstructions().size() - 1, instructionsToAdd);
    }

    private boolean isReference(TypeInferer typeInferer, int var) {
        VariableType liveType = typeInferer.typeOf(var);
        switch (liveType) {
            case BYTE_ARRAY:
            case CHAR_ARRAY:
            case SHORT_ARRAY:
            case INT_ARRAY:
            case FLOAT_ARRAY:
            case LONG_ARRAY:
            case DOUBLE_ARRAY:
            case OBJECT_ARRAY:
            case OBJECT:
                return true;
            default:
                return false;
        }
    }

    private boolean requiresGc(MethodReference methodReference) {
        ClassReader cls = classSource.get(methodReference.getClassName());
        if (cls == null) {
            return true;
        }
        if (cls.getAnnotations().get(NoGC.class.getName()) != null) {
            return false;
        }
        MethodReader method = cls.getMethod(methodReference.getDescriptor());
        return method.getAnnotations().get(NoGC.class.getName()) == null;
    }
}
