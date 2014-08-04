package org.teavm.debugging;

import java.util.Map;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface JavaScriptValue {
    String getRepresentation();

    Map<String, JavaScriptVariable> getProperties();
}
