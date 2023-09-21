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

import static org.junit.Assert.assertTrue;
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
        assertTrue("CollectionTest - a) add did not work", col.add(101));
        assertTrue("CollectionTest - b) add did not work", col.contains(101));

        // remove
        assertTrue("CollectionTest - a) remove did not work", col.remove(101));
        assertTrue("CollectionTest - b) remove did not work", !col.contains(101));

        // addAll
        assertTrue("CollectionTest - a) addAll failed", col.addAll(myCollection));
        assertTrue("CollectionTest - b) addAll failed", col.containsAll(myCollection));

        // containsAll
        assertTrue("CollectionTest - a) containsAll failed", col.containsAll(myCollection));
        col.remove(101);
        assertTrue("CollectionTest - b) containsAll failed", !col.containsAll(myCollection));

        // removeAll
        assertTrue("CollectionTest - a) removeAll failed", col.removeAll(myCollection));
        assertTrue("CollectionTest - b) removeAll failed", !col.removeAll(myCollection));
        assertTrue("CollectionTest - c) removeAll failed", !col.contains(102));
        assertTrue("CollectionTest - d) removeAll failed", !col.contains(103));

        // retianAll
        col.addAll(myCollection);
        assertTrue("CollectionTest - a) retainAll failed", col.retainAll(myCollection));
        assertTrue("CollectionTest - b) retainAll failed", !col.retainAll(myCollection));
        assertTrue("CollectionTest - c) retainAll failed", col.containsAll(myCollection));
        assertTrue("CollectionTest - d) retainAll failed", !col.contains(0));
        assertTrue("CollectionTest - e) retainAll failed", !col.contains(50));

        // clear
        col.clear();
        assertTrue("CollectionTest - a) clear failed", col.isEmpty());
        assertTrue("CollectionTest - b) clear failed", !col.contains(101));
    }
}
