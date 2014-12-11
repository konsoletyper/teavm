package org.teavm.jso.test;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public interface RegExp extends JSObject {
    @JSProperty
    String getSource();
}
