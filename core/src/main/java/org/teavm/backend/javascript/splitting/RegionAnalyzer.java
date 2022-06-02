/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.javascript.splitting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import org.teavm.callgraph.CallGraph;
import org.teavm.callgraph.CallGraphNode;
import org.teavm.callgraph.CallSite;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ProgramReader;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.instructions.InvocationType;

public class RegionAnalyzer {
    private static final MethodDescriptor LAUNCHER = new MethodDescriptor(CodeFragmentTransformer.LAUNCHER_NAME,
            void.class);

    private ClassReaderSource classes;
    private CallGraph callGraph;
    private Map<CallGraphNode, NodeInfo> nodeInfoMap = new LinkedHashMap<>();
    private Map<FieldReference, NodeInfo> nodeInfoByField = new LinkedHashMap<>();
    private Map<String, NodeInfo> nodeInfoByClass = new LinkedHashMap<>();
    private Map<String, NodeInfo> nodeInfoByStringConstant = new LinkedHashMap<>();
    private List<NodeInfo> nodeInfoList = new ArrayList<>();
    private Queue<Step> queue = new ArrayDeque<>();
    private List<Region> regions = new ArrayList<>();
    private List<Step> deferredSteps = new ArrayList<>();
    private Map<BitSet, Part> parts = new LinkedHashMap<>();
    private boolean splitCalled;

    public RegionAnalyzer(ClassReaderSource classes, CallGraph callGraph) {
        this.classes = classes;
        this.callGraph = callGraph;
    }

    public void analyze(MethodReference entryPoint) {
        mark(entryPoint);
        buildParts();
    }

    public boolean hasSplitting() {
        return splitCalled;
    }

    public Collection<? extends Part> getParts() {
        return parts.values();
    }

    public NodeInfo getNodeInfo(MethodReference method) {
        CallGraphNode node = callGraph.getNode(method);
        if (node == null) {
            return null;
        }
        return nodeInfoMap.get(node);
    }

    public boolean isSplit(MethodReference method) {
        CallGraphNode node = callGraph.getNode(method);
        if (node == null) {
            return false;
        }
        NodeInfo info = nodeInfoMap.get(node);
        return info != null && info.split;
    }

    public boolean isShared(MethodReference method) {
        CallGraphNode node = callGraph.getNode(method);
        if (node == null) {
            return false;
        }
        NodeInfo info = nodeInfoMap.get(node);
        return info != null && info.shared;
    }

    public NodeInfo getNodeInfo(FieldReference field) {
        return nodeInfoByField.get(field);
    }

    public NodeInfo getNodeInfo(String className) {
        return nodeInfoByClass.get(className);
    }

    public NodeInfo getNodeInfoByStringConstant(String string) {
        return nodeInfoByStringConstant.get(string);
    }

    private void mark(MethodReference entryPoint) {
        CallGraphNode node = callGraph.getNode(entryPoint);
        if (node == null) {
            return;
        }

        Region firstRegion = createRegion(null);
        NodeInfo firstInfo = getInfo(node);
        firstInfo.startingRegion = firstRegion;
        deferredSteps.add(new Step(firstInfo, firstRegion));

        while (!deferredSteps.isEmpty()) {
            queue.addAll(deferredSteps);
            for (Step step : deferredSteps) {
                step.nodeInfo.deferred = false;
            }
            deferredSteps.clear();
            processQueue();
        }
    }

    private void processQueue() {
        while (!queue.isEmpty()) {
            Step step = queue.remove();
            if (step.nodeInfo.deferred || step.nodeInfo.regions.get(step.region.id)) {
                continue;
            }
            if (step.region.predecessors.intersects(step.nodeInfo.regions)) {
                step.nodeInfo.shared = true;
                continue;
            }

            CallGraphNode cgNode = step.nodeInfo.node;
            if (cgNode != null) {
                processCalledNode(cgNode, step);
                ClassReader owner = classes.get(cgNode.getMethod().getClassName());
                if (owner != null) {
                    MethodReader method = owner.getMethod(cgNode.getMethod().getDescriptor());
                    if (method != null && method.getProgram() != null) {
                        processCalledMethodBody(method.getProgram(), step.region);
                    }
                }
            }
        }
    }

    private void processCalledNode(CallGraphNode calledNode, Step step) {
        NodeInfo calledNodeInfo = getInfo(calledNode);
        if (calledNodeInfo.deferred || step.region.predecessors.intersects(calledNodeInfo.regions)) {
            return;
        }

        if (isFragmentCall(calledNode.getMethod())) {
            Region newRegion = createRegion(step.region);
            Step nextStep = new Step(calledNodeInfo, createRegion(step.region));
            calledNodeInfo.deferred = true;
            calledNodeInfo.split = true;
            deferredSteps.add(nextStep);
            splitCalled = true;
        } else {
            queue.add(new Step(calledNodeInfo, step.region));
        }
    }

    private boolean isFragmentCall(MethodReference method) {
        if (!method.getName().equals(CodeFragmentTransformer.LAUNCHER_NAME)) {
            return false;
        }
        ClassReader cls = classes.get(method.getClassName());
        if (cls == null) {
            return false;
        }

        MethodReader methodReader = cls.getMethod(LAUNCHER);
        if (methodReader == null) {
            return false;
        }

        return methodReader.getAnnotations().get(AsyncLauncher.class.getName()) != null;
    }

