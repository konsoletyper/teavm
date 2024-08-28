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
package org.teavm.backend.wasm.disasm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.teavm.backend.wasm.parser.NameIndirectMapListener;
import org.teavm.backend.wasm.parser.NameMapListener;
import org.teavm.backend.wasm.parser.NameSectionListener;

public class NameAccumulatingSectionListener implements NameSectionListener {
    private List<String> functions = new ArrayList<>();
    private List<List<String>> locals = new ArrayList<>();
    private List<String> types = new ArrayList<>();
    private List<String> globals = new ArrayList<>();
    private List<List<String>> fields = new ArrayList<>();

    @Override
    public NameMapListener functions() {
        return listener(functions);
    }

    @Override
    public NameIndirectMapListener locals() {
        return indirectListener(locals);
    }

    @Override
    public NameMapListener types() {
        return listener(types);
    }

    @Override
    public NameMapListener globals() {
        return listener(globals);
    }

    @Override
    public NameIndirectMapListener fields() {
        return indirectListener(fields);
    }

    public NameProvider buildProvider() {
        return new NameProvider() {
            String[] functionArray = buildMap(functions);
            String[][] localArray = buildIndirectMap(locals);
            String[] typeArray = buildMap(types);
            String[] globalArray = buildMap(globals);
            String[][] fieldArray = buildIndirectMap(fields);

            @Override
            public String function(int index) {
                return fetch(functionArray, index);
            }

            @Override
            public String local(int functionIndex, int index) {
                return fetch(localArray, functionIndex, index);
            }

            @Override
            public String type(int index) {
                return fetch(typeArray, index);
            }

            @Override
            public String global(int index) {
                return fetch(globalArray, index);
            }

            @Override
            public String field(int typeIndex, int index) {
                return fetch(fieldArray, typeIndex, index);
            }

            private String fetch(String[] array, int index) {
                return array != null && index < array.length ? array[index] : null;
            }

            private String fetch(String[][] array, int mapIndex, int index) {
                return array != null && mapIndex < array.length ? fetch(array[mapIndex], index) : null;
            }
        };
    }

    private String[] buildMap(List<String> list) {
        if (list == null) {
            return null;
        }
        return list.toArray(new String[0]);
    }

    private String[][] buildIndirectMap(List<List<String>> list) {
        var result = new String[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            result[i] = buildMap(list.get(i));
        }
        return result;
    }

    private NameMapListener listener(List<String> target) {
        return (index, name) -> {
            if (index >= target.size()) {
                target.addAll(Collections.nCopies(index - target.size() + 1, null));
            }
            target.set(index, name);
        };
    }

    private NameIndirectMapListener indirectListener(List<List<String>> target) {
        return index -> {
            if (index >= target.size()) {
                target.addAll(Collections.nCopies(index - target.size() + 1, null));
            }
            var list = new ArrayList<String>();
            target.set(index, list);
            return listener(list);
        };
    }
}
