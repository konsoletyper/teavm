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
package org.teavm.newir.jvm;

import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.newir.decl.IrAttribute;
import org.teavm.newir.decl.IrClass;
import org.teavm.newir.decl.IrMethod;

public final class JvmAttr {
    private JvmAttr() {
    }

    public static final IrAttribute<String, IrClass> CLASS_NAME = new IrAttribute<>(null);
    public static final IrAttribute<MethodDescriptor, IrMethod> METHOD_DESC = new IrAttribute<>(null);
    public static final IrAttribute<MethodReference, IrMethod> METHOD_REF = new IrAttribute<>(null);
}
