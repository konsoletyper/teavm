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

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.BinaryBranchingCondition;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.SwitchTableEntry;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.ProgramUtils;
import org.teavm.runtime.ExceptionHandling;
import org.teavm.runtime.ShadowStack;

public class ExceptionHandlingShadowStackContributor {
    private Characteristics characteristics;
    private List<CallSiteDescriptor> callSites;
    private BasicBlock defaultExceptionHandler;
    private MethodReference method;
    private Program program;
    private DominatorTree dom;
    private BasicBlock[] variableDefinitionPlaces;
    private boolean hasExceptionHandlers;
    private int parameterCount;

    public ExceptionHandlingShadowStackContributor(Characteristics characteristics,
            List<CallSiteDescriptor> callSites, MethodReference method, Program program) {
        this.characteristics = characteristics;
        this.callSites = callSites;
        this.method = method;
        this.program = program;

        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        dom = GraphUtils.buildDominatorTree(cfg);
        variableDefinitionPlaces = ProgramUtils.getVariableDefinitionPlaces(program);
        parameterCount = method.parameterCount() + 1;
    }

    public boolean contribute() {
        int[] blockMapping = new int[program.basicBlockCount()];
        for (int i = 0; i < blockMapping.length; ++i) {
            blockMapping[i] = i;
        }

        List<Phi> allPhis = new ArrayList<>();
        int blockCount = program.basicBlockCount();
        for (int i = 0; i < blockCount; ++i) {
            allPhis.addAll(program.basicBlockAt(i).getPhis());
        }

        Set<BasicBlock> exceptionHandlers = new HashSet<>();
        for (int i = 0; i < blockCount; ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                exceptionHandlers.add(tryCatch.getHandler());
            }

            if (block.getExceptionVariable() != null) {
                InvokeInstruction catchCall = new InvokeInstruction();
                catchCall.setType(InvocationType.SPECIAL);
                catchCall.setMethod(new MethodReference(ExceptionHandling.class, "catchException",
                        Throwable.class));
                catchCall.setReceiver(block.getExceptionVariable());
                block.addFirst(catchCall);
                block.setExceptionVariable(null);
            }

            int newIndex = contributeToBasicBlock(block);
            if (newIndex != i) {
                blockMapping[i] = newIndex;
                hasExceptionHandlers = true;
            }
        }

        for (Phi phi : allPhis) {
            if (!exceptionHandlers.contains(phi.getBasicBlock())) {
                for (Incoming incoming : phi.getIncomings()) {
                    int mappedSource = blockMapping[incoming.getSource().getIndex()];
                    incoming.setSource(program.basicBlockAt(mappedSource));
                }
            }
        }

