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

import java.util.LinkedHashMap;
import java.util.Map;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.gc.WasmGCInitializerContributor;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCStandardClasses;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmMemorySegment;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.render.WasmBinaryWriter;
import org.teavm.backend.wasm.runtime.StringInternPool;
import org.teavm.backend.wasm.runtime.gc.WasmGCSupport;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.MethodReference;

public class WasmGCStringPool implements WasmGCStringProvider, WasmGCInitializerContributor {
    private WasmGCStandardClasses standardClasses;
    private WasmModule module;
    private WasmBinaryWriter binaryWriter = new WasmBinaryWriter();
    private Map<String, WasmGCStringConstant> stringMap = new LinkedHashMap<>();
    private BaseWasmFunctionRepository functionProvider;
    private WasmFunction initNextStringFunction;
    private WasmFunction initStringsFunction;
    private WasmArray stringsArray;
    private WasmGCNameProvider names;
    private WasmFunctionTypes functionTypes;
    private DependencyInfo dependencyInfo;

    public WasmGCStringPool(WasmGCStandardClasses standardClasses, WasmModule module,
            BaseWasmFunctionRepository functionProvider, WasmGCNameProvider names,
            WasmFunctionTypes functionTypes, DependencyInfo dependencyInfo) {
        this.standardClasses = standardClasses;
        this.module = module;
        this.functionProvider = functionProvider;
        this.names = names;
        this.functionTypes = functionTypes;
        this.dependencyInfo = dependencyInfo;
    }

    @Override
    public void contributeToInitializerDefinitions(WasmFunction function) {
        var segment = new WasmMemorySegment();
        module.getSegments().add(segment);
        segment.setData(binaryWriter.getData());
    }

    @Override
    public void contributeToInitializer(WasmFunction function) {
        if (initNextStringFunction == null) {
            return;
        }
        if (hasIntern()) {
            var internInit = functionProvider.forStaticMethod(new MethodReference(StringInternPool.class, "<clinit>",
                    void.class));
            function.getBody().add(new WasmCall(internInit));
        }
        var stringIterator = stringMap.values().iterator();
        while (stringIterator.hasNext()) {
            var elementCount = 0;
            var array = new WasmArrayNewFixed(stringsArray);
            // WasmArrayNewFixed cannot be larger than 10000 elements
            while (elementCount < 10000 && stringIterator.hasNext()) {
                array.getElements().add(new WasmGetGlobal(stringIterator.next().global));
                ++elementCount;
            }
            function.getBody().add(new WasmCall(initStringsFunction, array));
        }
    }

    @Override
    public WasmGCStringConstant getStringConstant(String string) {
        return stringMap.computeIfAbsent(string, s -> {
            if (initNextStringFunction == null) {
                createInitNextStringFunction();
                createInitStringsFunction();
            }
            binaryWriter.writeLEB(string.length());
            writeWTF8(string, binaryWriter);
            var brief = string.length() > 16 ? string.substring(0, 16) : string;
            var globalName = names.topLevel("teavm@string<" + stringMap.size() + ">"
                    + WasmGCNameProvider.sanitize(brief));
            var globalType = standardClasses.stringClass().getStructure().getNonNullReference();
            var global = new WasmGlobal(globalName, globalType,
                    new WasmStructNewDefault(standardClasses.stringClass().getStructure()));
            global.setImmutable(true);
            module.globals.add(global);
            return new WasmGCStringConstant(stringMap.size(), global);
        });
    }

    private void writeWTF8(String s, WasmBinaryWriter writer) {
        for (var i = 0; i < s.length(); ++i) {
            var c = (int) s.charAt(i);
            if (c < 0x80) {
                writer.writeByte(c);
            } else if (c < 0x800) {
                writer.writeByte(0xC0 | ((c >> 6) & 0x1F));
                writer.writeByte(0x80 | (c & 0x3F));
            } else if (c < 0x10000) {
                writer.writeByte(0xE0 | ((c >> 12) & 0x1F));
                writer.writeByte(0x80 | ((c >> 6) & 0x3F));
                writer.writeByte(0x80 | (c & 0x3F));
            }
        }
    }

    private void createInitStringsFunction() {
        stringsArray = new WasmArray(names.topLevel("teavm@stringArray"),
                standardClasses.stringClass().getStructure().getNonNullReference().asStorage());
        module.types.add(stringsArray);
        var function = new WasmFunction(functionTypes.of(null, stringsArray.getNonNullReference()));
        function.setName(names.topLevel("teavm@initStrings"));
        module.functions.add(function);

        var stringsLocal = new WasmLocal(stringsArray.getNonNullReference(), "strings");
        var indexLocal = new WasmLocal(WasmType.INT32, "index");
        var lengthLocal = new WasmLocal(WasmType.INT32, "length");
        function.add(stringsLocal);
        function.add(indexLocal);
        function.add(lengthLocal);

        var length = new WasmArrayLength(new WasmGetLocal(stringsLocal));
        function.getBody().add(new WasmSetLocal(lengthLocal, length));
        function.getBody().add(new WasmSetLocal(indexLocal, new WasmInt32Constant(0)));

        var loop = new WasmBlock(true);
        function.getBody().add(loop);
        var get = new WasmArrayGet(stringsArray, new WasmGetLocal(stringsLocal), new WasmGetLocal(indexLocal));
        loop.getBody().add(new WasmCall(initNextStringFunction, get));
        var next = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, new WasmGetLocal(indexLocal),
                new WasmInt32Constant(1));
        loop.getBody().add(new WasmSetLocal(indexLocal, next));
        var compare = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_UNSIGNED,
                new WasmGetLocal(indexLocal), new WasmGetLocal(lengthLocal));
        loop.getBody().add(new WasmBranch(compare, loop));

        initStringsFunction = function;
    }

    private void createInitNextStringFunction() {
        var stringTypeInfo = standardClasses.stringClass();
        var nextCharArrayFunction = functionProvider.forStaticMethod(new MethodReference(WasmGCSupport.class,
                "nextCharArray", char[].class));
        var function = new WasmFunction(functionTypes.of(null, stringTypeInfo.getStructure().getNonNullReference()));
        function.setName(names.topLevel("teavm@initNextString"));

        var stringLocal = new WasmLocal(stringTypeInfo.getType());
        function.add(stringLocal);

        var value = new WasmCall(nextCharArrayFunction);
        function.getBody().add(new WasmStructSet(stringTypeInfo.getStructure(),
                new WasmGetLocal(stringLocal), WasmGCClassInfoProvider.CUSTOM_FIELD_OFFSETS, value));
        function.getBody().add(new WasmStructSet(stringTypeInfo.getStructure(), new WasmGetLocal(stringLocal),
                WasmGCClassInfoProvider.VT_FIELD_OFFSET,
                new WasmGetGlobal(stringTypeInfo.getVirtualTablePointer())));
        if (hasIntern()) {
            var queryFunction = functionProvider.forStaticMethod(new MethodReference(StringInternPool.class,
                    "query", String.class, String.class));
            function.getBody().add(new WasmDrop(new WasmCall(queryFunction, new WasmGetLocal(stringLocal))));
            functionProvider.forStaticMethod(new MethodReference(StringInternPool.class, "<clinit>",
                    void.class));
        }
        module.functions.add(function);
        initNextStringFunction = function;
    }

    private boolean hasIntern() {
        var intern = dependencyInfo.getMethod(new MethodReference(String.class, "intern", String.class));
        return intern != null && intern.isUsed();
    }
}