    private void processCalledMethodBody(ProgramReader program, Region region) {
        for (BasicBlockReader block : program.getBasicBlocks()) {
            block.readAllInstructions(new AbstractInstructionReader() {
                @Override
                public void initClass(String className) {
                    markClass(className);
                }

                @Override
                public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                        List<? extends VariableReader> arguments, InvocationType type) {
                    if (instance == null) {
                        markClass(method.getClassName());
                    }
                }

                @Override
                public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                        ValueType fieldType) {
                    if (instance == null) {
                        markField(field);
                    }
                }

                @Override
                public void putField(VariableReader instance, FieldReference field, VariableReader value,
                        ValueType fieldType) {
                    if (instance == null) {
                        markField(field);
                    }
                }

                @Override
                public void classConstant(VariableReader receiver, ValueType cst) {
                    markType(cst);
                }

                @Override
                public void stringConstant(VariableReader receiver, String cst) {
                    markString(cst);
                }

                @Override
                public void create(VariableReader receiver, String type) {
                    markClass(type);
                }

                @Override
                public void createArray(VariableReader receiver, ValueType itemType,
                        List<? extends VariableReader> dimensions) {
                    markType(itemType);
                }

                @Override
                public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
                    markType(itemType);
                }

                @Override
                public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
                    markType(type);
                }

                @Override
                public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
                    markType(targetType);
                }

                private void markField(FieldReference field) {
                    getInfo(field).regions.set(region.id);
                    markClass(field.getClassName());
                }

                private void markType(ValueType type) {
                    while (type instanceof ValueType.Array) {
                        type = ((ValueType.Array) type).getItemType();
                    }
                    if (type instanceof ValueType.Object) {
                        markClass(((ValueType.Object) type).getClassName());
                    }
                }

                private void markClass(String className) {
                    NodeInfo info = getInfo(className);
                    if (!info.regions.get(region.id)) {
                        info.regions.set(region.id);
                        markString(className);
                        ClassReader cls = classes.get(className);
                        if (cls != null) {
                            if (cls.getParent() != null) {
                                markClass(cls.getParent());
                            }
                            for (String itf : cls.getInterfaces()) {
                                markClass(itf);
                            }
                        }
                    }
                }

                private void markString(String string) {
                    getInfoByStringConstant(string).regions.set(region.id);
                }
            });
        }
    }

    private void buildParts() {
        for (NodeInfo nodeInfo : nodeInfoList) {
            Part part = parts.computeIfAbsent(nodeInfo.regions, bs -> new Part(bs, parts.size()));
            for (int i = nodeInfo.regions.nextSetBit(0); i >= 0; i = nodeInfo.regions.nextSetBit(i + 1)) {
                regions.get(i).parts.add(part);
            }
            nodeInfo.regions = null;
            nodeInfo.mainPart = part;
        }
    }

    private NodeInfo getInfo(CallGraphNode node) {
        return nodeInfoMap.computeIfAbsent(node, n -> {
            NodeInfo info = createNodeInfo();
            info.node = n;
            return info;
        });
    }

    private NodeInfo getInfo(FieldReference field) {
        return nodeInfoByField.computeIfAbsent(field, f -> createNodeInfo());
    }

    private NodeInfo getInfo(String className) {
        return nodeInfoByClass.computeIfAbsent(className, c -> createNodeInfo());
    }

    private NodeInfo getInfoByStringConstant(String string) {
        return nodeInfoByStringConstant.computeIfAbsent(string, c -> createNodeInfo());
    }

    private NodeInfo createNodeInfo() {
        NodeInfo info = new NodeInfo();
        nodeInfoList.add(info);
        return info;
    }

    private Region createRegion(Region parent) {
        Region region = new Region(regions.size(), parent);
        regions.add(region);
        return region;
    }

    public static class NodeInfo {
        CallGraphNode node;
        Region startingRegion;
        BitSet regions = new BitSet();
        boolean shared;
        Part mainPart;
        boolean deferred;
        boolean split;

        void mark(Region region) {
            if (region.predecessors.intersects(regions)) {
                shared = true;
            } else {
                regions.set(region.id);
            }
        }

        public Part getPart() {
            return mainPart;
        }

        public boolean isShared() {
            return shared;
        }
    }

    static class Step {
        NodeInfo nodeInfo;
        Region region;

        Step(NodeInfo nodeInfo, Region region) {
            this.nodeInfo = nodeInfo;
            this.region = region;
        }
    }

    static class Region {
        int id;
        Region parent;
        final BitSet predecessors = new BitSet();
        Set<Part> parts = new HashSet<>();

        Region(int id, Region parent) {
            this.id = id;
            this.parent = parent;
            if (parent != null) {
                predecessors.or(parent.predecessors);
            }
            predecessors.set(id);
        }
    }

    public static class Part {
        BitSet regions;
        private int index;

        Part(BitSet regions, int index) {
            this.regions = regions;
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }
}
