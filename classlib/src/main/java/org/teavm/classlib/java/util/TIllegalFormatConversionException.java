/*
 *  Copyright 2017 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.util;

public class TIllegalFormatConversionException extends TIllegalFormatException {
    private char conversion;
    private Class<?> argumentClass;

    public TIllegalFormatConversionException(char conversion, Class<?> argumentClass) {
        super("Can't format argument of " + argumentClass + " using " + conversion + " conversion");
        this.conversion = conversion;
        this.argumentClass = argumentClass;
    }

    public char getConversion() {
        return conversion;
    }

    public Class<?> getArgumentClass() {
        return argumentClass;
    }
}
