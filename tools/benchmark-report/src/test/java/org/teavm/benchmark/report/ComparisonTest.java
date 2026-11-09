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
package org.teavm.benchmark.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class ComparisonTest {
    @Test
    public void welchTTest() {
        var a = new Statistics(List.of(1.0, 2.0, 3.0, 4.0, 5.0));
        var b = new Statistics(List.of(6.0, 7.0, 8.0, 9.0, 10.0));
        // t = -5, 8 degrees of freedom
        assertEquals(0.00105283, Statistics.welchTTest(a, b, 1), 1e-8);
        assertEquals(1, Statistics.welchTTest(a, a, 1), 1e-9);
        // same values in other unit
        var c = new Statistics(List.of(1000.0, 2000.0, 3000.0, 4000.0, 5000.0));
        assertEquals(1, Statistics.welchTTest(a, c, 0.001), 1e-9);
        assertTrue(Double.isNaN(Statistics.welchTTest(a, new Statistics(List.of(1.0)), 1)));
    }

    @Test
    public void significance() {
        var base = result("base", "ms/op", 10.0, 10.1, 9.9, 10.0, 10.05);
        var same = result("same", "ms/op", 10.05, 9.95, 10.1, 9.9, 10.0);
        var slower = result("slower", "ms/op", 12.0, 12.1, 11.9, 12.0, 12.05);
        assertFalse(ComparisonTable.isSignificant(same, base));
        assertTrue(ComparisonTable.describe(same, base).endsWith("(not significant)"));
        assertTrue(ComparisonTable.isSignificant(slower, base));
        assertEquals("1.20x slower than base", ComparisonTable.describe(slower, base));

        var lessAlloc = result("less", "B/op", 80.0, 80.0, 81.0, 79.0, 80.0);
        var moreAlloc = result("more", "B/op", 100.0, 100.0, 101.0, 99.0, 100.0);
        assertEquals("1.25x less than more", ComparisonTable.describe(lessAlloc, moreAlloc));
    }

    @Test
    public void displaysBytes() {
        var perOp = ScoreUnits.display(result("a", "B/op", 3.0 * 1024 * 1024, 3.0 * 1024 * 1024));
        assertEquals("MB/op", perOp.unit);
        assertEquals("3.000", perOp.score);
        var plain = ScoreUnits.display(result("a", "B", 1536.0 * 1024 * 1024, 1536.0 * 1024 * 1024));
        assertEquals("GB", plain.unit);
        assertEquals("1.500", plain.score);
    }

    @Test
    public void readsSecondaryMetrics() throws IOException {
        var dir = new File("build/tmp/comparison-test");
        dir.mkdirs();
        var file = new File(dir, "input.json");
        Files.writeString(file.toPath(), "[{\"benchmark\":\"a.b.C.m\",\"mode\":\"avgt\",\"forks\":1,"
                + "\"params\":{\"p\":\"1\"},"
                + "\"primaryMetric\":{\"score\":2.0,\"scoreError\":\"NaN\",\"scoreUnit\":\"ms/op\","
                + "\"rawData\":[[2.0]]},"
                + "\"secondaryMetrics\":{"
                + "\"gc.alloc.rate\":{\"score\":5.0,\"scoreError\":0.1,\"scoreUnit\":\"MB/sec\",\"rawData\":[[5.0]]},"
                + "\"gc.alloc.rate.norm\":{\"score\":100.0,\"scoreError\":1.0,\"scoreUnit\":\"B/op\","
                + "\"rawData\":[[100.0]]}}}]", StandardCharsets.UTF_8);
        var results = JmhJsonFormat.read("run=" + file.getPath());
        assertEquals(2, results.size());
        assertEquals("a.b.C.m", results.get(0).getBenchmark());
        assertNull(results.get(0).getSecondaryMetric());
        assertEquals("run", results.get(0).getSource());
        assertEquals("a.b.C.m:gc.alloc.rate.norm", results.get(1).getBenchmark());
        assertEquals("gc.alloc.rate.norm", results.get(1).getSecondaryMetric());
        assertEquals("B/op", results.get(1).getUnit());
        assertEquals(Map.of("p", "1"), results.get(1).getParams());

        var table = new ComparisonTable(results);
        assertEquals("C.m:gc.alloc.rate.norm", table.shortName(results.get(1).getBenchmark()));

        var copy = new File(dir, "copy.json");
        JmhJsonFormat.write(results, copy, null);
        assertEquals(1, JmhJsonFormat.read(copy, "copy", Set.of()).size());
    }

    private static BenchmarkResult result(String source, String unit, Double... values) {
        return new BenchmarkResult(source, source, "a.b.C.m", "avgt", Map.of(), unit, List.of(List.of(values)));
    }
}
