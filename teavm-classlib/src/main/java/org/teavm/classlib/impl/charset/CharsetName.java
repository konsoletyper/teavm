package org.teavm.classlib.impl.charset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Alexey Andreev
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface CharsetName {
    String value();
}
