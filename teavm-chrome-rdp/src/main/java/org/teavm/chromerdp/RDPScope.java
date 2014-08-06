package org.teavm.chromerdp;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
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
