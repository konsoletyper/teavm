package org.teavm.javascript.ni;

import org.teavm.codegen.NamingStrategy;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface GeneratorContext {
    String getParameterName(int index);

    NamingStrategy getNaming();
}
