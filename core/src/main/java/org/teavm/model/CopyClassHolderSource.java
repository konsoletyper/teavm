/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.model;

import org.teavm.model.util.ModelUtils;
import org.teavm.parsing.resource.MapperClassHolderSource;

public class CopyClassHolderSource implements ClassHolderSource {
    private ClassReaderSource innerSource;
    private MapperClassHolderSource mapperSource = new MapperClassHolderSource(this::copyClass);

    public CopyClassHolderSource(ClassReaderSource innerSource) {
        this.innerSource = innerSource;
    }

    @Override
    public ClassHolder get(String name) {
        return mapperSource.get(name);
    }

    private ClassHolder copyClass(String className) {
        ClassReader original = innerSource.get(className);
        if (original == null) {
            return null;
        }
        return ModelUtils.copyClass(original);
    }
}
