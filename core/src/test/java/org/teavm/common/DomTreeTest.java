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
package org.teavm.common;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DomTreeTest {
    @Test
    public void domTree1() {
        var builder = new GraphBuilder();
        addEdges(builder, 0, 16, 17);
        addEdges(builder, 1, 14);
        addEdges(builder, 2, 18);
        addEdges(builder, 3, 6, 7, 15);
        addEdges(builder, 4, 11, 15, 26);
        addEdges(builder, 5, 11, 15, 30);
        addEdges(builder, 6);
        addEdges(builder, 7, 15, 34);
        addEdges(builder, 8, 15);
        addEdges(builder, 9, 13, 15, 38);
        addEdges(builder, 10, 11, 15, 42);
        addEdges(builder, 11, 8, 9, 15);
        addEdges(builder, 12, 8, 15);
        addEdges(builder, 13, 8, 15);
        addEdges(builder, 14, 10, 15);
        addEdges(builder, 15);
        addEdges(builder, 16, 1, 2);
        addEdges(builder, 17, 16, 20, 24, 28, 32, 36, 40, 44);
        addEdges(builder, 18, 19, 21);
        addEdges(builder, 19, 22);
        addEdges(builder, 20, 18);
        addEdges(builder, 21);
        addEdges(builder, 22, 23, 25);
        addEdges(builder, 23);
        addEdges(builder, 24, 22);
        addEdges(builder, 25);
        addEdges(builder, 26, 11, 15, 27, 29);
        addEdges(builder, 27, 5, 11, 15);
        addEdges(builder, 28, 26);
        addEdges(builder, 29);
        addEdges(builder, 30, 11, 15, 31, 33);
        addEdges(builder, 31, 3, 4, 11, 15);
        addEdges(builder, 32, 30);
        addEdges(builder, 33);
        addEdges(builder, 34, 15, 35, 37);
        addEdges(builder, 35, 6, 15);
        addEdges(builder, 36, 34);
        addEdges(builder, 37);
        addEdges(builder, 38, 13, 15, 39, 41);
        addEdges(builder, 39, 12, 13, 15);
        addEdges(builder, 40, 38);
        addEdges(builder, 41);
        addEdges(builder, 42, 11, 15, 43, 45);
        addEdges(builder, 43, 5, 11, 15);
        addEdges(builder, 44, 42);
        addEdges(builder, 45);

        var graph = builder.build();
        var dom = GraphUtils.buildDominatorTree(graph);
        assertEquals(0, dom.immediateDominatorOf(8));
        assertEquals(17, dom.immediateDominatorOf(44));
        assertEquals(11, dom.immediateDominatorOf(9));
    }

    private static void addEdges(GraphBuilder builder, int from, int... to) {
        for (int target : to) {
            builder.addEdge(from, target);
        }
    }
}
