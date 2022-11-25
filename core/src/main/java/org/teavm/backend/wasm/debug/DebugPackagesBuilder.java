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
import java.util.Objects;

public class DebugPackagesBuilder extends DebugSectionBuilder implements DebugPackages {
    private DebugStrings strings;
    private ObjectIntMap<PackageInfo> packages = new ObjectIntHashMap<>();

    public DebugPackagesBuilder(DebugStrings strings) {
        super(DebugConstants.SECTION_PACKAGES);
        this.strings = strings;
    }

    @Override
    public int packagePtr(int basePackage, String name) {
        var key = new PackageInfo(basePackage, name);
        var result = packages.getOrDefault(key, -1);
        if (result < 0) {
            result = packages.size() + 1;
            packages.put(key, result);
            blob.writeLEB(basePackage);
            blob.writeLEB(strings.stringPtr(name));
        }
        return result;
    }

    private static class PackageInfo {
        private int parent;
        private String name;

        PackageInfo(int parent, String name) {
            this.parent = parent;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PackageInfo that = (PackageInfo) o;
            return parent == that.parent && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(parent, name);
        }
    }
}
