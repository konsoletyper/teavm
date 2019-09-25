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
package org.teavm.parsing.resource;

import java.util.function.Function;
import org.teavm.common.CachedFunction;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;

public class MapperClassHolderSource implements ClassHolderSource {
    private Function<String, ClassHolder> mapper;

    public MapperClassHolderSource(Function<String, ClassHolder> mapper) {
        this.mapper = new CachedFunction<>(mapper);
    }

    @Override
    public ClassHolder get(String name) {
        return mapper.apply(name);
    }
}
