/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.wasm.intrinsics;

import org.teavm.ast.Expr;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.generate.WasmStringPool;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.FieldReference;

public interface WasmIntrinsicManager {
    WasmExpression generate(Expr expr);

    BinaryWriter getBinaryWriter();

    WasmStringPool getStringPool();

    Diagnostics getDiagnostics();

    NameProvider getNames();

    WasmLocal getTemporary(WasmType type);

    int getStaticField(FieldReference field);

    void releaseTemporary(WasmLocal local);
}
