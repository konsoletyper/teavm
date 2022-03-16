/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.backend.lowlevel.transform;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.common.Graph;
import org.teavm.common.GraphSplittingBackend;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.SwitchTableEntry;
import org.teavm.model.util.BasicBlockMapper;
import org.teavm.model.util.BasicBlockSplitter;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.LivenessAnalyzer;
import org.teavm.model.util.PhiUpdater;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.TypeInferer;
import org.teavm.model.util.UsageExtractor;
import org.teavm.model.util.VariableType;
import org.teavm.runtime.Fiber;

public class CoroutineTransformation {
    private static final MethodReference FIBER_SUSPEND = new MethodReference(Fiber.class, "suspend",
            Fiber.AsyncCall.class, Object.class);
    private static final String ASYNC_CALL = Fiber.class.getName() + "$AsyncCall";
    private ClassReaderSource classSource;
    private LivenessAnalyzer livenessAnalysis = new LivenessAnalyzer();
    private TypeInferer variableTypes = new TypeInferer();
    private Set<MethodReference> asyncMethods;
    private Program program;
    private Variable fiberVar;
    private BasicBlockSplitter splitter;
    private SwitchInstruction resumeSwitch;
    private int parameterCount;
    private ValueType returnType;
    private boolean hasThreads;

    public CoroutineTransformation(ClassReaderSource classSource, Set<MethodReference> asyncMethods,
            boolean hasThreads) {
        this.classSource = classSource;
        this.asyncMethods = asyncMethods;
        this.hasThreads = hasThreads;
    }

    public void apply(Program program, MethodReference methodReference) {
        if (methodReference.getClassName().equals(Fiber.class.getName())) {
            return;
        }

        ClassReader cls = classSource.get(methodReference.getClassName());
        if (cls != null && cls.getInterfaces().contains(ASYNC_CALL)) {
            return;
        }

        boolean hasJob = false;
        for (BasicBlock block : program.getBasicBlocks()) {
            if (hasSplitInstructions(block)) {
                hasJob = true;
            }
        }
        if (!hasJob) {
            return;
        }

        this.program = program;
        parameterCount = methodReference.parameterCount();
        returnType = methodReference.getReturnType();
        variableTypes.inferTypes(program, methodReference);
        livenessAnalysis.analyze(program, methodReference.getDescriptor());
        splitter = new BasicBlockSplitter(program);
        int basicBlockCount = program.basicBlockCount();
        createSplitPrologue();
        for (int i = 1; i <= basicBlockCount; ++i) {
            processBlock(program.basicBlockAt(i));
        }
        splitter.fixProgram();
        processIrreducibleCfg();
        new PhiUpdater().updatePhis(program, methodReference.parameterCount() + 1);
    }

    private void createSplitPrologue() {
        fiberVar = program.createVariable();
        fiberVar.setLabel("fiber");

        BasicBlock firstBlock = program.basicBlockAt(0);
        BasicBlock continueBlock = splitter.split(firstBlock, null);
        BasicBlock switchStateBlock = program.createBasicBlock();
        TextLocation location = continueBlock.getFirstInstruction().getLocation();

        InvokeInstruction getFiber = new InvokeInstruction();
        getFiber.setType(InvocationType.SPECIAL);
        getFiber.setMethod(new MethodReference(Fiber.class, "current", Fiber.class));
        getFiber.setReceiver(fiberVar);
        getFiber.setLocation(location);
        firstBlock.add(getFiber);

        InvokeInstruction isResuming = new InvokeInstruction();
        isResuming.setType(InvocationType.SPECIAL);
        isResuming.setMethod(new MethodReference(Fiber.class, "isResuming", boolean.class));
        isResuming.setInstance(fiberVar);
        isResuming.setReceiver(program.createVariable());
        isResuming.setLocation(location);
        firstBlock.add(isResuming);

        BranchingInstruction jumpIfResuming = new BranchingInstruction(BranchingCondition.NOT_EQUAL);
        jumpIfResuming.setOperand(isResuming.getReceiver());
        jumpIfResuming.setConsequent(switchStateBlock);
        jumpIfResuming.setAlternative(continueBlock);
        firstBlock.add(jumpIfResuming);

        InvokeInstruction popInt = new InvokeInstruction();
        popInt.setType(InvocationType.SPECIAL);
        popInt.setMethod(new MethodReference(Fiber.class, "popInt", int.class));
        popInt.setInstance(fiberVar);
        popInt.setReceiver(program.createVariable());
        popInt.setLocation(location);
        switchStateBlock.add(popInt);

        resumeSwitch = new SwitchInstruction();
        resumeSwitch.setDefaultTarget(continueBlock);
        resumeSwitch.setCondition(popInt.getReceiver());
        resumeSwitch.setLocation(location);
        switchStateBlock.add(resumeSwitch);
    }

