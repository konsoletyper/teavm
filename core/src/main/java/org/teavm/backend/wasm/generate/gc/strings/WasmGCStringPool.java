/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.generate.gc.strings;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCStandardClasses;
import org.teavm.backend.wasm.generate.gc.initialization.WasmGCInitializerContributor;
import org.teavm.backend.wasm.generate.gc.method.WasmGCFunctionProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.render.WasmBinaryWriter;
import org.teavm.model.MethodReference;

public class WasmGCStringPool implements WasmGCStringProvider, WasmGCInitializerContributor {
    private WasmGCStandardClasses standardClasses;
    private WasmModule module;
    private WasmBinaryWriter binaryWriter = new WasmBinaryWriter();
    private Map<String, WasmGCStringConstant> stringMap = new LinkedHashMap<>();
    private WasmGCFunctionProvider functionProvider;

    public WasmGCStringPool(WasmGCStandardClasses standardClasses, WasmModule module,
            WasmGCFunctionProvider functionProvider) {
        this.standardClasses = standardClasses;
        this.module = module;
        this.functionProvider = functionProvider;
    }

    @Override
    public void contributeToInitializerDefinitions(WasmFunction function) {
        for (var str : stringMap.values()) {
            var newStruct = new WasmStructNewDefault(standardClasses.stringClass().getStructure());
            function.getBody().add(new WasmSetGlobal(str.global, newStruct));
        }
    }

    @Override
    public void contributeToInitializer(WasmFunction function) {
        var nextCharArrayFunction = functionProvider.getStaticFunction(new MethodReference(WasmGCStringPool.class,
                "nextCharArray", char[].class));
    }

    @Override
    public WasmGCStringConstant getStringConstant(String string) {
        return stringMap.computeIfAbsent(string, s -> {
            binaryWriter.writeInt32(string.length());
            binaryWriter.writeBytes(string.getBytes(StandardCharsets.UTF_8));
            var globalName = "_teavm_java_string_" + stringMap.size();
            var globalType = standardClasses.stringClass().getType();
            var global = new WasmGlobal(globalName, globalType, WasmExpression.defaultValueOfType(globalType));
            module.globals.add(global);
            return new WasmGCStringConstant(stringMap.size(), global);
        });
    }

    static char[] nextCharArray() {
        var length = nextLEB();
        var result = new char[length];
        var pos = 0;
        while (pos < length) {
            var b = nextByte();
            if ((b & 0x80) == 0) {
                result[pos++] = (char) b;
            } else if ((b & 0xE0) == 0xC0) {
                var b2 = nextByte();
                result[pos++] = (char) (((b & 0x1F) << 6) | (b2 & 0x3F));
            } else if ((b & 0xF0) == 0xE0) {
                var b2 = nextByte();
                var b3 = nextByte();
                var c = (char) (((b & 0x0F) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3F));
                result[pos++] = c;
            } else if ((b & 0xF8) == 0xF0) {
                var b2 = nextByte();
                var b3 = nextByte();
                var b4 = nextByte();
                var code = ((b & 0x07) << 18) | ((b2 & 0x3f) << 12) | ((b3 & 0x3F) << 6) | (b4 & 0x3F);
                result[pos++] = Character.highSurrogate(code);
                result[pos++] = Character.lowSurrogate(code);
            }
        }
        return result;
    }

    private static int nextLEB() {
        var shift = 0;
        var result = 0;
        while (true) {
            var b = nextByte();
            var digit = b & 0x7F;
            result |= digit << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
        }
        return result;
    }

    private static native byte nextByte();

    private static native void error();
}
