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
package org.teavm.wasm.render;

import java.util.List;
import org.teavm.wasm.model.WasmFunction;
import org.teavm.wasm.model.WasmLocal;
import org.teavm.wasm.model.WasmType;
import org.teavm.wasm.model.expression.WasmExpression;

public class WasmRenderer {
    private WasmRenderingVisitor visitor = new WasmRenderingVisitor();

    public void render(WasmFunction function) {
        visitor.open().append("func " + function.getName());
        for (WasmType type : function.getParameters()) {
            visitor.append(" ").open().append("param ").append(type).close();
        }
        if (function.getResult() != null) {
            visitor.append(" ").open().append("result ").append(function.getResult()).close();
        }

        int firstLocalVariable = function.getParameters().size();
        if (firstLocalVariable > function.getLocalVariables().size()) {
            visitor.open().append("local");
            List<WasmLocal> locals = function.getLocalVariables().subList(firstLocalVariable,
                    function.getLocalVariables().size());
            for (WasmLocal local : locals) {
                visitor.append(" " + local.getType());
            }
            visitor.close();
        }
        for (WasmExpression part : function.getBody()) {
            visitor.line(part);
        }
        visitor.close().lf().lf();
    }

    @Override
    public String toString() {
        return visitor.sb.toString();
    }
}
