/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.util.logging;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TInteger;
import org.teavm.classlib.java.lang.TObject;

public class TLevel extends TObject implements TSerializable {
    public static final TLevel OFF = new TLevel("OFF", TInteger.MAX_VALUE);
    public static final TLevel SEVERE = new TLevel("SEVERE", 1000);
    public static final TLevel WARNING = new TLevel("WARNING", 900);
    public static final TLevel INFO = new TLevel("INFO", 800);
    public static final TLevel CONFIG = new TLevel("CONFIG", 700);
    public static final TLevel FINE = new TLevel("FINE", 500);
    public static final TLevel FINER = new TLevel("FINER", 400);
    public static final TLevel FINEST = new TLevel("FINEST", 300);
    public static final TLevel ALL = new TLevel("ALL", TInteger.MIN_VALUE);
    private String name;
    private int value;

    protected TLevel(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    @Override
    public final String toString() {
        return name;
    }

    public final int intValue() {
        return value;
    }
}
