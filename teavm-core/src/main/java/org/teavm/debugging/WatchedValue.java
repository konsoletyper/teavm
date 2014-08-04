package org.teavm.debugging;

import java.util.Map;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public abstract class WatchedValue {
    public abstract String getRepresentation();

    public abstract Map<String, LocalVariable> getProperties();
}
