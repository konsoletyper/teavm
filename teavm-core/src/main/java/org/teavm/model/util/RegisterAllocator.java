/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.model.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.common.DisjointSet;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class RegisterAllocator {
    public int[] allocateRegisters(MethodHolder method, LivenessAnalyzer liveness) {
        InterferenceGraphBuilder interferenceBuilder = new InterferenceGraphBuilder();
        Graph interferenceGraph = interferenceBuilder.build(method.getProgram(), liveness);
        int[] congruenceClasses = buildPhiCongruenceClasses(method.getProgram());
        int[] colors = new int[method.getProgram().variableCount() * 2];
        Arrays.fill(colors, -1);
        for (int i = 0; i < method.parameterCount(); ++i) {
            colors[i] = i;
        }
        GraphBuilder graphBuilder = new GraphBuilder();
        Program program = method.getProgram();
        for (int v = 0; v < interferenceGraph.size(); ++v) {
            for (int w : interferenceGraph.outgoingEdges(v)) {
                if (w <= v) {
                    continue;
                }
                if (congruenceClasses[v] == congruenceClasses[w]) {

                }
            }
        }
        GraphColorer colorer = new GraphColorer();
        colorer.colorize(interferenceGraph, colors);
        return colors;
    }

    private static class PhiArgumentCopy {
        Incoming incoming;
        int original;
    }

    private List<PhiArgumentCopy> insertPhiArgumentsCopies(Program program) {
        List<PhiArgumentCopy> copies = new ArrayList<>();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            for (Phi phi : program.basicBlockAt(i).getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {

                }
            }
        }
        return copies;
    }

    private int[] buildPhiCongruenceClasses(Program program) {
        DisjointSet classes = new DisjointSet();
        for (int i = 0; i < program.variableCount(); ++i) {
            classes.create();
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    classes.union(phi.getReceiver().getIndex(), incoming.getValue().getIndex());
                }
            }
        }
        return classes.pack(program.variableCount());
    }
}
