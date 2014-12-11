package org.teavm.jso.test;

import org.teavm.jso.JSConstructor;
import org.teavm.jso.JSObject;

/**
 *
 * @author Alexey Andreev
 */
public interface Window extends JSObject {
    @JSConstructor("RegExp")
    RegExp createRegExp(String regex);
}
