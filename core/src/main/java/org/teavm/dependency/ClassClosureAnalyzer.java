/*
 *  Copyright 2019 Alexey Andreev.
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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.instructions.InvocationType;

class ClassClosureAnalyzer extends AbstractInstructionReader {
    private ClassReaderSource classSource;
    private Set<String> result = new LinkedHashSet<>();

    ClassClosureAnalyzer(ClassReaderSource classSource) {
        this.classSource = classSource;
    }

    static Set<? extends String> build(ClassReaderSource classSource,
            Collection<? extends String> initialClasses) {
        ClassClosureAnalyzer analyzer = new ClassClosureAnalyzer(classSource);
        for (String className : initialClasses) {
            analyzer.build(className);
        }
        return analyzer.result;
    }

    void build(String className) {
        if (!result.add(className)) {
            return;
        }

        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return;
        }

        if (cls.getParent() != null) {
            build(cls.getParent());
        }
        for (String itf : cls.getInterfaces()) {
            build(itf);
        }

        for (FieldReader field : cls.getFields()) {
            build(field.getType());
        }
        for (MethodReader method : cls.getMethods()) {
            build(method.getResultType());
            for (ValueType paramType : method.getParameterTypes()) {
                build(paramType);
            }
            if (method.getProgram() != null) {
                for (BasicBlockReader block : method.getProgram().getBasicBlocks()) {
                    block.readAllInstructions(this);
                }
            }
        }
    }

    void build(ValueType type) {
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array) type).getItemType();
        }
        if (type instanceof ValueType.Object) {
            build(((ValueType.Object) type).getClassName());
        }
    }
    @Override
    public void classConstant(VariableReader receiver, ValueType cst) {
        build(cst);
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
        build(targetType);
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
        build(itemType);
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType,
            List<? extends VariableReader> dimensions) {
        build(itemType);
    }

    @Override
    public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
            ValueType fieldType) {
        build(fieldType);
    }

    @Override
    public void putField(VariableReader instance, FieldReference field, VariableReader value, ValueType fieldType) {
        build(fieldType);
    }

    @Override
    public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments, InvocationType type) {
        build(method.getReturnType());
        for (ValueType paramType : method.getParameterTypes()) {
            build(paramType);
        }
    }

    @Override
    public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
        build(type);
    }
}
