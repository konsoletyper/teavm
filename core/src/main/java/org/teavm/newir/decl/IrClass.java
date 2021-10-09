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
import org.teavm.newir.type.IrType;

public final class IrClass extends IrDeclaration<IrClass> {
    private IrClass superclass;
    private Map<IrMethod, IrFunction> methods = new LinkedHashMap<>();
    private List<IrField> fields = new ArrayList<>();
    private IrFunction initializer;
    private IrObjectType type;

    public IrClass getSuperclass() {
        return superclass;
    }

    public void setSuperclass(IrClass superclass) {
        this.superclass = superclass;
    }

    public IrFunction getInitializer() {
        return initializer;
    }

    public void setInitializer(IrFunction initializer) {
        this.initializer = initializer;
    }

    public IrFunction getMethodImplementation(IrMethod method) {
        return methods.get(method);
    }

    public void setMethodImplementation(IrMethod method, IrFunction function) {
        methods.put(method, function);
    }

    public void removeMethodImplementation(IrMethod method) {
        methods.remove(method);
    }

    public boolean hasMethod(IrMethod method) {
        return methods.containsKey(method);
    }

    public Collection<? extends IrMethod> getMethods() {
        return methods.keySet();
    }

    public List<? extends IrField> getFields() {
        return fields;
    }

    public IrField createField(IrType type) {
        return new IrField(this, type);
    }

    public void removeField(IrField field) {
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
