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
package org.teavm.backend.c.analyze;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.interop.DelegateTo;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.ClassReader;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.ProgramReader;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.ExceptionHandlerDescriptor;

public class TypeCollector extends AbstractInstructionReader {
    private Set<ValueType> types = new HashSet<>();

    public Set<? extends ValueType> getTypes() {
        return types;
    }

    public void collect(ListableClassReaderSource classSource) {
        for (String className : classSource.getClassNames()) {
            ClassReader cls = classSource.get(className);
            collect(cls);
        }
    }

    public void collectFromCallSites(List<CallSiteDescriptor> callSites) {
        for (CallSiteDescriptor callSite : callSites) {
            for (ExceptionHandlerDescriptor handler : callSite.getHandlers()) {
                if (handler.getClassName() != null) {
                    types.add(ValueType.object(handler.getClassName()));
                }
            }
        }
    }

    private void collect(ClassReader cls) {
        for (MethodReader method : cls.getMethods()) {
            collect(method);
            types.add(ValueType.object(cls.getName()));
        }
    }

    private void collect(MethodReader method) {
        if (method.getAnnotations().get(DelegateTo.class.getName()) != null) {
            return;
        }

        if (method.getProgram() != null) {
            collect(method.getProgram());
        }
    }

    private void collect(ProgramReader program) {
        for (BasicBlockReader block : program.getBasicBlocks()) {
            block.readAllInstructions(this);
        }
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, List<? extends VariableReader> dimensions) {
        ValueType type = itemType;
        for (int i = 1; i <= dimensions.size(); ++i) {
            type = ValueType.arrayOf(type);
        }
        addType(type);
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
        addType(ValueType.arrayOf(itemType));
    }

    @Override
    public void classConstant(VariableReader receiver, ValueType cst) {
        addType(cst);
    }

    @Override
    public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
        addType(type);
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
        addType(targetType);
    }

    private void addType(ValueType type) {
        types.add(type);
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array) type).getItemType();
            if (!types.add(type)) {
                break;
            }
        }
    }
}
