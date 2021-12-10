/*
 *  Copyright 2021 konso.
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
package org.teavm.backend.c.generate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.model.ValueType;

public class ShorteningFileNameProvider implements FileNameProvider {
    private final FileNameProvider nameProvider;
    private final Map<String, String> names = new HashMap<>();
    private final Set<String> usedNames = new HashSet<>();

    public ShorteningFileNameProvider(FileNameProvider nameProvider) {
        this.nameProvider = nameProvider;
    }

    @Override
    public String fileName(String className) {
        return unique(shorten(nameProvider.fileName(className)));
    }

    @Override
    public String fileName(ValueType type) {
        return unique(shorten(nameProvider.fileName(type)));
    }

    @Override
    public String escapeName(String name) {
        return unique(shorten(nameProvider.escapeName(name)));
    }

    private String unique(String name) {
        return names.computeIfAbsent(name, n -> {
            if (usedNames.add(n)) {
                return n;
            }
            int suffix = 1;
            while (true) {
                String candidate = n + "_" + suffix++;
                if (usedNames.add(candidate)) {
                    return candidate;
                }
            }
        });
    }

    private static String shorten(String name) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        while (true) {
            int next = name.indexOf('/', index);
            if (next < 0) {
                break;
            }
            if (next > index) {
                sb.append(name.charAt(index));
            }
            sb.append('/');
            index = next + 1;
        }
        return sb.append(name, index, name.length()).toString();
    }
}