    private void processBlock(BasicBlock block) {
        Map<Instruction, BitSet> splitInstructions = collectSplitInstructions(block);
        List<Instruction> instructionList = new ArrayList<>(splitInstructions.keySet());
        Collections.reverse(instructionList);
        for (Instruction instruction : instructionList) {
            BasicBlock intermediate = splitter.split(block, instruction.getPrevious());
            BasicBlock next = splitter.split(intermediate, instruction);
            createSplitPoint(block, intermediate, next, splitInstructions.get(instruction));
            block = next;
        }
    }

    private Map<Instruction, BitSet> collectSplitInstructions(BasicBlock block) {
        if (!hasSplitInstructions(block)) {
            return Collections.emptyMap();
        }

        BitSet live = livenessAnalysis.liveOut(block.getIndex());

        Map<Instruction, BitSet> result = new LinkedHashMap<>();
        UsageExtractor use = new UsageExtractor();
        DefinitionExtractor def = new DefinitionExtractor();
        for (Instruction instruction = block.getLastInstruction(); instruction != null;
                instruction = instruction.getPrevious()) {
            instruction.acceptVisitor(def);
            if (def.getDefinedVariables() != null) {
                for (Variable var : def.getDefinedVariables()) {
                    live.clear(var.getIndex());
                }
            }
            instruction.acceptVisitor(use);
            if (use.getUsedVariables() != null) {
                for (Variable var : use.getUsedVariables()) {
                    live.set(var.getIndex());
                }
            }

            if (isSplitInstruction(instruction)) {
                result.put(instruction, (BitSet) live.clone());
            }
        }
        return result;
    }

