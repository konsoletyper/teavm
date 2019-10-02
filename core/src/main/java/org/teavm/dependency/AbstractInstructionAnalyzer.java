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

import java.util.List;
import java.util.Objects;
import org.teavm.model.CallLocation;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodReference;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.instructions.InvocationType;

abstract class AbstractInstructionAnalyzer extends AbstractInstructionReader {
    private static final MethodReference STRING_INIT_FROM_CHARS_METHOD = new MethodReference(String.class,
            "<init>", char[].class, void.class);
    static final MethodReference CLONE_METHOD = new MethodReference(Object.class, "clone", Object.class);
    private static final MethodReference NPE_INIT_METHOD = new MethodReference(NullPointerException.class,
            "<init>", void.class);
    private static final MethodReference AIOOB_INIT_METHOD = new MethodReference(ArrayIndexOutOfBoundsException.class,
            "<init>", void.class);
    static final MethodReference MONITOR_ENTER_METHOD = new MethodReference(Object.class,
            "monitorEnter", Object.class, void.class);
    static final MethodReference MONITOR_ENTER_SYNC_METHOD = new MethodReference(Object.class,
            "monitorEnterSync", Object.class, void.class);
    static final MethodReference MONITOR_EXIT_METHOD = new MethodReference(Object.class,
            "monitorExit", Object.class, void.class);
    static final MethodReference MONITOR_EXIT_SYNC_METHOD = new MethodReference(Object.class,
            "monitorExitSync", Object.class, void.class);

    protected TextLocation location;
    protected MethodReference caller;
    protected CallLocation callLocation;

    public void setCaller(MethodReference caller) {
        this.caller = caller;
        callLocation = null;
    }

    @Override
    public void location(TextLocation location) {
        if (!Objects.equals(this.location, location)) {
            this.location = location;
            callLocation = null;
        }
    }

    @Override
    public void classConstant(VariableReader receiver, ValueType cst) {
        DependencyNode node = getNode(receiver);
        if (node != null) {
            node.propagate(getAnalyzer().getType("java.lang.Class"));
            if (!(cst instanceof ValueType.Primitive)) {
                StringBuilder sb = new StringBuilder();
                if (cst instanceof ValueType.Object) {
                    sb.append(((ValueType.Object) cst).getClassName());
                } else {
                    sb.append(cst.toString());
                }
                node.getClassValueNode().propagate(getAnalyzer().getType(sb.toString()));
            } else {
                node.getClassValueNode().propagate(getAnalyzer().getType("~" + cst.toString()));
            }
        }
        while (cst instanceof ValueType.Array) {
            cst = ((ValueType.Array) cst).getItemType();
        }
        if (cst instanceof ValueType.Object) {
            String className = ((ValueType.Object) cst).getClassName();
            getAnalyzer().linkClass(className);
        }
    }

    @Override
    public void stringConstant(VariableReader receiver, String cst) {
        DependencyNode node = getNode(receiver);
        if (node != null) {
            node.propagate(getAnalyzer().getType("java.lang.String"));
        }
        MethodDependency method = getAnalyzer().linkMethod(STRING_INIT_FROM_CHARS_METHOD);
        method.addLocation(getCallLocation());
        method.use();
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
        DependencyNode node = getNode(receiver);
        if (node != null) {
            node.propagate(getAnalyzer().getType("[" + itemType));
        }
        String className = extractClassName(itemType);
        if (className != null) {
            getAnalyzer().linkClass(className);
        }
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, List<? extends VariableReader> dimensions) {
        DependencyNode node = getNode(receiver);
        for (int i = 0; i < dimensions.size(); ++i) {
            if (node == null) {
                break;
            }
            String itemTypeStr;
            if (itemType instanceof ValueType.Object) {
                itemTypeStr = ((ValueType.Object) itemType).getClassName();
            } else {
                itemTypeStr = itemType.toString();
            }
            node.propagate(getAnalyzer().getType(itemTypeStr));
            node = node.getArrayItem();
            itemType = ((ValueType.Array) itemType).getItemType();
        }
        String className = extractClassName(itemType);
        if (className != null) {
            getAnalyzer().linkClass(className);
        }
    }

    protected final String extractClassName(ValueType itemType) {
        while (itemType instanceof ValueType.Array) {
            itemType = ((ValueType.Array) itemType).getItemType();
        }
        return itemType instanceof ValueType.Object ? ((ValueType.Object) itemType).getClassName() : null;
    }

    @Override
    public void create(VariableReader receiver, String type) {
        getAnalyzer().linkClass(type);
        DependencyNode node = getNode(receiver);
        if (node != null) {
            node.propagate(getAnalyzer().getType(type));
        }
    }

