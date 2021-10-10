/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.newir.decl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.teavm.newir.type.IrType;

public final class IrClass extends IrDeclaration<IrClass> {
    private IrClass superclass;
    private IrClass[] interfaces;
    private Map<IrMethod, IrFunction> methods = new LinkedHashMap<>();
    private List<IrField> fields = new ArrayList<>();
    private IrFunction initializer;
    private IrObjectType type;
    private Consumer<IrClass> contentInitializer;

    public IrClass() {
        this(null);
    }

    public IrClass(Consumer<IrClass> contentInitializer) {
        this.contentInitializer = contentInitializer;
    }

    private void initialize() {
        if (contentInitializer != null) {
            contentInitializer.accept(this);
            contentInitializer = null;
        }
    }

    public IrClass getSuperclass() {
        return superclass;
    }

    public void setSuperclass(IrClass superclass) {
        this.superclass = superclass;
    }

    public int getInterfaceCount() {
        return interfaces != null ? interfaces.length : 0;
    }

    public IrClass getInterface(int index) {
        if (interfaces == null) {
            throw new IndexOutOfBoundsException();
        }
        return interfaces[index];
    }

    public void setInterfaces(IrClass... interfaces) {
        this.interfaces = interfaces.length == 0 ? null : interfaces.clone();
    }

    public IrFunction getInitializer() {
        return initializer;
    }

    public void setInitializer(IrFunction initializer) {
        this.initializer = initializer;
    }

    public IrFunction getMethodImplementation(IrMethod method) {
        initialize();
        return methods.get(method);
    }

    public void setMethodImplementation(IrMethod method, IrFunction function) {
        initialize();
        methods.put(method, function);
    }

    public void removeMethodImplementation(IrMethod method) {
        initialize();
        methods.remove(method);
    }

    public boolean hasMethod(IrMethod method) {
        initialize();
        return methods.containsKey(method);
    }

    public Collection<? extends IrMethod> getMethods() {
        initialize();
        return methods.keySet();
    }

    public List<? extends IrField> getFields() {
        initialize();
        return fields;
    }

    public IrField createField(IrType type) {
        initialize();
        IrField field = new IrField(this, type);
        fields.add(field);
        return field;
    }

    public void removeField(IrField field) {
        initialize();
        if (fields.remove(field)) {
            field.declaringClass = null;
        }
    }

    public IrObjectType asType() {
        if (type == null) {
            type = new IrObjectType(this);
        }
        return type;
    }
}