    private boolean hasSplitInstructions(BasicBlock block) {
        for (Instruction instruction : block) {
            if (isSplitInstruction(instruction)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSplitInstruction(Instruction instruction) {
        if (instruction instanceof InvokeInstruction) {
            InvokeInstruction invoke = (InvokeInstruction) instruction;
            MethodReference method = findRealMethod(invoke.getMethod());
            if (method.equals(FIBER_SUSPEND)) {
                return true;
            }
            if (method.getClassName().equals(Fiber.class.getName())) {
                return false;
            }
            return asyncMethods.contains(method);
        } else if (instruction instanceof InitClassInstruction) {
            return isSplittingClassInitializer(((InitClassInstruction) instruction).getClassName());
        } else {
            return hasThreads && instruction instanceof MonitorEnterInstruction;
        }
    }

    private void createSplitPoint(BasicBlock block, BasicBlock intermediate, BasicBlock next, BitSet liveVars) {
        int stateNumber = resumeSwitch.getEntries().size();
        Instruction splitInstruction = intermediate.getFirstInstruction();

        JumpInstruction jumpToIntermediate = new JumpInstruction();
        jumpToIntermediate.setTarget(intermediate);
        jumpToIntermediate.setLocation(splitInstruction.getLocation());
        block.add(jumpToIntermediate);

        BasicBlock restoreBlock = program.createBasicBlock();
        BasicBlock saveBlock = program.createBasicBlock();

        SwitchTableEntry switchTableEntry = new SwitchTableEntry();
        switchTableEntry.setCondition(stateNumber);
        switchTableEntry.setTarget(restoreBlock);
        resumeSwitch.getEntries().add(switchTableEntry);

        InvokeInstruction isSuspending = new InvokeInstruction();
        isSuspending.setType(InvocationType.SPECIAL);
        isSuspending.setMethod(new MethodReference(Fiber.class, "isSuspending", boolean.class));
        isSuspending.setInstance(fiberVar);
        isSuspending.setReceiver(program.createVariable());
        isSuspending.setLocation(splitInstruction.getLocation());
        intermediate.add(isSuspending);

        BranchingInstruction branchIfSuspending = new BranchingInstruction(BranchingCondition.NOT_EQUAL);
        branchIfSuspending.setOperand(isSuspending.getReceiver());
        branchIfSuspending.setConsequent(saveBlock);
        branchIfSuspending.setAlternative(next);
        branchIfSuspending.setLocation(splitInstruction.getLocation());
        intermediate.add(branchIfSuspending);

        restoreBlock.addAll(restoreState(liveVars));
        JumpInstruction doneRestoring = new JumpInstruction();
        doneRestoring.setTarget(intermediate);
        restoreBlock.add(doneRestoring);
        for (Instruction instruction : restoreBlock) {
            instruction.setLocation(splitInstruction.getLocation());
        }

        for (Instruction instruction : saveState(liveVars)) {
            instruction.setLocation(splitInstruction.getLocation());
            saveBlock.add(instruction);
        }
        for (Instruction instruction : saveStateNumber(stateNumber)) {
            instruction.setLocation(splitInstruction.getLocation());
            saveBlock.add(instruction);
        }
        createReturnInstructions(splitInstruction.getLocation(), saveBlock);
    }

    private List<Instruction> saveState(BitSet vars) {
        List<Instruction> instructions = new ArrayList<>();
        for (int var = vars.nextSetBit(0); var >= 0; var = vars.nextSetBit(var + 1)) {
            saveVariable(var, instructions);
        }

        return instructions;
    }

    private List<Instruction> saveStateNumber(int number) {
        IntegerConstantInstruction constant = new IntegerConstantInstruction();
        constant.setReceiver(program.createVariable());
        constant.setConstant(number);

        InvokeInstruction invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(new MethodReference(Fiber.class, "push", int.class, void.class));
        invoke.setInstance(fiberVar);
        invoke.setArguments(constant.getReceiver());

        return Arrays.asList(constant, invoke);
    }

    private List<Instruction> restoreState(BitSet vars) {
        List<Instruction> instructions = new ArrayList<>();
        int[] varArray = new int[vars.cardinality()];
        int j = 0;
        for (int i = vars.nextSetBit(0); i >= 0; i = vars.nextSetBit(i + 1)) {
            varArray[j++] = i;
        }

        for (int i = varArray.length - 1; i >= 0; --i) {
            restoreVariable(varArray[i], instructions);
        }

        return instructions;
    }

    private void saveVariable(int var, List<Instruction> instructions) {
        VariableType type = variableTypes.typeOf(var);
        InvokeInstruction invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setInstance(fiberVar);
        invoke.setArguments(program.variableAt(var));

        switch (type) {
            case INT:
                invoke.setMethod(new MethodReference(Fiber.class, "push", int.class, void.class));
                break;
            case LONG:
                invoke.setMethod(new MethodReference(Fiber.class, "push", long.class, void.class));
                break;
            case FLOAT:
                invoke.setMethod(new MethodReference(Fiber.class, "push", float.class, void.class));
                break;
            case DOUBLE:
                invoke.setMethod(new MethodReference(Fiber.class, "push", double.class, void.class));
                break;
            default:
                invoke.setMethod(new MethodReference(Fiber.class, "push", Object.class, void.class));
                break;
        }

        instructions.add(invoke);
    }

    private void restoreVariable(int var, List<Instruction> instructions) {
        VariableType type = variableTypes.typeOf(var);
        InvokeInstruction invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setInstance(fiberVar);
        invoke.setReceiver(program.variableAt(var));

        switch (type) {
            case INT:
                invoke.setMethod(new MethodReference(Fiber.class, "popInt", int.class));
                break;
            case LONG:
                invoke.setMethod(new MethodReference(Fiber.class, "popLong", long.class));
                break;
            case FLOAT:
                invoke.setMethod(new MethodReference(Fiber.class, "popFloat", float.class));
                break;
            case DOUBLE:
                invoke.setMethod(new MethodReference(Fiber.class, "popDouble", double.class));
                break;
            default:
                invoke.setMethod(new MethodReference(Fiber.class, "popObject", Object.class));
                break;
        }

        instructions.add(invoke);
    }

    private boolean isSplittingClassInitializer(String className) {
        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return false;
        }

        MethodReader method = cls.getMethod(new MethodDescriptor("<clinit>", ValueType.VOID));
        return method != null && asyncMethods.contains(method.getReference());
    }

    private MethodReference findRealMethod(MethodReference method) {
        String clsName = method.getClassName();
        while (clsName != null) {
            ClassReader cls = classSource.get(clsName);
            if (cls == null) {
                break;
            }
            MethodReader methodReader = cls.getMethod(method.getDescriptor());
            if (methodReader != null) {
                return new MethodReference(clsName, method.getDescriptor());
            }
            clsName = cls.getParent();
            if (clsName != null && clsName.equals(cls.getName())) {
                break;
            }
        }
        return method;
    }

    private void createReturnInstructions(TextLocation location, BasicBlock block) {
        ExitInstruction exit = new ExitInstruction();
        exit.setLocation(location);
        if (returnType == ValueType.VOID) {
            block.add(exit);
            return;
        }
        exit.setValueToReturn(program.createVariable());
        Instruction returnValue = createReturnValueInstruction(exit.getValueToReturn());
        returnValue.setLocation(location);
        block.add(returnValue);
        block.add(exit);
    }

    private Instruction createReturnValueInstruction(Variable target) {
        if (returnType instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) returnType).getKind()) {
                case BOOLEAN:
                case BYTE:
                case CHARACTER:
                case SHORT:
                case INTEGER: {
                    IntegerConstantInstruction instruction = new IntegerConstantInstruction();
                    instruction.setReceiver(target);
                    return instruction;
                }
                case LONG: {
                    LongConstantInstruction instruction = new LongConstantInstruction();
                    instruction.setReceiver(target);
                    return instruction;
                }
                case FLOAT: {
                    FloatConstantInstruction instruction = new FloatConstantInstruction();
                    instruction.setReceiver(target);
                    return instruction;
                }
                case DOUBLE: {
                    DoubleConstantInstruction instruction = new DoubleConstantInstruction();
                    instruction.setReceiver(target);
                    return instruction;
                }
            }
        }

