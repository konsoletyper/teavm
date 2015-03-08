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
package org.teavm.chromerdp;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Alexey Andreev
 */
public class RDPScope extends AbstractMap<String, RDPLocalVariable> {
    private AtomicReference<Map<String, RDPLocalVariable>> backingMap = new AtomicReference<>();
    private ChromeRDPDebugger debugger;
    private String id;

    public RDPScope(ChromeRDPDebugger debugger, String id) {
        this.debugger = debugger;
        this.id = id;
    }

    @Override
    public Set<Entry<String, RDPLocalVariable>> entrySet() {
        initBackingMap();
        return backingMap.get().entrySet();
    }

    @Override
    public int size() {
        initBackingMap();
        return backingMap.get().size();
    }

    @Override
    public RDPLocalVariable get(Object key) {
        initBackingMap();
        return backingMap.get().get(key);
    }

    private void initBackingMap() {
        if (backingMap.get() != null) {
            return;
        }
        Map<String, RDPLocalVariable> newBackingMap = new HashMap<>();
        if (id != null) {
            for (RDPLocalVariable variable : debugger.getScope(id)) {
                newBackingMap.put(variable.getName(), variable);
            }
        }
        backingMap.compareAndSet(null, newBackingMap);
    }
}