        return hasExceptionHandlers;
    }

    private int contributeToBasicBlock(BasicBlock block) {
        int[] currentJointSources = new int[program.variableCount()];
        int[] jointReceiverMap = new int[program.variableCount()];
        Arrays.fill(currentJointSources, -1);
        Arrays.fill(jointReceiverMap, -1);
        IntSet outgoingVariablesToRemove = new IntHashSet();
        IntSet variablesDefinedHere = new IntHashSet();

        for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
            for (Phi phi : tryCatch.getHandler().getPhis()) {
                List<Variable> sourceVariables = phi.getIncomings().stream()
                        .filter(incoming -> incoming.getSource() == tryCatch.getProtectedBlock())
                        .map(incoming -> incoming.getValue())
                        .collect(Collectors.toList());
                if (sourceVariables.isEmpty()) {
                    continue;
                }

                for (Variable sourceVar : sourceVariables) {
                    BasicBlock sourceVarDefinedAt = variableDefinitionPlaces[sourceVar.getIndex()];
                    if (sourceVar.getIndex() < parameterCount
                            || dom.dominates(sourceVarDefinedAt.getIndex(), block.getIndex())) {
                        currentJointSources[phi.getReceiver().getIndex()] = sourceVar.getIndex();
                        break;
                    }
                }
                for (Variable sourceVar : sourceVariables) {
                    jointReceiverMap[sourceVar.getIndex()] = phi.getReceiver().getIndex();
                }
            }
        }

        DefinitionExtractor defExtractor = new DefinitionExtractor();
        List<BasicBlock> blocksToClearHandlers = new ArrayList<>();
        blocksToClearHandlers.add(block);
        BasicBlock initialBlock = block;

        for (Instruction insn : block) {
            if (isCallInstruction(insn)) {
                BasicBlock next;
                boolean last = false;
                if (isSpecialCallInstruction(insn)) {
                    next = null;
                    while (insn.getNext() != null) {
                        Instruction nextInsn = insn.getNext();
                        nextInsn.delete();
                    }
                    last = true;
                } else if (insn instanceof RaiseInstruction) {
                    InvokeInstruction raise = new InvokeInstruction();
                    raise.setMethod(new MethodReference(ExceptionHandling.class, "throwException", Throwable.class,
                            void.class));
                    raise.setType(InvocationType.SPECIAL);
                    raise.setArguments(((RaiseInstruction) insn).getException());
                    raise.setLocation(insn.getLocation());
                    insn.replace(raise);
                    insn = raise;
                    next = null;
                } else if (insn.getNext() != null && insn.getNext() instanceof JumpInstruction) {
                    next = ((JumpInstruction) insn.getNext()).getTarget();
                    insn.getNext().delete();
                    last = true;
                } else {
                    next = program.createBasicBlock();
                    next.getTryCatchBlocks().addAll(ProgramUtils.copyTryCatches(block, program));
                    blocksToClearHandlers.add(next);

                    while (insn.getNext() != null) {
                        Instruction nextInsn = insn.getNext();
                        nextInsn.delete();
                        next.add(nextInsn);
                    }
                }

                String fileName = insn.getLocation() != null ? insn.getLocation().getFileName() : null;
                int lineNumber = insn.getLocation() != null ? insn.getLocation().getLine() : -1;
                if (fileName != null) {
                    fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                }
                CallSiteLocation location = new CallSiteLocation(fileName, method.getClassName(), method.getName(),
                        lineNumber);
                CallSiteDescriptor callSite = new CallSiteDescriptor(callSites.size(), location);
                callSites.add(callSite);
                List<Instruction> pre = setLocation(getInstructionsBeforeCallSite(callSite), insn.getLocation());
                List<Instruction> post = getInstructionsAfterCallSite(initialBlock, block, next, callSite,
                        currentJointSources, outgoingVariablesToRemove, variablesDefinedHere);
                post = setLocation(post, insn.getLocation());
                block.getLastInstruction().insertPreviousAll(pre);
                block.addAll(post);
                hasExceptionHandlers = true;

                if (next == null || last) {
                    break;
                }
                block = next;
                outgoingVariablesToRemove.clear();
                variablesDefinedHere.clear();
            }

            insn.acceptVisitor(defExtractor);
            for (Variable definedVar : defExtractor.getDefinedVariables()) {
                int jointReceiver = jointReceiverMap[definedVar.getIndex()];
                if (jointReceiver >= 0) {
                    int formerVar = currentJointSources[jointReceiver];
                    if (formerVar >= 0) {
                        if (variableDefinitionPlaces[formerVar] == initialBlock) {
                            outgoingVariablesToRemove.add(formerVar);
                        }
                    }
                    currentJointSources[jointReceiver] = definedVar.getIndex();
                    variablesDefinedHere.add(definedVar.getIndex());
                }
            }
        }

        fixOutgoingPhis(initialBlock, block, currentJointSources, outgoingVariablesToRemove, variablesDefinedHere);
        for (BasicBlock blockToClear : blocksToClearHandlers) {
            blockToClear.getTryCatchBlocks().clear();
        }

        return block.getIndex();
    }

    private boolean isCallInstruction(Instruction insn) {
        if (insn instanceof InitClassInstruction || insn instanceof ConstructInstruction
                || insn instanceof ConstructArrayInstruction || insn instanceof ConstructMultiArrayInstruction
                || insn instanceof CloneArrayInstruction || insn instanceof RaiseInstruction) {
            return true;
        } else if (insn instanceof InvokeInstruction) {
            MethodReference method = ((InvokeInstruction) insn).getMethod();
            if (characteristics.isManaged(method)) {
                return true;
            }
            return method.getClassName().equals(ExceptionHandling.class.getName())
                    && method.getName().startsWith("throw");
        }
        return false;
    }

    private boolean isSpecialCallInstruction(Instruction insn) {
        if (!(insn instanceof InvokeInstruction)) {
            return false;
        }
        MethodReference method = ((InvokeInstruction) insn).getMethod();
        return method.getClassName().equals(ExceptionHandling.class.getName()) && method.getName().startsWith("throw");
    }

    private List<Instruction> setLocation(List<Instruction> instructions, TextLocation location) {
        if (location != null) {
            for (Instruction instruction : instructions) {
                instruction.setLocation(location);
            }
        }
        return instructions;
    }

    private List<Instruction> getInstructionsBeforeCallSite(CallSiteDescriptor callSite) {
        List<Instruction> instructions = new ArrayList<>();

        Variable idVariable = program.createVariable();
        IntegerConstantInstruction idInsn = new IntegerConstantInstruction();
        idInsn.setConstant(callSite.getId());
        idInsn.setReceiver(idVariable);
        instructions.add(idInsn);

        InvokeInstruction registerInsn = new InvokeInstruction();
        registerInsn.setMethod(new MethodReference(ShadowStack.class, "registerCallSite", int.class, void.class));
        registerInsn.setType(InvocationType.SPECIAL);
        registerInsn.setArguments(idVariable);
        instructions.add(registerInsn);

        return instructions;
    }

    private List<Instruction> getInstructionsAfterCallSite(BasicBlock initialBlock, BasicBlock block, BasicBlock next,
            CallSiteDescriptor callSite, int[] currentJointSources, IntSet outgoingVariablesToRemove,
            IntSet variablesDefinedHere) {
        Program program = block.getProgram();
        List<Instruction> instructions = new ArrayList<>();

        Variable handlerIdVariable = program.createVariable();
        InvokeInstruction getHandlerIdInsn = new InvokeInstruction();
        getHandlerIdInsn.setMethod(new MethodReference(ShadowStack.class, "getExceptionHandlerId", int.class));
        getHandlerIdInsn.setType(InvocationType.SPECIAL);
        getHandlerIdInsn.setReceiver(handlerIdVariable);
        instructions.add(getHandlerIdInsn);

        SwitchInstruction switchInsn = new SwitchInstruction();
        switchInsn.setCondition(handlerIdVariable);

        if (next != null) {
            SwitchTableEntry continueExecutionEntry = new SwitchTableEntry();
            continueExecutionEntry.setCondition(callSite.getId());
            continueExecutionEntry.setTarget(next);
            switchInsn.getEntries().add(continueExecutionEntry);
        }

        boolean defaultExists = false;
        int nextHandlerId = callSite.getId();
        for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
            ExceptionHandlerDescriptor handler = new ExceptionHandlerDescriptor(++nextHandlerId,
                    tryCatch.getExceptionType());
            callSite.getHandlers().add(handler);

            if (tryCatch.getExceptionType() == null) {
                defaultExists = true;
                switchInsn.setDefaultTarget(tryCatch.getHandler());
            } else {
                SwitchTableEntry catchEntry = new SwitchTableEntry();
                catchEntry.setTarget(tryCatch.getHandler());
                catchEntry.setCondition(handler.getId());
                switchInsn.getEntries().add(catchEntry);
            }
        }
        fixOutgoingPhis(initialBlock, block, currentJointSources, outgoingVariablesToRemove, variablesDefinedHere);

        if (!defaultExists) {
            switchInsn.setDefaultTarget(getDefaultExceptionHandler());
        }

        if (switchInsn.getEntries().isEmpty()) {
            instructions.clear();
            JumpInstruction jump = new JumpInstruction();
            jump.setTarget(switchInsn.getDefaultTarget());
            instructions.add(jump);
        } else if (switchInsn.getEntries().size() == 1) {
            SwitchTableEntry entry = switchInsn.getEntries().get(0);

            IntegerConstantInstruction singleTestConstant = new IntegerConstantInstruction();
            singleTestConstant.setConstant(entry.getCondition());
            singleTestConstant.setReceiver(program.createVariable());
            instructions.add(singleTestConstant);

            BinaryBranchingInstruction branching = new BinaryBranchingInstruction(BinaryBranchingCondition.EQUAL);
            branching.setConsequent(entry.getTarget());
            branching.setAlternative(switchInsn.getDefaultTarget());
            branching.setFirstOperand(switchInsn.getCondition());
            branching.setSecondOperand(singleTestConstant.getReceiver());
            instructions.add(branching);
        } else {
            instructions.add(switchInsn);
        }

        return instructions;
    }

    private void fixOutgoingPhis(BasicBlock block, BasicBlock newBlock, int[] currentJointSources,
            IntSet outgoingVariablesToRemove, IntSet variablesDefinedHere) {
        for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
            for (Phi phi : tryCatch.getHandler().getPhis()) {
                int value = currentJointSources[phi.getReceiver().getIndex()];
                if (value < 0) {
                    continue;
                }
                List<Incoming> additionalIncomings = new ArrayList<>();
                for (int i = 0; i < phi.getIncomings().size(); i++) {
                    Incoming incoming = phi.getIncomings().get(i);
                    if (incoming.getSource() != block) {
                        continue;
                    }
                    if (outgoingVariablesToRemove.contains(incoming.getValue().getIndex())) {
                        phi.getIncomings().remove(i--);
                        break;
                    } else if (incoming.getValue().getIndex() == value && incoming.getSource() != newBlock) {
                        if (variablesDefinedHere.contains(value)) {
                            incoming.setSource(newBlock);
                        } else {
                            Incoming incomingCopy = new Incoming();
                            incomingCopy.setSource(newBlock);
                            incomingCopy.setValue(incoming.getValue());
                            additionalIncomings.add(incomingCopy);
                        }
                        break;
                    }
                }

                phi.getIncomings().addAll(additionalIncomings);
            }
        }
    }

    private BasicBlock getDefaultExceptionHandler() {
        if (defaultExceptionHandler == null) {
            defaultExceptionHandler = program.createBasicBlock();
            Variable result = createReturnValueInstructions(defaultExceptionHandler);
            ExitInstruction exit = new ExitInstruction();
            exit.setValueToReturn(result);
            defaultExceptionHandler.add(exit);
        }
        return defaultExceptionHandler;
    }

    private Variable createReturnValueInstructions(BasicBlock block) {
        ValueType returnType = method.getReturnType();
        if (returnType == ValueType.VOID) {
            return null;
        }

        Variable variable = program.createVariable();

        if (returnType instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) returnType).getKind()) {
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case CHARACTER:
                case INTEGER:
                    IntegerConstantInstruction intConstant = new IntegerConstantInstruction();
                    intConstant.setReceiver(variable);
                    block.add(intConstant);
                    return variable;
                case LONG:
                    LongConstantInstruction longConstant = new LongConstantInstruction();
                    longConstant.setReceiver(variable);
                    block.add(longConstant);
                    return variable;
                case FLOAT:
                    FloatConstantInstruction floatConstant = new FloatConstantInstruction();
                    floatConstant.setReceiver(variable);
                    block.add(floatConstant);
                    return variable;
                case DOUBLE:
                    DoubleConstantInstruction doubleConstant = new DoubleConstantInstruction();
                    doubleConstant.setReceiver(variable);
                    block.add(doubleConstant);
                    return variable;
            }
        }

        NullConstantInstruction nullConstant = new NullConstantInstruction();
        nullConstant.setReceiver(variable);
        block.add(nullConstant);

        return variable;
    }
}
