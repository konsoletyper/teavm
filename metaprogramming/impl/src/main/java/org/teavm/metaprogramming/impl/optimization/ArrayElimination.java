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
package org.teavm.metaprogramming.impl.optimization;

import org.teavm.common.DisjointSet;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.PhiUpdater;
import org.teavm.model.util.UsageExtractor;

public class ArrayElimination {
    private Program program;
    private int[] varClasses;
    private int[] constantValues;
    private boolean[] constants;
    private boolean[] unsafeArrays;
    private int[] arraySizes;
    private boolean hasModifications;

    public void apply(Program program, MethodReference methodReference) {
        this.program = program;
        findVarClasses();
        findConstantVariables();
        findSafeArrays();
        removeSafeArrays();
        if (hasModifications) {
            Variable[] parameters = new Variable[methodReference.parameterCount() + 1];
            for (int i = 0; i < parameters.length; ++i) {
                parameters[i] = program.variableAt(i);
            }
            new PhiUpdater().updatePhis(program, parameters);
        }
    }

    private void findVarClasses() {
        DisjointSet varClasses = new DisjointSet();
        for (int i = 0; i < program.variableCount(); ++i) {
            varClasses.create();
        }
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                int receiver;
                int assignee;
                if (instruction instanceof AssignInstruction) {
                    AssignInstruction assign = (AssignInstruction) instruction;
                    receiver = assign.getReceiver().getIndex();
                    assignee = assign.getAssignee().getIndex();
                } else if (instruction instanceof UnwrapArrayInstruction) {
                    UnwrapArrayInstruction unwrap = (UnwrapArrayInstruction) instruction;
                    receiver = unwrap.getReceiver().getIndex();
                    assignee = unwrap.getArray().getIndex();
                } else {
                    continue;
                }
                varClasses.union(receiver, assignee);
            }
        }
        this.varClasses = varClasses.pack(program.variableCount());
    }

    private void findConstantVariables() {
        int[] constantValuesByClasses = new int[program.variableCount()];
        boolean[] constantsByClasses = new boolean[program.variableCount()];
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (instruction instanceof IntegerConstantInstruction) {
                    IntegerConstantInstruction constant = (IntegerConstantInstruction) instruction;
                    int receiver = varClasses[constant.getReceiver().getIndex()];
                    constantsByClasses[receiver] = true;
                    constantValuesByClasses[receiver] = constant.getConstant();
                }
            }
        }

        constantValues = new int[program.variableCount()];
        constants = new boolean[program.variableCount()];
        for (int i = 0; i < program.variableCount(); ++i) {
            constantValues[i] = constantValuesByClasses[varClasses[i]];
            constants[i] = constantsByClasses[varClasses[i]];
        }
    }

    private void findSafeArrays() {
        boolean[] unsafeArraysByClasses = new boolean[program.variableCount()];
        int[] arraySizesByClasses = new int[program.variableCount()];

        DefinitionExtractor defExtractor = new DefinitionExtractor();
        UsageExtractor useExtractor = new UsageExtractor();
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Phi phi : block.getPhis()) {
                unsafeArraysByClasses[varClasses[phi.getReceiver().getIndex()]] = true;
                for (Incoming incoming : phi.getIncomings()) {
                    unsafeArraysByClasses[varClasses[incoming.getValue().getIndex()]] = true;
                }
            }
            for (Instruction instruction : block) {
                if (instruction instanceof GetElementInstruction) {
                    GetElementInstruction getElement = (GetElementInstruction) instruction;
                    unsafeArraysByClasses[varClasses[getElement.getReceiver().getIndex()]] = true;
                } else if (instruction instanceof PutElementInstruction) {
                    PutElementInstruction putElement = (PutElementInstruction) instruction;
                    unsafeArraysByClasses[varClasses[putElement.getValue().getIndex()]] = true;
                    if (!constants[putElement.getIndex().getIndex()]) {
                        unsafeArraysByClasses[varClasses[putElement.getIndex().getIndex()]] = true;
                    }
                } else if (instruction instanceof ConstructArrayInstruction) {
                    ConstructArrayInstruction construct = (ConstructArrayInstruction) instruction;
                    int receiver = varClasses[construct.getReceiver().getIndex()];
                    if (!constants[construct.getSize().getIndex()]) {
                        unsafeArraysByClasses[receiver] = true;
                    } else {
                        arraySizesByClasses[receiver] = constantValues[construct.getSize().getIndex()];
                    }
                } else {
                    if (instruction instanceof AssignInstruction || instruction instanceof UnwrapArrayInstruction) {
                        continue;
                    }
                    instruction.acceptVisitor(defExtractor);
                    instruction.acceptVisitor(useExtractor);
                    for (Variable var : defExtractor.getDefinedVariables()) {
                        unsafeArraysByClasses[varClasses[var.getIndex()]] = true;
                    }
                    for (Variable var : useExtractor.getUsedVariables()) {
                        unsafeArraysByClasses[varClasses[var.getIndex()]] = true;
                    }
                }
            }
        }

        unsafeArrays = new boolean[program.variableCount()];
        arraySizes = new int[program.variableCount()];
        for (int i = 0; i < program.variableCount(); ++i) {
            unsafeArrays[i] = unsafeArraysByClasses[varClasses[i]];
            arraySizes[i] = arraySizesByClasses[varClasses[i]];
        }
    }

    private void removeSafeArrays() {
        int[][] arrayItemsAsVariablesByClass = new int[program.variableCount()][];
        for (int i = 0; i < arrayItemsAsVariablesByClass.length; ++i) {
            int varClass = varClasses[i];
            if (arrayItemsAsVariablesByClass[varClass] != null || unsafeArrays[i]) {
                continue;
            }
            int size = arraySizes[i];
            arrayItemsAsVariablesByClass[varClass] = new int[size];
            for (int j = 0; j < size; ++j) {
                arrayItemsAsVariablesByClass[varClass][j] = program.createVariable().getIndex();
            }
        }

        int[][] arrayItemsAsVariables = new int[arrayItemsAsVariablesByClass.length][];
        for (int i = 0; i < arrayItemsAsVariables.length; ++i) {
            arrayItemsAsVariables[i] = arrayItemsAsVariablesByClass[varClasses[i]];
        }

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (instruction instanceof GetElementInstruction) {
                    GetElementInstruction getElement = (GetElementInstruction) instruction;
                    int array = getElement.getArray().getIndex();
                    if (!unsafeArrays[array]) {
                        int index = constantValues[getElement.getIndex().getIndex()];
                        Variable var = program.variableAt(arrayItemsAsVariables[array][index]);
                        AssignInstruction assign = new AssignInstruction();
                        assign.setReceiver(getElement.getReceiver());
                        assign.setAssignee(var);
                        assign.setLocation(getElement.getLocation());
                        getElement.replace(assign);
                        hasModifications = true;
                    }
                } else if (instruction instanceof PutElementInstruction) {
                    PutElementInstruction putElement = (PutElementInstruction) instruction;
                    int array = putElement.getArray().getIndex();
                    if (!unsafeArrays[array]) {
                        int index = constantValues[putElement.getIndex().getIndex()];
                        Variable var = program.variableAt(arrayItemsAsVariables[array][index]);
                        AssignInstruction assign = new AssignInstruction();
                        assign.setReceiver(var);
                        assign.setAssignee(putElement.getValue());
                        assign.setLocation(putElement.getLocation());
                        putElement.replace(assign);
                        hasModifications = true;
                    }
                } else if (instruction instanceof ConstructArrayInstruction) {
                    ConstructArrayInstruction construct = (ConstructArrayInstruction) instruction;
                    if (!unsafeArrays[construct.getReceiver().getIndex()]) {
                        int[] vars = arrayItemsAsVariables[construct.getReceiver().getIndex()];
                        for (int i = 0; i < vars.length; ++i) {
                            Instruction constantInsn = createDefaultConstantInstruction(construct.getItemType(),
                                    program.variableAt(vars[i]));
                            constantInsn.setLocation(construct.getLocation());
                            construct.insertPrevious(constantInsn);
                        }
                        construct.delete();
                        hasModifications = true;
                    }
                } else if (instruction instanceof UnwrapArrayInstruction) {
                    UnwrapArrayInstruction unwrap = (UnwrapArrayInstruction) instruction;
                    if (!unsafeArrays[unwrap.getArray().getIndex()]) {
                        unwrap.delete();
                        hasModifications = true;
                    }
                } else if (instruction instanceof AssignInstruction) {
                    AssignInstruction assign = (AssignInstruction) instruction;
                    if (!unsafeArrays[assign.getAssignee().getIndex()]) {
                        assign.delete();
                        hasModifications = true;
                    }
                }
            }
        }
    }

    private Instruction createDefaultConstantInstruction(ValueType valueType, Variable receiver) {
        if (valueType instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) valueType).getKind()) {
                case BOOLEAN:
                case CHARACTER:
                case BYTE:
                case SHORT:
                case INTEGER: {
                    IntegerConstantInstruction result = new IntegerConstantInstruction();
                    result.setReceiver(receiver);
                    return result;
                }
                case LONG: {
                    LongConstantInstruction result = new LongConstantInstruction();
                    result.setReceiver(receiver);
                    return result;
                }
                case FLOAT: {
                    FloatConstantInstruction result = new FloatConstantInstruction();
                    result.setReceiver(receiver);
                    return result;
                }
                case DOUBLE: {
                    DoubleConstantInstruction result = new DoubleConstantInstruction();
                    result.setReceiver(receiver);
                    return result;
                }
            }
        }
        NullConstantInstruction result = new NullConstantInstruction();
        result.setReceiver(receiver);
        return result;
    }
}