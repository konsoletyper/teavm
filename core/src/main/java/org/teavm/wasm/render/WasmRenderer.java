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
import org.teavm.wasm.model.WasmMemorySegment;
import org.teavm.wasm.model.WasmModule;
import org.teavm.wasm.model.WasmType;
import org.teavm.wasm.model.expression.WasmExpression;

public class WasmRenderer {
    private WasmRenderingVisitor visitor = new WasmRenderingVisitor();

    public WasmRenderer append(String text) {
        visitor.append(text);
        return this;
    }

    public WasmRenderer lf() {
        visitor.lf();
        return this;
    }

    public WasmRenderer indent() {
        visitor.indent();
        return this;
    }

    public WasmRenderer outdent() {
        visitor.outdent();
        return this;
    }

    public void render(WasmModule module) {
        visitor.open().append("module");
        renderMemory(module);
        for (WasmFunction function : module.getFunctions().values()) {
            if (function.getImportName() == null) {
                continue;
            }
            lf().renderImport(function);
        }
        for (WasmFunction function : module.getFunctions().values()) {
            if (function.getImportName() != null) {
                continue;
            }
            lf().render(function);
        }
        for (WasmFunction function : module.getFunctions().values()) {
            if (function.getExportName() == null) {
                continue;
            }
            lf().renderExport(function);
        }
        visitor.close().lf();
    }

    public void renderMemory(WasmModule module) {
        visitor.open().append("memory " + module.getMemorySize());
        for (WasmMemorySegment segment : module.getSegments()) {
            visitor.lf().open().append("segment " + segment.getLength());
            visitor.indent();
            for (int i = 0; i < segment.getLength(); i += 256) {
                visitor.lf().append("\"");
                byte[] part = segment.getData(i, Math.max(segment.getLength(), i + 256) - i);
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < part.length; ++j) {
                    int b = part[j] << 24 >>> 24;
                    if (b < ' ' || b > 126) {
                        sb.append("\\0x" + Character.forDigit(b >> 4, 16) + Character.forDigit(b & 0xF, 16));
                    } else if (b == '\\') {
                        sb.append("\\\\");
                    } else if (b == '"') {
                        sb.append("\\\"");
                    } else {
                        sb.append((char) b);
                    }
                }
                visitor.append(sb.toString()).append("\"");
            }
            visitor.outdent();
            visitor.close();
        }
        visitor.close().lf();
    }

    public void renderImport(WasmFunction function) {
        String importModule = function.getImportModule();
        if (importModule == null) {
            importModule = "";
        }
        visitor.open().append("import $" + function.getName() + " \"" + importModule + "\" "
                + "\"" + function.getImportName() + "\"");
        renderSignature(function);
        visitor.close();
    }

    public void renderExport(WasmFunction function) {
        visitor.open().append("export \"" + function.getExportName() + "\" $" + function.getName()).close();
    }

    public void render(WasmFunction function) {
        visitor.open().append("func $" + function.getName());
        renderSignature(function);

        int firstLocalVariable = function.getParameters().size();
        if (firstLocalVariable < function.getLocalVariables().size()) {
            visitor.lf().open().append("local");
            List<WasmLocal> locals = function.getLocalVariables().subList(firstLocalVariable,
                    function.getLocalVariables().size());
            for (WasmLocal local : locals) {
                visitor.append(" ").append(local.getType());
            }
            visitor.close();
        }
        for (WasmExpression part : function.getBody()) {
            visitor.line(part);
        }
        visitor.close().lf();
        visitor.clear();
    }

    private void renderSignature(WasmFunction function) {
        if (!function.getParameters().isEmpty()) {
            visitor.append(" ").open().append("param");
            for (WasmType type : function.getParameters()) {
                visitor.append(" ").append(type);
            }
            visitor.close();
        }
        if (function.getResult() != null) {
            visitor.append(" ").open().append("result ").append(function.getResult()).close();
        }
    }

    @Override
    public String toString() {
        return visitor.sb.toString();
    }
}
