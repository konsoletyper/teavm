/*
 *  Copyright 2026 Jelle De Loecker.
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
package org.teavm.dependency;

import static org.junit.Assert.assertArrayEquals;
import java.util.Properties;
import org.junit.Test;
import org.teavm.common.ServiceRepository;
import org.teavm.diagnostics.AccumulationDiagnostics;
import org.teavm.model.ReferenceCache;
import org.teavm.model.ValueType;

public class SuperArrayFilterTest {
    private static PreciseDependencyAnalyzer createAnalyzer() {
        return new PreciseDependencyAnalyzer(name -> null, null, SuperArrayFilterTest.class.getClassLoader(),
                new ServiceRepository() {
                    @Override
                    public <T> T getService(Class<T> type) {
                        return null;
                    }
                }, new AccumulationDiagnostics(), new ReferenceCache(), new String[0], Properties::new);
    }

    @Test
    public void arrayOfPrimitiveArraysPassesFromLargeTypeSet() {
        var analyzer = createAnalyzer();
        var intArray = ValueType.arrayOf(ValueType.INTEGER);
        var intArrayArray = ValueType.arrayOf(intArray);

        var source = analyzer.createNode();
        // The set must be large enough for the analyzer to ask filters to extract types from it,
        // instead of matching types one by one.
        for (int i = 0; i < 20; ++i) {
            source.propagate(analyzer.getType(ValueType.object("T" + i)));
        }
        source.propagate(analyzer.getType(intArray));
        source.propagate(analyzer.getType(intArrayArray));

        var target = analyzer.createNode(intArrayArray);
        source.connect(target);
        analyzer.processDependencies();

        assertArrayEquals(new ValueType[] { intArrayArray }, target.getTypes());
    }
}
