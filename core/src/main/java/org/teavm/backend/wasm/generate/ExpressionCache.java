/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.backend.wasm.generate;

import java.util.List;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.model.TextLocation;

class ExpressionCache {
    private TemporaryVariablePool tmpVars;

    ExpressionCache(TemporaryVariablePool tmpVars) {
        this.tmpVars = tmpVars;
    }

    public CachedExpression create(WasmExpression expr, WasmType type, TextLocation location,
            List<WasmExpression> body) {
        if (expr instanceof WasmGetLocal) {
            var getLocalExpr = (WasmGetLocal) expr;
            return new LocalVarCachedExpression(getLocalExpr.getLocal());
        } else if (expr instanceof WasmInt32Constant) {
            var constExpr = (WasmInt32Constant) expr;
            return new Int32CachedExpression(constExpr.getValue());
        } else {
            var tmpVar = tmpVars.acquire(type);
            var storeExpr = new WasmSetLocal(tmpVar, expr);
            storeExpr.setLocation(location);
            body.add(storeExpr);
            return new TmpVarCachedExpression(tmpVar);
        }
    }

    private static class LocalVarCachedExpression extends CachedExpression {
        private final WasmLocal localVar;

        LocalVarCachedExpression(WasmLocal localVar) {
            this.localVar = localVar;
        }

        @Override
        WasmExpression expr() {
            return new WasmGetLocal(localVar);
        }
    }

    private class TmpVarCachedExpression extends CachedExpression {
        private final WasmLocal tmpVar;

        TmpVarCachedExpression(WasmLocal tmpVar) {
            this.tmpVar = tmpVar;
        }

        @Override
        WasmExpression expr() {
            return new WasmGetLocal(tmpVar);
        }

        @Override
        void release() {
            tmpVars.release(tmpVar);
        }
    }

    private static class Int32CachedExpression extends CachedExpression {
        private final int value;

        Int32CachedExpression(int value) {
            this.value = value;
        }

        @Override
        WasmExpression expr() {
            return new WasmInt32Constant(value);
        }
    }
}
