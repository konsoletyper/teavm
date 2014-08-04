package org.teavm.debugging;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class LocalVariable {
    private String name;
    private WatchedValue value;

    LocalVariable(String name, WatchedValue value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public WatchedValue getValue() {
        return value;
    }
}