    @Override
    public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
            ValueType fieldType) {
        FieldDependency fieldDep = getAnalyzer().linkField(field);
        fieldDep.addLocation(getCallLocation());
        if (!(fieldType instanceof ValueType.Primitive)) {
            DependencyNode receiverNode = getNode(receiver);
            if (receiverNode != null) {
                fieldDep.getValue().connect(receiverNode);
            }
        }
        touchField(instance, fieldDep, field);
    }

    @Override
    public void putField(VariableReader instance, FieldReference field, VariableReader value,
            ValueType fieldType) {
        FieldDependency fieldDep = getAnalyzer().linkField(field);
        fieldDep.addLocation(getCallLocation());
        if (!(fieldType instanceof ValueType.Primitive)) {
            DependencyNode valueNode = getNode(value);
            if (valueNode != null) {
                valueNode.connect(fieldDep.getValue());
            }
        }
        touchField(instance, fieldDep, field);
    }

    private void touchField(VariableReader instance, FieldDependency fieldDep, FieldReference field) {
        if (instance == null) {
            if (fieldDep.getField() != null) {
                initClass(fieldDep.getField().getOwnerName());
            }
        } else {
            getAnalyzer().linkClass(field.getClassName());
        }
    }

    @Override
    public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments, InvocationType type) {
        if (instance == null) {
            invokeSpecial(receiver, null, method, arguments);
        } else {
            switch (type) {
                case SPECIAL:
                    invokeSpecial(receiver, instance, method, arguments);
                    break;
                case VIRTUAL:
                    invokeVirtual(receiver, instance, method, arguments);
                    break;
            }
        }
    }

    protected abstract void invokeSpecial(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments);

    protected abstract void invokeVirtual(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments);

    @Override
    public void invokeDynamic(VariableReader receiver, VariableReader instance, MethodDescriptor method,
            List<? extends VariableReader> arguments, MethodHandle bootstrapMethod,
            List<RuntimeConstant> bootstrapArguments) {
        // Should be eliminated by processInvokeDynamic method
    }

    @Override
    public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
        String className = extractClassName(type);
        if (className != null) {
            getAnalyzer().linkClass(className);
        }
    }

    @Override
    public void initClass(String className) {
        getAnalyzer().linkClass(className).initClass(getCallLocation());
    }

    @Override
    public void nullCheck(VariableReader receiver, VariableReader value) {
        DependencyNode valueNode = getNode(value);
        DependencyNode receiverNode = getNode(receiver);
        if (valueNode != null) {
            valueNode.connect(receiverNode);
        }
        MethodDependency npeMethod = getAnalyzer().linkMethod(NPE_INIT_METHOD);
        npeMethod.addLocation(getCallLocation());
        npeMethod.use();
    }

    @Override
    public void monitorEnter(VariableReader objectRef) {
        if (getAnalyzer().asyncSupported) {
            MethodDependency methodDep = getAnalyzer().linkMethod(MONITOR_ENTER_METHOD);
            methodDep.addLocation(getCallLocation());
            getNode(objectRef).connect(methodDep.getVariable(1));
            methodDep.use();
        }

        MethodDependency methodDep = getAnalyzer().linkMethod(MONITOR_ENTER_SYNC_METHOD);
        methodDep.addLocation(getCallLocation());
        getNode(objectRef).connect(methodDep.getVariable(1));
        methodDep.use();
    }

    @Override
    public void monitorExit(VariableReader objectRef) {
        if (getAnalyzer().asyncSupported) {
            MethodDependency methodDep = getAnalyzer().linkMethod(MONITOR_EXIT_METHOD);
            methodDep.addLocation(getCallLocation());
            getNode(objectRef).connect(methodDep.getVariable(1));
            methodDep.use();
        }

        MethodDependency methodDep = getAnalyzer().linkMethod(MONITOR_EXIT_SYNC_METHOD);
        methodDep.addLocation(getCallLocation());
        getNode(objectRef).connect(methodDep.getVariable(1));
        methodDep.use();
    }

    @Override
    public void boundCheck(VariableReader receiver, VariableReader index, VariableReader array, boolean lower) {
        MethodDependency methodDep = getAnalyzer().linkMethod(AIOOB_INIT_METHOD);
        methodDep.addLocation(getCallLocation());
        methodDep.getVariable(0).propagate(getAnalyzer().getType(ArrayIndexOutOfBoundsException.class.getName()));
        methodDep.use();
    }

    protected abstract DependencyNode getNode(VariableReader variable);

    protected abstract DependencyAnalyzer getAnalyzer();

    protected CallLocation getCallLocation() {
        if (callLocation == null) {
            callLocation = new CallLocation(caller, location);
        }
        return callLocation;
    }
}
