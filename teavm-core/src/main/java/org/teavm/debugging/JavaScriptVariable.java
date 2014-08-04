package org.teavm.debugging;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface JavaScriptVariable {
    String getName();

    JavaScriptValue getValue();
}
