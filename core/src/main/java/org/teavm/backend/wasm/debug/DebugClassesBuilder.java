/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.debug;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;

public class DebugClassesBuilder extends DebugSectionBuilder implements DebugClasses {
    private DebugPackages packages;
    private DebugStrings strings;
    private ObjectIntMap<String> classes = new ObjectIntHashMap<>();

    public DebugClassesBuilder(DebugPackages packages, DebugStrings strings) {
        super(DebugConstants.SECTION_CLASSES);
        this.packages = packages;
        this.strings = strings;
    }

    @Override
    public int classPtr(String className) {
        var result = classes.getOrDefault(className, -1);
        if (result < 0) {
            result = classes.size();
            classes.put(className, result);
            var packagePtr = 0;
            var index = 0;
            while (true) {
                var next = className.indexOf('.', index);
                if (next < 0) {
                    break;
                }
                packagePtr = packages.packagePtr(packagePtr, className.substring(index, next));
                index = next + 1;
            }
            blob.writeLEB(packagePtr);
            blob.writeLEB(strings.stringPtr(className.substring(index)));
        }
        return result;
    }
}
