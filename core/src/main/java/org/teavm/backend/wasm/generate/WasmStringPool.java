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
package org.teavm.backend.wasm.generate;

import java.util.HashMap;
import java.util.Map;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.binary.DataArray;
import org.teavm.backend.wasm.binary.DataPrimitives;
import org.teavm.backend.wasm.binary.DataStructure;
import org.teavm.backend.wasm.binary.DataValue;
import org.teavm.model.ValueType;
import org.teavm.runtime.RuntimeObject;

public class WasmStringPool {
    private WasmClassGenerator classGenerator;
    private BinaryWriter binaryWriter;
    private Map<String, Integer> stringMap = new HashMap<>();
    private DataStructure arrayHeaderType = new DataStructure((byte) 0,
            DataPrimitives.INT, /* class pointer */
            DataPrimitives.ADDRESS, /* monitor */
            DataPrimitives.INT /* size */);
    private DataStructure stringType = new DataStructure((byte) 0,
            DataPrimitives.INT, /* class pointer */
            DataPrimitives.ADDRESS, /* monitor */
            DataPrimitives.ADDRESS, /* characters */
            DataPrimitives.INT /* hash code */);

    public WasmStringPool(WasmClassGenerator classGenerator, BinaryWriter binaryWriter) {
        this.classGenerator = classGenerator;
        this.binaryWriter = binaryWriter;
    }

    public int getStringPointer(String value) {
        Integer pointer = stringMap.get(value);
        if (pointer == null) {
            pointer = generateStringPointer(value);
            stringMap.put(value, pointer);
        }
        return pointer;
    }

    private int generateStringPointer(String value) {
        DataArray charactersType = new DataArray(DataPrimitives.SHORT, value.length());
        DataStructure wrapperType = new DataStructure((byte) 0, arrayHeaderType, charactersType);
        DataValue wrapper = wrapperType.createValue();
        DataValue header = wrapper.getValue(0);
        DataValue characters = wrapper.getValue(1);

        int classPointer = classGenerator.getClassPointer(ValueType.arrayOf(ValueType.CHARACTER));
        header.setInt(0, (classPointer >>> 3) | RuntimeObject.GC_MARKED);
        header.setInt(2, value.length());
        for (int i = 0; i < value.length(); ++i) {
            characters.setShort(i, (short) value.charAt(i));
        }

        DataValue stringObject = stringType.createValue();
        int stringPointer = binaryWriter.append(stringObject);
        classPointer = classGenerator.getClassPointer(ValueType.object(String.class.getName()));
        stringObject.setInt(0, (classPointer >>> 3) | RuntimeObject.GC_MARKED);
        stringObject.setAddress(2, binaryWriter.append(wrapper));

        return stringPointer;
    }
}
