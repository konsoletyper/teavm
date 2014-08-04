package org.teavm.chromerdp;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class RDPScope extends AbstractMap<String, RDPLocalVariable> {
    private volatile Map<String, RDPLocalVariable> backingMap;
    private ChromeRDPDebugger debugger;
    private String id;

    public RDPScope(ChromeRDPDebugger debugger, String id) {
        this.debugger = debugger;
        this.id = id;
    }

    @Override
    public Set<Entry<String, RDPLocalVariable>> entrySet() {
        initBackingMap();
        return backingMap.entrySet();
    }

    @Override
    public int size() {
        initBackingMap();
        return backingMap.size();
    }

    @Override
    public RDPLocalVariable get(Object key) {
        initBackingMap();
        return backingMap.get(key);
    }

    private void initBackingMap() {
        if (backingMap != null) {
            return;
        }
        if (id == null) {
            backingMap = new HashMap<>();
        }
        Map<String, RDPLocalVariable> newBackingMap = new HashMap<>();
        for (RDPLocalVariable variable : debugger.getScope(id)) {
            newBackingMap.put(variable.getName(), variable);
        }
        backingMap = newBackingMap;
    }
}
