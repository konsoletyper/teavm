/*
 *  Copyright 2026 TeaVM contributors.
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
import static org.junit.Assert.assertSame;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.vm.TeaVMBuilder;

public class DependencyOrderTest {
    @Test
    public void consumerOrderInDiscoveredDomain() {
        var analyzer = createAnalyzer();
        var first = new HashedNode(analyzer, 2);
        var second = new HashedNode(analyzer, 1);
        var third = new HashedNode(analyzer, 0);
        var order = new ArrayList<String>();
        first.addConsumer(type -> order.add("first"));
        second.addConsumer(type -> order.add("second"));
        third.addConsumer(type -> order.add("third"));
        first.connect(second);
        second.connect(third);

        first.propagate(analyzer.getClassType("Example"));

        assertSame(first.typeSet, second.typeSet);
        assertSame(first.typeSet, third.typeSet);
        assertEquals(List.of("first", "second", "third"), order);
    }

    @Test
    public void consumerOrderInMergedDomain() {
        var analyzer = createAnalyzer();
        var first = new HashedNode(analyzer, 1);
        var second = new HashedNode(analyzer, 0);
        var commonType = analyzer.getClassType("Common");
        first.propagate(commonType);
        first.propagate(analyzer.getClassType("Extra"));
        second.propagate(commonType);
        first.connect(second);
        assertSame(first.typeSet, second.typeSet);

        var order = new ArrayList<String>();
        first.addConsumer(type -> order.add("first"));
        second.addConsumer(type -> order.add("second"));
        order.clear();

        first.propagate(analyzer.getClassType("Example"));

        assertEquals(List.of("first", "second"), order);
    }

    private DependencyAnalyzer createAnalyzer() {
        return (DependencyAnalyzer) new TeaVMBuilder(new JavaScriptTarget()).build().getDependencyInfo();
    }

    // Make hash iteration order differ from discovery order without depending on object allocation.
    private static class HashedNode extends DependencyNode {
        private final int hash;

        HashedNode(DependencyAnalyzer analyzer, int hash) {
            super(analyzer, null);
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
