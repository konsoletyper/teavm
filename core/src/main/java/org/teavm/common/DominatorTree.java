/*
 *  Copyright 2011 Alexey Andreev.
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

public interface DominatorTree {
    boolean directlyDominates(int a, int b);

    int commonDominatorOf(int a, int b);

    boolean dominates(int a, int b);

    int immediateDominatorOf(int a);

    int levelOf(int a);

    default int commonDominatorOf(int[] nodes) {
        if (nodes.length == 0) {
            return -1;
        }

        int result = nodes[0];
        for (int i = 1; i < nodes.length; ++i) {
            result = commonDominatorOf(result, nodes[i]);
        }
        return result;
    }
}
