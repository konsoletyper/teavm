package org.teavm.debugging;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class LineNumberIterator {
    private DebugInformation debugInformation;
    private int index;

    LineNumberIterator(DebugInformation debugInformation) {
        this.debugInformation = debugInformation;
    }

    public boolean isEndReached() {
        return index < debugInformation.lineMapping.size();
    }

    public GeneratedLocation getLocation() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return DebugInformation.key(debugInformation.lineMapping.get(index));
    }

    public int getLineNumber() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return debugInformation.lineMapping.get(index).get(2);
    }

    public void next() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        ++index;
    }
}
