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
package org.teavm.backend.wasm.render;

import java.util.List;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmMemorySegment;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.expression.WasmExpression;

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

    public boolean isLineNumbersEmitted() {
        return visitor.lineNumbersEmitted;
    }

    public void setLineNumbersEmitted(boolean value) {
        visitor.lineNumbersEmitted = value;
    }

    public void render(WasmModule module) {
        visitor.open().append("module");
        renderMemory(module);
        renderTypes(module);

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
        renderTable(module);
        if (module.getStartFunction() != null) {
            visitor.lf().open().append("start $" + module.getStartFunction().getName()).close().lf();
        }
        visitor.close().lf();
    }

    public void renderMemory(WasmModule module) {
        visitor.lf();
        visitor.open().append("memory " + module.getMemorySize());
        for (WasmMemorySegment segment : module.getSegments()) {
            visitor.lf().open().append("segment " + segment.getOffset());
            visitor.indent();
            for (int i = 0; i < segment.getLength(); i += 256) {
                visitor.lf().append("\"");
                byte[] part = segment.getData(i, Math.min(segment.getLength(), i + 256) - i);
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < part.length; ++j) {
                    int b = part[j] << 24 >>> 24;
                    if (b < ' ' || b > 126) {
                        sb.append("\\" + Character.forDigit(b >> 4, 16) + Character.forDigit(b & 0xF, 16));
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
        WasmSignature signature = WasmSignature.fromFunction(function);
        visitor.append(" ").open().append("type $type" + visitor.getSignatureIndex(signature)).close();
    }

    private void renderTypes(WasmModule module) {
        WasmSignatureCollector signatureCollector = new WasmSignatureCollector(visitor::getSignatureIndex);
        for (WasmFunction function : module.getFunctions().values()) {
            visitor.getSignatureIndex(WasmSignature.fromFunction(function));
            for (WasmExpression part : function.getBody()) {
                part.acceptVisitor(signatureCollector);
            }
        }

        if (visitor.signatureList.isEmpty()) {
            return;
        }

        visitor.lf();
        int index = 0;
        for (WasmSignature signature : visitor.signatureList) {
            visitor.open().append("type $type" + index++ + " ");
            visitor.open().append("func");
            if (signature.types.length > 1) {
                visitor.append(" ").open().append("param");
                for (int i = 1; i < signature.types.length; ++i) {
                    visitor.append(" ").append(signature.types[i]);
                }
                visitor.close();
            }
            if (signature.types[0] != null) {
                visitor.append(" ").open().append("result ");
                visitor.append(signature.types[0]);
                visitor.close();
            }
            visitor.close();
            visitor.close();
            visitor.lf();
        }
    }

    private void renderTable(WasmModule module) {
        if (module.getFunctionTable().isEmpty()) {
            return;
        }

        visitor.lf().open().append("table");
        for (WasmFunction function : module.getFunctionTable()) {
            visitor.lf().append("$" + function.getName());
        }
        visitor.close().lf();
    }

    @Override
    public String toString() {
        return visitor.sb.toString();
    }
}
