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
package org.teavm.backend.wasm.gc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;

public final class WasmGCUtil {
    private WasmGCUtil() {
    }


    public static String findCommonSuperclass(ClassHierarchy hierarchy, ClassReader a, ClassReader b) {
        var firstPath = findPathToRoot(hierarchy, a);
        Collections.reverse(firstPath);
        var secondPath = findPathToRoot(hierarchy, b);
        Collections.reverse(secondPath);
        if (firstPath.get(0) != secondPath.get(0)) {
            return "java.lang.Object";
        }
        var max = Math.max(firstPath.size(), secondPath.size());
        var index = 1;
        while (index < max && firstPath.get(index) == secondPath.get(index)) {
            ++index;
        }
        return firstPath.get(index).getName();
    }

    private static List<ClassReader> findPathToRoot(ClassHierarchy hierarchy, ClassReader cls) {
        var path = new ArrayList<ClassReader>();
        while (cls != null) {
            path.add(cls);
            cls = cls.getParent() != null ? hierarchy.getClassSource().get(cls.getParent()) : null;
        }
        return path;
    }

}
