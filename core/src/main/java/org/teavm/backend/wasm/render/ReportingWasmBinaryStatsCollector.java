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
package org.teavm.backend.wasm.render;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportingWasmBinaryStatsCollector implements WasmBinaryStatsCollector {
    private Map<String, ClassStats> statsByClass = new LinkedHashMap<>();
    private ObjectIntMap<String> sectionSizes = new ObjectIntHashMap<>();
    private int stringsSize;

    @Override
    public void addClassCodeSize(String className, int bytes) {
        getStats(className).codeSize += bytes;
    }

    @Override
    public void addClassMetadataSize(String className, int bytes) {
        getStats(className).metadataSize += bytes;
    }

    private ClassStats getStats(String className) {
        return statsByClass.computeIfAbsent(className, k -> new ClassStats());
    }

    @Override
    public void addSectionSize(String name, int bytes) {
        sectionSizes.put(name, sectionSizes.get(name) + bytes);
    }

    @Override
    public void addStringsSize(int bytes) {
        stringsSize += bytes;
    }

    public void write(Writer writer) throws IOException {
        var pw = new PrintWriter(writer);
        pw.println("[classes]");
        for (var entry : statsByClass.entrySet()) {
            pw.append(entry.getKey()).append(" ");
            pw.print(entry.getValue().codeSize);
            pw.append(" ");
            pw.print(entry.getValue().metadataSize);
            pw.println();
        }

        pw.println();
        pw.println("[strings]");
        pw.println(stringsSize);

        pw.println();
        pw.println("[sections]");
        for (var entry : sectionSizes) {
            pw.append(entry.key).append(" ");
            pw.print(entry.value);
            pw.println();
        }
    }

    private static class ClassStats {
        int codeSize;
        int metadataSize;
    }
}
