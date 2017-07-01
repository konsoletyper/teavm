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
package org.teavm.classlib.java.nio.charset;

public class TCodingErrorAction {
    public static final TCodingErrorAction IGNORE = new TCodingErrorAction("IGNORE");
    public static final TCodingErrorAction REPLACE = new TCodingErrorAction("REPLACE");
    public static final TCodingErrorAction REPORT = new TCodingErrorAction("REPORT");
    private String name;

    public TCodingErrorAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
