/*
 *  Copyright 2017 Alexey Andreev.
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

/*
 *  Copyright 2014 Alexey Andreev.
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
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.teavm.classlib.support;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Collection;
import java.util.TreeSet;

public class CollectionTestSupport {

    Collection<Integer> col; // must contain the Integers 0 to 99

    public CollectionTestSupport() {
    }

    public CollectionTestSupport(Collection<Integer> c) {
        col = c;
    }

    public void runTest() {
        new UnmodifiableCollectionTestSupport(col).runTest();

        // setup
        Collection<Integer> myCollection = new TreeSet<>();
        myCollection.add(101);
        myCollection.add(102);
        myCollection.add(103);

        // add
        assertTrue(col.add(101), "CollectionTest - a) add did not work");
        assertTrue(col.contains(101), "CollectionTest - b) add did not work");

        // remove
        assertTrue(col.remove(101), "CollectionTest - a) remove did not work");
        assertTrue(!col.contains(101), "CollectionTest - b) remove did not work");

        // addAll
        assertTrue(col.addAll(myCollection), "CollectionTest - a) addAll failed");
        assertTrue(col.containsAll(myCollection), "CollectionTest - b) addAll failed");

        // containsAll
        assertTrue(col.containsAll(myCollection), "CollectionTest - a) containsAll failed");
        col.remove(101);
        assertTrue(!col.containsAll(myCollection), "CollectionTest - b) containsAll failed");

        // removeAll
        assertTrue(col.removeAll(myCollection), "CollectionTest - a) removeAll failed");
        assertTrue(!col.removeAll(myCollection), "CollectionTest - b) removeAll failed");
        assertTrue(!col.contains(102), "CollectionTest - c) removeAll failed");
        assertTrue(!col.contains(103), "CollectionTest - d) removeAll failed");

        // retianAll
        col.addAll(myCollection);
        assertTrue(col.retainAll(myCollection), "CollectionTest - a) retainAll failed");
        assertTrue(!col.retainAll(myCollection), "CollectionTest - b) retainAll failed");
        assertTrue(col.containsAll(myCollection), "CollectionTest - c) retainAll failed");
        assertTrue(!col.contains(0), "CollectionTest - d) retainAll failed");
        assertTrue(!col.contains(50), "CollectionTest - e) retainAll failed");

        // clear
        col.clear();
        assertTrue(col.isEmpty(), "CollectionTest - a) clear failed");
        assertTrue(!col.contains(101), "CollectionTest - b) clear failed");
    }
}
