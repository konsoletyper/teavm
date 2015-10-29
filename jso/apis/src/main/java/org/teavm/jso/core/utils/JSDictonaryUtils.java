/*
 *  Copyright 2015 Jan-Felix Wittmann.
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
package org.teavm.jso.core.utils;

import java.util.Map;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSDictonary;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;

/**
*
* @author Jan-Felix Wittmann
*/
public class JSDictonaryUtils {
    
    private JSDictonaryUtils() {
        
    }
    
    public static <V extends Object> JSDictonary of(JSDictonary dict, Map<String, V> map,
            JSFromObjectMapper<V, JSObject> mapper) {
        for (Map.Entry<String, V> entry : map.entrySet()) {
            dict.put(entry.getKey(), mapper.apply(entry.getValue()));
        }
        return dict;
    }

    private static <V extends Object> JSDictonary of(Map<String, V> map, JSFromObjectMapper<V, JSObject> mapper) {
        final JSDictonary dict = JSDictonary.create();
        return of(dict, map, mapper);
    }

    public static JSDictonary ofStringMap(Map<String, String> map) {
        return of(map, value -> JSString.valueOf(value));
    }

    public static JSDictonary ofIntMap(Map<String, Integer> map) {
        return of(map, value -> JSNumber.valueOf(value));
    }

    public static JSDictonary ofFloatMap(Map<String, Float> map) {
        return of(map, value -> JSNumber.valueOf(value));
    }

    public static JSDictonary ofDoubleMap(Map<String, Double> map) {
        return of(map, value -> JSNumber.valueOf(value));
    }
}
