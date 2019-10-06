/*
 *  Copyright 2015 Alexey Andreev.
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

public class TUUID {

    private String value;

    private TUUID(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TUUID) {
            TUUID other = (TUUID) o;
            return value.equals(other.value);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    public static TUUID randomUUID() {
        String value = s4() + s4() + "-" + s4() + "-" + s4() + "-" + s4() + "-" + s4() + s4() + s4();
        return new TUUID(value);
    }

    private static String s4() {
        return Integer.toString((int) Math.floor((1 + Math.random()) * 65536), 16).substring(1);
    }

    public static TUUID fromString(String value) {
        return new TUUID(value);
    }
}
