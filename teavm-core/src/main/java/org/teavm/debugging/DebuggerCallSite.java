package org.teavm.debugging;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public abstract class DebuggerCallSite {
    static final int NONE = 0;
    static final int STATIC = 1;
    static final int VIRTUAL = 2;

    public abstract void acceptVisitor(DebuggerCallSiteVisitor visitor);
}
