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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

/**
 * Summarizes CPU profile in Chrome DevTools format ({@code .cpuprofile}) by self time of functions.
 */
public class CpuProfileSummary {
    private final List<Entry> entries;
    private final long totalSamples;

    private CpuProfileSummary(List<Entry> entries, long totalSamples) {
        this.entries = entries;
        this.totalSamples = totalSamples;
    }

    public static CpuProfileSummary parse(String profile) {
        var root = new ObjectMapper().readTree(profile);
        var samplesByFunction = new HashMap<String, Long>();
        var total = 0L;
        for (var node : root.path("nodes")) {
            var hitCount = node.path("hitCount").asLong();
            if (hitCount == 0) {
                continue;
            }
            var name = node.path("callFrame").path("functionName").asString();
            if (name.isEmpty()) {
                name = "(anonymous)";
            }
            samplesByFunction.merge(name, hitCount, Long::sum);
            total += hitCount;
        }
        var entries = new ArrayList<Entry>();
        for (var mapEntry : samplesByFunction.entrySet()) {
            entries.add(new Entry(mapEntry.getKey(), mapEntry.getValue()));
        }
        entries.sort((a, b) -> Long.compare(b.samples, a.samples));
        return new CpuProfileSummary(entries, total);
    }

    public long getTotalSamples() {
        return totalSamples;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public double fraction(Entry entry) {
        return totalSamples > 0 ? (double) entry.samples / totalSamples : 0;
    }

    public static class Entry {
        public final String function;
        public final long samples;

        Entry(String function, long samples) {
            this.function = function;
            this.samples = samples;
        }
    }
}