        NullConstantInstruction instruction = new NullConstantInstruction();
        instruction.setReceiver(target);
        return instruction;
    }

    private void processIrreducibleCfg() {
        Graph graph = ProgramUtils.buildControlFlowGraph(program);
        if (!GraphUtils.isIrreducible(graph)) {
            return;
        }

        SplittingBackend splittingBackend = new SplittingBackend();
        int[] weights = new int[graph.size()];
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            weights[i] = program.basicBlockAt(i).instructionCount();
        }
        GraphUtils.splitIrreducibleGraph(graph, weights, splittingBackend);
        new PhiUpdater().updatePhis(program, parameterCount + 1);
    }

    class SplittingBackend implements GraphSplittingBackend {
        @Override
        public int[] split(int[] domain, int[] nodes) {
            int[] copies = new int[nodes.length];
            IntIntMap map = new IntIntHashMap();
            IntSet nodeSet = IntHashSet.from(nodes);
            List<List<Incoming>> outputs = ProgramUtils.getPhiOutputs(program);
            for (int i = 0; i < nodes.length; ++i) {
                int node = nodes[i];
                BasicBlock block = program.basicBlockAt(node);
                BasicBlock blockCopy = program.createBasicBlock();
                ProgramUtils.copyBasicBlock(block, blockCopy);
                copies[i] = blockCopy.getIndex();
                map.put(node, copies[i] + 1);
            }

            BasicBlockMapper copyBlockMapper = new BasicBlockMapper((int block) -> {
                int mappedIndex = map.get(block);
                return mappedIndex == 0 ? block : mappedIndex - 1;
            });
            for (int copy : copies) {
                copyBlockMapper.transform(program.basicBlockAt(copy));
            }
            for (int domainNode : domain) {
                copyBlockMapper.transformWithoutPhis(program.basicBlockAt(domainNode));
            }

            for (int i = 0; i < nodes.length; ++i) {
                int node = nodes[i];
                BasicBlock blockCopy = program.basicBlockAt(copies[i]);
                for (Incoming output : outputs.get(node)) {
                    if (!nodeSet.contains(output.getPhi().getBasicBlock().getIndex())) {
                        Incoming outputCopy = new Incoming();
                        outputCopy.setSource(blockCopy);
                        outputCopy.setValue(output.getValue());
                        output.getPhi().getIncomings().add(outputCopy);
                    }
                }
            }

            return copies;
        }
    }
}
