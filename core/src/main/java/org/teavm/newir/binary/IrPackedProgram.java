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
package org.teavm.newir.binary;

import org.teavm.newir.decl.IrClass;
import org.teavm.newir.decl.IrField;
import org.teavm.newir.decl.IrFunction;
import org.teavm.newir.decl.IrGlobal;
import org.teavm.newir.decl.IrMethod;
import org.teavm.newir.expr.IrExpr;

public class IrPackedProgram {
    byte[] data;
    String[] strings;
    IrFunction[] functions;
    IrMethod[] methods;
    IrField[] fields;
    IrClass[] classes;
    IrGlobal[] globals;

    public IrPackedProgram(byte[] data, String[] strings, IrFunction[] functions, IrMethod[] methods,
            IrField[] fields, IrClass[] classes, IrGlobal[] globals) {
        this.data = data;
        this.strings = strings;
        this.functions = functions;
        this.methods = methods;
        this.fields = fields;
        this.classes = classes;
        this.globals = globals;
    }

    public String[] getStrings() {
        return strings != null ? strings.clone() : new String[0];
    }

    public IrFunction[] getFunctions() {
        return functions != null ? functions.clone() : new IrFunction[0];
    }

    public IrMethod[] getMethods() {
        return methods != null ? methods.clone() : new IrMethod[0];
    }

    public IrField[] getFields() {
        return fields != null ? fields.clone() : new IrField[0];
    }

    public IrClass[] getClasses() {
        return classes != null ? classes.clone() : new IrClass[0];
    }

    public IrGlobal[] getGlobals() {
        return globals != null ? globals.clone() : new IrGlobal[0];
    }

    public byte[] getData() {
        return data.clone();
    }
}
