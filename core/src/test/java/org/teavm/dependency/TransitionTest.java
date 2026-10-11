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
package org.teavm.dependency;

import static org.junit.Assert.assertEquals;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import org.junit.Test;
import org.teavm.common.ServiceRepository;
import org.teavm.diagnostics.AccumulationDiagnostics;
import org.teavm.model.ReferenceCache;
import org.teavm.model.ValueType;

public class TransitionTest {
    @Test
    public void mergeDeliversTypesThatAreStillOnTheirWay() {
        var analyzer = new PreciseDependencyAnalyzer(name -> null, null, TransitionTest.class.getClassLoader(),
                new ServiceRepository() {
                    @Override
                    public <T> T getService(Class<T> type) {
                        return null;
                    }
                }, new AccumulationDiagnostics(), new ReferenceCache(), new String[0], Properties::new);
        var a = analyzer.getType(ValueType.object("A"));
        var b = analyzer.getType(ValueType.object("B"));
        var c = analyzer.getType(ValueType.object("C"));

        var source = analyzer.createNode();
        var first = analyzer.createNode();
        var second = analyzer.createNode();
        Set<ValueType> received = new LinkedHashSet<>();
        second.addConsumer(type -> received.add(type.getValueType()));
        second.propagate(c);
        source.propagate(c);

        // A transition with a filter never merges domains, so A reaches the first node by a plain delivery.
        source.connect(first, type -> true);
        source.connect(second);

        // When A reaches the first node, the source gets B. B reaches the second node before A does, and that
        // delivery merges the second node into the source's domain, which already holds A.
        first.addConsumer(type -> {
            if (type == a) {
                source.propagate(b);
            }
        });
        source.propagate(a);
        analyzer.processDependencies();

        assertEquals(Set.of(a.getValueType(), b.getValueType(), c.getValueType()), received);
    }
}
