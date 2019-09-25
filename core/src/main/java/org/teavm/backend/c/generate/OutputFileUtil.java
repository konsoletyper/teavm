/*
 *  Copyright 2019 Alexey Andreev.
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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.teavm.vm.BuildTarget;

public final class OutputFileUtil {
    private OutputFileUtil() {
    }

    public static void write(BufferedCodeWriter code, String name, BuildTarget buildTarget) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                buildTarget.createResource(name), StandardCharsets.UTF_8))) {
            code.writeTo(writer, name);
        }
    }
}
