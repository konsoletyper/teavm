/*
 *  Copyright 2016 Liraz.
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
package org.teavm.jso.dom.html.use;

import java.util.Objects;

public interface UseHTMLValue<V> {

    /**
     * Retrieves the value.
     *
     * @return the value
     */
    V getValue();

    /**
     * A utility to get the value out of the enum, but with a safe check for null.
     *
     * @param value - the enum value to call a getValue() on
     * @return the value.
     */
    static <H, V extends Enum & UseHTMLValue<H>> H getHtmlValue(V value) {
        return value != null ? value.getValue() : null;
    }

    /**
     * Converting an HTML value into a typed enum.
     *
     * @param type      - the enum type
     * @param htmlValue - the html value
     * @return the enum value that matched the htmlValue
     */
    static <H, V extends Enum & UseHTMLValue<H>> V toEnumValue(Class<V> type, H htmlValue) {
        for (V value : type.getEnumConstants()) {
            if (Objects.equals(value.getValue(), htmlValue)) {
                return value;
            }
        }
        return null;
    }
}
