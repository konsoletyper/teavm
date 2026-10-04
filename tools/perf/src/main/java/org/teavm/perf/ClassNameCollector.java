/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.perf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * Lists names of classes in directories and JAR files.
 */
public final class ClassNameCollector {
    private ClassNameCollector() {
    }

    public static List<String> collect(File file) throws IOException {
        var result = new ArrayList<String>();
        if (file.isDirectory()) {
            var root = file.toPath();
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .map(path -> root.relativize(path).toString().replace(File.separatorChar, '/'))
                        .forEach(path -> addClass(path, result));
            }
        } else if (file.isFile() && file.getName().endsWith(".jar")) {
            try (var zip = new ZipFile(file)) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        addClass(entry.getName(), result);
                    }
                }
            }
        }
        return result;
    }

    private static void addClass(String path, List<String> result) {
        if (!path.endsWith(".class") || path.startsWith("META-INF/")) {
            return;
        }
        var name = path.substring(0, path.length() - ".class".length()).replace('/', '.');
        if (name.endsWith("module-info") || name.endsWith("package-info") || name.contains(".jmh_generated.")) {
            return;
        }
        result.add(name);
    }
}
