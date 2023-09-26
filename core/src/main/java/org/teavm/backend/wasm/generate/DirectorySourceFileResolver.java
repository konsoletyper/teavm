/*
 *  Copyright 2023 Alexey Andreev.
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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectorySourceFileResolver implements SourceFileResolver {
    private List<File> directories;
    private Map<String, Wrapper> cache = new HashMap<>();

    public DirectorySourceFileResolver(List<File> directories) {
        this.directories = List.copyOf(directories);
    }

    @Override
    public String resolveFile(String file) {
        return cache.computeIfAbsent(file, f -> {
            for (var dir : directories) {
                var candidate = new File(dir, f);
                if (candidate.isFile()) {
                    return new Wrapper(candidate.getAbsolutePath());
                }
            }
            return new Wrapper(null);
        }).value;
    }

    private static class Wrapper {
        final String value;

        Wrapper(String value) {
            this.value = value;
        }
    }
}
