/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.newir.util;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class NameGenerator<T> {
    private ObjectIntMap<String> usedNames = new ObjectIntHashMap<>();
    private Map<T, String> names = new HashMap<>();
    private Function<T, String> hintProvider;

    public NameGenerator(Function<T, String> hintProvider) {
        this.hintProvider = hintProvider;
    }

    public String getName(T declaration) {
        String result = names.get(declaration);
        if (result == null) {
            String hint = hintProvider.apply(declaration);
            result = generateName(hint != null ? hint : "anonymous");
            names.put(declaration, result);
        }
        return result;
    }

    private String generateName(String hint) {
        String result = hint;
        int index = usedNames.get(result);
        if (index > 0) {
            do {
                result = hint + "_" + index++;
            } while (usedNames.containsKey(result));
        } else {
            ++index;
        }
        usedNames.put(hint, index);
        return result;
    }
}
