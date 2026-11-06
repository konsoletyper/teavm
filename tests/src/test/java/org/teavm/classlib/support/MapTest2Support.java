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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Map;

public class MapTest2Support {

    Map<String, String> map;

    public MapTest2Support(Map<String, String> m) {
        super();
        map = m;
        if (!map.isEmpty()) {
            fail("Map must be empty");
        }
    }

    public void runTest() {
        try {
            map.put("one", "1");
            assertEquals(1, map.size(), "size should be one");
            map.clear();
            assertEquals(0, map.size(), "size should be zero");
            assertTrue(!map.entrySet().iterator().hasNext(), "Should not have entries");
            assertTrue(!map.keySet().iterator().hasNext(), "Should not have keys");
            assertTrue(!map.values().iterator().hasNext(), "Should not have values");
        } catch (UnsupportedOperationException e) {
            // ok
        }

        try {
            map.put("one", "1");
            assertEquals(1, map.size(), "size should be one");
            map.remove("one");
            assertEquals(0, map.size(), "size should be zero");
            assertTrue(!map.entrySet().iterator().hasNext(), "Should not have entries");
            assertTrue(!map.keySet().iterator().hasNext(), "Should not have keys");
            assertTrue(!map.values().iterator().hasNext(), "Should not have values");
        } catch (UnsupportedOperationException e) {
            // ok
        }
    }

}
