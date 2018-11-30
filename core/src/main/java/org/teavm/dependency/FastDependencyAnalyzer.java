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
package org.teavm.dependency;

import static org.teavm.dependency.AbstractInstructionAnalyzer.MONITOR_ENTER_METHOD;
import static org.teavm.dependency.AbstractInstructionAnalyzer.MONITOR_ENTER_SYNC_METHOD;
import static org.teavm.dependency.AbstractInstructionAnalyzer.MONITOR_EXIT_METHOD;
import static org.teavm.dependency.AbstractInstructionAnalyzer.MONITOR_EXIT_SYNC_METHOD;
import java.util.HashMap;
import java.util.Map;
import org.teavm.common.ServiceRepository;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ProgramReader;
import org.teavm.model.TryCatchBlockReader;
import org.teavm.model.ValueType;

public class FastDependencyAnalyzer extends DependencyAnalyzer {
    DependencyNode instancesNode;
    DependencyNode classesNode;
    private Map<MethodReference, FastVirtualCallConsumer> virtualCallConsumers = new HashMap<>();
    private Map<String, DependencyNode> subtypeNodes = new HashMap<>();

    public FastDependencyAnalyzer(ClassReaderSource classSource, ClassLoader classLoader,
            ServiceRepository services, Diagnostics diagnostics) {
        super(classSource, classLoader, services, diagnostics);

        instancesNode = new DependencyNode(this, null);
        classesNode = new DependencyNode(this, null);

        instancesNode.addConsumer(type -> {
            getSubtypeNode(type.getName()).propagate(type);
        });
    }

    @Override
    protected void processMethod(MethodDependency methodDep) {
        MethodReader method = methodDep.getMethod();
        ProgramReader program = method.getProgram();

        if (program != null) {
            FastInstructionAnalyzer instructionAnalyzer = new FastInstructionAnalyzer(this);
            instructionAnalyzer.setCaller(method.getReference());
            for (BasicBlockReader block : program.getBasicBlocks()) {
                block.readAllInstructions(instructionAnalyzer);

                for (TryCatchBlockReader tryCatch : block.readTryCatchBlocks()) {
                    if (tryCatch.getExceptionType() != null) {
                        linkClass(tryCatch.getExceptionType());
                    }
                }
            }

            methodDep.variableNodes = new DependencyNode[program.variableCount()];
            for (int i = 0; i < methodDep.variableNodes.length; ++i) {
                methodDep.variableNodes[i] = instancesNode;
            }
        }

        if (method.hasModifier(ElementModifier.SYNCHRONIZED)) {
            processAsyncMethod(methodDep);
        }
    }

    private void processAsyncMethod(MethodDependency methodDep) {
        if (asyncSupported) {
            linkMethod(MONITOR_ENTER_METHOD).use();
        }

        linkMethod(MONITOR_ENTER_SYNC_METHOD).use();

        if (asyncSupported) {
            linkMethod(MONITOR_EXIT_METHOD).use();
        }

        linkMethod(MONITOR_EXIT_SYNC_METHOD).use();
    }

    @Override
    DependencyNode createParameterNode(MethodReference method, ValueType type, int index) {
        return instancesNode;
    }

    @Override
    DependencyNode createResultNode(MethodReference method) {
        return instancesNode;
    }

    @Override
    DependencyNode createThrownNode(MethodReference method) {
        return instancesNode;
    }

    @Override
    DependencyNode createFieldNode(FieldReference field, ValueType type) {
        return instancesNode;
    }

    @Override
    DependencyNode createArrayItemNode(DependencyNode parent) {
        return instancesNode;
    }

    @Override
    DependencyNode createClassValueNode(int degree, DependencyNode parent) {
        return classesNode;
    }

    private DependencyNode getSubtypeNode(String type) {
        if (type.equals("java.lang.Object")) {
            return instancesNode;
        }
        return subtypeNodes.computeIfAbsent(type, key -> {
            DependencyNode node = createNode();

            defer(() -> {
                int degree = 0;
                while (degree < key.length() && key.charAt(degree) == '[') {
                    degree++;
                }

                if (degree > 0) {
                    ValueType fullType = ValueType.parse(key);
                    if (fullType instanceof ValueType.Object) {
                        String prefix = key.substring(0, degree) + "L";
                        String className = ((ValueType.Object) fullType).getClassName();
                        ClassReader cls = getClassSource().get(key);
                        if (cls != null) {
                            if (cls.getParent() != null) {
                                node.connect(getSubtypeNode(prefix + cls.getParent().replace('.', '/') + ";"));
                            } else {
                                node.connect(getSubtypeNode("java.lang.Object"));
                            }
                            for (String itf : cls.getInterfaces()) {
                                node.connect(getSubtypeNode(prefix + itf.replace('.', '/') + ";"));
                            }
                        }
                    } else {
                        node.connect(getSubtypeNode("java.lang.Object"));
                    }
                } else {
                    ClassReader cls = getClassSource().get(key);
                    if (cls != null) {
                        if (cls.getParent() != null) {
                            node.connect(getSubtypeNode(cls.getParent()));
                        }
                        for (String itf : cls.getInterfaces()) {
                            node.connect(getSubtypeNode(itf));
                        }
                    }
                }
            });

            return node;
        });
    }

    FastVirtualCallConsumer getVirtualCallConsumer(MethodReference method) {
        return virtualCallConsumers.computeIfAbsent(method, key -> {
            FastVirtualCallConsumer consumer = new FastVirtualCallConsumer(instancesNode, key.getDescriptor(), this);
            defer(() -> {
                getSubtypeNode(method.getClassName()).addConsumer(consumer);
            });
            return consumer;
        });
    }

    @Override
    boolean domainOptimizationEnabled() {
        return false;
    }
}
