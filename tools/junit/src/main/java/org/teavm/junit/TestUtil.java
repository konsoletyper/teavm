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
package org.teavm.junit;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import org.apache.commons.io.IOUtils;

final class TestUtil {
    private TestUtil() {
    }

    static File getOutputFile(File path, String baseName, String suffix, boolean separateDir, String extension) {
        StringBuilder simpleName = new StringBuilder();
        simpleName.append(baseName);
        if (!suffix.isEmpty()) {
            if (!separateDir) {
                simpleName.append('-').append(suffix);
            }
        }
        File outputFile;
        if (separateDir) {
            outputFile = new File(new File(path, simpleName.toString()), "test" + extension);
        } else {
            simpleName.append(extension);
            outputFile = new File(path, simpleName.toString());
        }

        return outputFile;
    }

    static void resourceToFile(String resource, File file, Map<String, String> properties) throws IOException {
        file.getParentFile().mkdirs();
        if (properties.isEmpty()) {
            try (InputStream input = TeaVMTestRunner.class.getClassLoader().getResourceAsStream(resource);
                    OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
                IOUtils.copy(input, output);
            }
        } else {
            String content;
            try (InputStream input = TeaVMTestRunner.class.getClassLoader().getResourceAsStream(resource)) {
                content = IOUtils.toString(input, UTF_8);
            }
            content = replaceProperties(content, properties);
            try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file));
                    Writer writer = new OutputStreamWriter(output)) {
                writer.write(content);
            }
        }
    }

    static String replaceProperties(String s, Map<String, String> properties) {
        int i = 0;
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            int next = s.indexOf("${", i);
            if (next < 0) {
                break;
            }
            int end = s.indexOf('}', next + 2);
            if (end < 0) {
                break;
            }

            sb.append(s, i, next);
            String property = s.substring(next + 2, end);
            String value = properties.get(property);
            if (value == null) {
                sb.append(s, next, end + 1);
            } else {
                sb.append(value);
            }
            i = end + 1;
        }

        if (i == 0) {
            return s;
        }

        return sb.append(s.substring(i)).toString();
    }

}
