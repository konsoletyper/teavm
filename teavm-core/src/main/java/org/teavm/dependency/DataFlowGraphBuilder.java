/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.dependency;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import java.util.Arrays;
import java.util.List;
import org.teavm.common.*;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
public class DataFlowGraphBuilder implements InstructionReader {
    private int lastIndex;
    private GraphBuilder builder = new GraphBuilder();
    private IntSet importantNodes = new IntOpenHashSet();
    private ObjectIntMap<MethodReference> methodNodes = new ObjectIntOpenHashMap<>();
    private ObjectIntMap<FieldReference> fieldNodes = new ObjectIntOpenHashMap<>();
    private int[] arrayNodes;

    public void important(int node) {
        importantNodes.add(node);
    }

    public int[] buildMapping(ProgramReader program, int paramCount) {
        lastIndex = program.variableCount();
        arrayNodes = new int[lastIndex];
        Arrays.fill(arrayNodes, -1);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            for (PhiReader phi : block.readPhis()) {
                for (IncomingReader incoming : phi.readIncomings()) {
                    builder.addEdge(incoming.getValue().getIndex(), phi.getReceiver().getIndex());
                }
            }
            block.readAllInstructions(this);
        }
        Graph graph = builder.build();

        DisjointSet classes = new DisjointSet();
        for (int i = 0; i < lastIndex; ++i) {
            classes.create();
        }
        IntegerArray startNodes = new IntegerArray(graph.size());
        for (int i = paramCount; i < graph.size(); ++i) {
            if (graph.incomingEdgesCount(i) == 0) {
                startNodes.add(i);
            }
            for (int pred : graph.incomingEdges(i)) {
                boolean predImportant = importantNodes.contains(classes.find(pred));
                boolean nodeImportant = importantNodes.contains(classes.find(i));
                if (predImportant && nodeImportant) {
                    continue;
                }
                int newCls = classes.union(pred, i);
                if (nodeImportant || predImportant) {
                    importantNodes.add(newCls);
                }
            }
        }

        int[][] sccs = GraphUtils.findStronglyConnectedComponents(graph, startNodes.getAll());
        for (int[] scc : sccs) {
            int last = -1;
            for (int node : scc) {
                if (!importantNodes.contains(classes.find(node))) {
                    continue;
                }
                last = last < 0 ? node : classes.union(node, last);
            }
        }
        return classes.pack(program.variableCount());
    }

    @Override
    public void location(InstructionLocation location) {
    }

    @Override
    public void nop() {
    }

    @Override
    public void classConstant(VariableReader receiver, ValueType cst) {
    }

    @Override
    public void nullConstant(VariableReader receiver) {
    }

    @Override
    public void integerConstant(VariableReader receiver, int cst) {
    }

    @Override
    public void longConstant(VariableReader receiver, long cst) {
    }

    @Override
    public void floatConstant(VariableReader receiver, float cst) {
    }

    @Override
    public void doubleConstant(VariableReader receiver, double cst) {
    }

    @Override
    public void stringConstant(VariableReader receiver, String cst) {
    }

    @Override
    public void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
            NumericOperandType type) {
    }

    @Override
    public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) {
    }

    @Override
    public void assign(VariableReader receiver, VariableReader assignee) {
        builder.addEdge(assignee.getIndex(), receiver.getIndex());
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
        builder.addEdge(value.getIndex(), receiver.getIndex());
        important(receiver.getIndex());
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
            NumericOperandType targetType) {
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
            CastIntegerDirection targetType) {
    }

    @Override
    public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
            BasicBlockReader alternative) {
    }

    @Override
    public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
            BasicBlockReader consequent, BasicBlockReader alternative) {
    }

    @Override
    public void jump(BasicBlockReader target) {
    }

    @Override
    public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
            BasicBlockReader defaultTarget) {
    }

    @Override
    public void exit(VariableReader valueToReturn) {
        if (valueToReturn != null) {
            important(valueToReturn.getIndex());
        }
    }

    @Override
    public void raise(VariableReader exception) {
        important(exception.getIndex());
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, List<? extends VariableReader> dimensions) {
    }

    @Override
    public void create(VariableReader receiver, String type) {
    }

    private int getFieldNode(FieldReference field) {
        int fieldNode = fieldNodes.getOrDefault(field, -1);
        if (fieldNode < 0) {
            fieldNode = lastIndex++;
            fieldNodes.put(field, fieldNode);
        }
        important(fieldNode);
        return fieldNode;
    }

    @Override
    public void getField(VariableReader receiver, VariableReader instance, FieldReference field, ValueType fieldType) {
        int fieldNode = getFieldNode(field);
        builder.addEdge(fieldNode, receiver.getIndex());
    }


    @Override
    public void putField(VariableReader instance, FieldReference field, VariableReader value) {
        int fieldNode = getFieldNode(field);
        builder.addEdge(value.getIndex(), fieldNode);
    }

    @Override
    public void arrayLength(VariableReader receiver, VariableReader array) {
    }

    @Override
    public void cloneArray(VariableReader receiver, VariableReader array) {
        important(receiver.getIndex());
        builder.addEdge(array.getIndex(), receiver.getIndex());
    }

    @Override
    public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
        if (elementType == ArrayElementType.OBJECT) {
            builder.addEdge(array.getIndex(), receiver.getIndex());
        }
    }

    private int getArrayElementNode(int array) {
        int node = arrayNodes[array];
        if (node < 0) {
            node = lastIndex++;
            arrayNodes[array] = node;
        }
        important(node);
        return node;
    }

    @Override
    public void getElement(VariableReader receiver, VariableReader array, VariableReader index) {
        builder.addEdge(getArrayElementNode(array.getIndex()), receiver.getIndex());
    }

    @Override
    public void putElement(VariableReader array, VariableReader index, VariableReader value) {
        builder.addEdge(value.getIndex(), getArrayElementNode(array.getIndex()));
    }

    private int getMethodNode(MethodReference method) {
        int methodNode = methodNodes.getOrDefault(method, -1);
        if (methodNode < 0) {
            methodNode = lastIndex++;
            methodNodes.put(method, methodNode);
        }
        important(methodNode);
        return methodNode;
    }

    @Override
    public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments, InvocationType type) {
        if (receiver != null) {
            builder.addEdge(getMethodNode(method), receiver.getIndex());
        }
    }

    @Override
    public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
    }

    @Override
    public void initClass(String className) {
    }

    @Override
    public void nullCheck(VariableReader receiver, VariableReader value) {
        builder.addEdge(value.getIndex(), receiver.getIndex());
    }

    @Override
    public void monitorEnter(VariableReader objectRef) {
    }

    @Override
    public void monitorExit(VariableReader objectRef) {
    }
}
