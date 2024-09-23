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
package org.teavm.platform.plugin.wasmgc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCCustomTypeMapper;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ValueType;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;

public class ResourceCustomTypeMapper implements WasmGCCustomTypeMapper {
    private WasmModule module;
    private WasmGCTypeMapper typeMapper;
    private ClassReaderSource classSource;
    private WasmGCNameProvider names;
    private Map<String, WasmStructure> structures = new HashMap<>();
    private WasmArray array;
    private WasmArray mapArray;

    public ResourceCustomTypeMapper(WasmModule module, WasmGCTypeMapper typeMapper, ClassReaderSource classSource,
            WasmGCNameProvider names) {
        this.module = module;
        this.typeMapper = typeMapper;
        this.classSource = classSource;
        this.names = names;
    }

    @Override
    public WasmType map(String className) {
        var cls = classSource.get(className);
        if (cls == null) {
            return null;
        }
        if (cls.getAnnotations().get(ResourceMarker.class.getName()) == null) {
            return null;
        }

        if (className.equals(Resource.class.getName())) {
            return WasmType.Reference.EQ;
        }
        if (className.equals(ResourceArray.class.getName())) {
            if (array == null) {
                array = new WasmArray(names.topLevel(names.suggestForClass(className)),
                        WasmType.Reference.EQ.asStorage());
                array.setImmutable(true);
                module.types.add(array);
            }
            return array.getReference();
        } else if (className.equals(ResourceMap.class.getName())) {
            if (mapArray == null) {
                mapArray = new WasmArray(names.topLevel(names.suggestForClass(className)),
                        typeMapper.mapStorageType(ValueType.object(ResourceMapEntry.class.getName())));
                mapArray.setImmutable(true);
                module.types.add(mapArray);
            }
            return mapArray.getReference();
        } else {
            return getStructure(className).getReference();
        }
    }

    private WasmStructure getStructure(String className) {
        var struct = structures.get(className);
        if (struct == null) {
            var name = names.topLevel(names.suggestForClass(className));
            var cls = classSource.get(className);
            struct = new WasmStructure(name, fields -> addFieldsFromClass(cls, fields));
            module.types.add(struct);
            structures.put(className, struct);
            if (!cls.getParent().equals(Resource.class.getName())) {
                struct.setSupertype(getStructure(cls.getParent()));
            }
        }
        return struct;
    }

    private void addFieldsFromClass(ClassReader cls, List<WasmField> fields) {
        if (!cls.getName().equals(Resource.class.getName())) {
            var parentCls = classSource.get(cls.getParent());
            if (parentCls != null) {
                addFieldsFromClass(parentCls, fields);
            }
        }
        for (var method : cls.getMethods()) {
            var annot = method.getAnnotations().get(FieldMarker.class.getName());
            if (annot == null) {
                continue;
            }
            var fieldName = annot.getValue("value").getString();
            var fieldType = typeMapper.mapStorageType(method.getResultType());
            var field = new WasmField(fieldType, names.structureField(fieldName));
            field.setImmutable(true);
            fields.add(field);
        }
    }
}
