/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.backend.wasm.generate.methods;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmNullCondition;
import org.teavm.model.ValueType;

public final class WasmGCGenerationUtil {
    private WasmGCGenerationUtil() {
    }

    public static void allocateArray(WasmGCClassInfoProvider classInfoProvider, ValueType itemType,
            WasmInstructionBuilder builder, BiConsumer<WasmArray, WasmInstructionBuilder> data) {
        var classInfoType = classInfoProvider.reflectionTypes().classInfo();
        var classInfo = classInfoProvider.getClassInfo(ValueType.arrayOf(itemType));

        var wasmArrayType = (WasmType.CompositeReference) classInfo.getStructure().getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET)
                .getUnpackedType();
        var wasmArray = (WasmArray) wasmArrayType.composite;

        int depth = 1;
        while (itemType instanceof ValueType.Array) {
            itemType = ((ValueType.Array) itemType).getItemType();
        }
        builder.getGlobal(classInfoProvider.getClassInfo(itemType).getPointer());
        for (var i = 0; i < depth; ++i) {
            builder.call(classInfoProvider.getGetArrayClassFunction());
        }
        builder.structGet(classInfoType.structure(), classInfoType.vtableIndex());

        builder.nullConst(WasmType.EQ);
        data.accept(wasmArray, builder);
        builder.structNew(classInfo.getStructure());
    }

    public static void getArrayLengthOfNullable(WasmInstructionBuilder builder,
            Consumer<WasmInstructionBuilder> value) {
        var outerBlock = builder.block(WasmType.INT32);
        var innerBlock = outerBlock.block();
        value.accept(innerBlock);
        innerBlock.nullBranch(WasmNullCondition.NULL, innerBlock)
                .arrayLength()
                .breakTo(outerBlock);
        outerBlock.i32Const(0);
    }
    
    public static void emitClassInfoLiteral(WasmGCClassInfoProvider classInfoProvider, WasmInstructionBuilder builder,
            ValueType type) {
        var degree = 0;
        if (type instanceof ValueType.Array) {
            var itemType = ((ValueType.Array) type).getItemType();
            if (!(itemType instanceof ValueType.Primitive)) {
                while (type instanceof ValueType.Array) {
                    type = ((ValueType.Array) type).getItemType();
                    ++degree;
                }
            }
        }
        builder.getGlobal(classInfoProvider.getClassInfo(type).getPointer());
        while (degree-- > 0) {
            builder.call(classInfoProvider.getGetArrayClassFunction());
        }
    }

    public static void emitClassLiteral(WasmGCClassInfoProvider classInfoProvider, WasmInstructionBuilder builder,
            ValueType type) {
        emitClassInfoLiteral(classInfoProvider, builder, type);
        builder.call(classInfoProvider.reflectionTypes().classInfo().classObjectFunction());
    }
}
