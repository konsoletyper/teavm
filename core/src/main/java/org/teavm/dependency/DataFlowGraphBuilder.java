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
import com.carrotsearch.hppc.cursors.IntCursor;
import java.util.Arrays;
import java.util.List;
import org.teavm.common.*;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

public class DataFlowGraphBuilder extends AbstractInstructionReader {
    private int lastIndex;
    private GraphBuilder builder = new GraphBuilder();
    private ObjectIntMap<FieldReference> fieldNodes = new ObjectIntOpenHashMap<>();
    private int returnIndex = -1;
    private int exceptionIndex;
    private DisjointSet classes = new DisjointSet();
    private int paramCount;
    private IntSet escaping = new IntOpenHashSet();

    private void join(int a, int b) {
        if (a < paramCount || b < paramCount) {
            return;
        }
        classes.union(a, b);
    }

    public int[] buildMapping(ProgramReader program, boolean[] significantParams, boolean needsReturn) {
        lastIndex = program.variableCount();
        this.paramCount = significantParams.length;
        if (needsReturn) {
            returnIndex = lastIndex++;
            escaping.add(returnIndex);
        }
        exceptionIndex = lastIndex++;
        for (int i = 0; i < lastIndex; ++i) {
            classes.create();
        }

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            for (PhiReader phi : block.readPhis()) {
                for (IncomingReader incoming : phi.readIncomings()) {
                    int from = incoming.getValue().getIndex();
                    int to = phi.getReceiver().getIndex();
                    builder.addEdge(from, to);
                }
            }
            block.readAllInstructions(this);
        }
        Graph graph = builder.build();
        for (int i = 0; i < paramCount; ++i) {
            if (significantParams[i]) {
                escaping.add(i);
            }
        }
        propagateEscaping(graph);

        int[] classMap = new int[classes.size()];
        Arrays.fill(classMap, -1);
        int[] result = new int[program.variableCount()];
        int classCount = 0;
        for (int i = 0; i < program.variableCount(); ++i) {
            if (!escaping.contains(i) && i >= significantParams.length) {
                result[i] = -1;
                continue;
            }
            int cls = classes.find(i);
            int packedCls = classMap[cls];
            if (packedCls < 0) {
                packedCls = classCount++;
                classMap[cls] = packedCls;
            }
            result[i] = packedCls;
        }
        return result;
    }

    private void propagateEscaping(Graph graph) {
        IntegerStack stack = new IntegerStack(graph.size());
        for (IntCursor node : escaping) {
            stack.push(node.value);
        }
        escaping.clear();
        while (!stack.isEmpty()) {
            int node = stack.pop();
            if (!escaping.add(node)) {
                continue;
            }
            if (node < graph.size()) {
                for (int pred : graph.incomingEdges(node)) {
                    if (!escaping.contains(pred)) {
                        stack.push(pred);
                    }
                }
                for (int succ : graph.outgoingEdges(node)) {
                    if (!escaping.contains(succ)) {
                        stack.push(succ);
                    }
                }
            }
        }
    }

    private void connect(int a, int b) {
        builder.addEdge(a, b);
        join(a, b);
    }

    @Override
    public void assign(VariableReader receiver, VariableReader assignee) {
        connect(assignee.getIndex(), receiver.getIndex());
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
        builder.addEdge(value.getIndex(), receiver.getIndex());
    }

    @Override
    public void exit(VariableReader valueToReturn) {
        if (valueToReturn != null && returnIndex >= 0) {
            builder.addEdge(valueToReturn.getIndex(), returnIndex);
        }
    }

    @Override
    public void raise(VariableReader exception) {
        builder.addEdge(exception.getIndex(), exceptionIndex);
        escaping.add(exceptionIndex);
    }

    private int getFieldNode(FieldReference field) {
        int fieldNode = fieldNodes.getOrDefault(field, -1);
        if (fieldNode < 0) {
            fieldNode = classes.create();
            fieldNodes.put(field, fieldNode);
        }
        escaping.add(fieldNode);
        return fieldNode;
    }

    @Override
    public void getField(VariableReader receiver, VariableReader instance, FieldReference field, ValueType fieldType) {
        if (fieldType instanceof ValueType.Primitive) {
            return;
        }
        builder.addEdge(getFieldNode(field), receiver.getIndex());
    }

    @Override
    public void putField(VariableReader instance, FieldReference field, VariableReader value, ValueType fieldType) {
        if (fieldType instanceof ValueType.Primitive) {
            return;
        }
        builder.addEdge(value.getIndex(), getFieldNode(field));
    }

    @Override
    public void cloneArray(VariableReader receiver, VariableReader array) {
        builder.addEdge(array.getIndex(), receiver.getIndex());
    }

    @Override
    public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
        if (elementType == ArrayElementType.OBJECT) {
            connect(array.getIndex(), receiver.getIndex());
        }
    }

    @Override
    public void getElement(VariableReader receiver, VariableReader array, VariableReader index,
            ArrayElementType type) {
        builder.addEdge(array.getIndex(), receiver.getIndex());
    }

    @Override
    public void putElement(VariableReader array, VariableReader index, VariableReader value, ArrayElementType type) {
        builder.addEdge(value.getIndex(), array.getIndex());
    }

    @Override
    public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments, InvocationType type) {
        ValueType[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; ++i) {
            if (!(paramTypes[i] instanceof ValueType.Primitive)) {
                escaping.add(arguments.get(i).getIndex());
            }
        }
        if (instance != null) {
            escaping.add(instance.getIndex());
        }
        if (receiver != null && !(method.getReturnType() instanceof ValueType.Primitive)) {
            escaping.add(receiver.getIndex());
        }
    }

    @Override
    public void nullCheck(VariableReader receiver, VariableReader value) {
        connect(value.getIndex(), receiver.getIndex());
    }

    @Override
    public void monitorEnter(VariableReader objectRef) {
        escaping.add(objectRef.getIndex());
    }

    @Override
    public void monitorExit(VariableReader objectRef) {
        escaping.add(exceptionIndex);
    }
}
