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

public class IrPackedTree {
    private byte[] data;
    private String[] strings;
    private IrFunction[] functions;
    private IrMethod[] methods;
    private IrField[] fields;
    private IrClass[] classes;
    private IrGlobal[] globals;

    IrPackedTree(byte[] data, String[] strings, IrFunction[] functions, IrMethod[] methods,
            IrField[] fields, IrClass[] classes, IrGlobal[] globals) {
        this.data = data;
        this.strings = strings;
        this.functions = functions;
        this.methods = methods;
        this.fields = fields;
        this.classes = classes;
        this.globals = globals;
    }

    public byte[] getData() {
        return data.clone();
    }
}
