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
import org.teavm.classlib.java.lang.TString;
import org.teavm.javascript.ni.Rename;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TLevel extends TObject implements TSerializable {
    public static final TLevel OFF = new TLevel(TString.wrap("OFF"), TInteger.MAX_VALUE);
    public static final TLevel SEVERE = new TLevel(TString.wrap("SEVERE"), 1000);
    public static final TLevel WARNING = new TLevel(TString.wrap("WARNING"), 900);
    public static final TLevel INFO = new TLevel(TString.wrap("INFO"), 800);
    public static final TLevel CONFIG = new TLevel(TString.wrap("CONFIG"), 700);
    public static final TLevel FINE = new TLevel(TString.wrap("FINE"), 500);
    public static final TLevel FINER = new TLevel(TString.wrap("FINER"), 400);
    public static final TLevel FINEST = new TLevel(TString.wrap("FINEST"), 300);
    public static final TLevel ALL = new TLevel(TString.wrap("FINEST"), TInteger.MIN_VALUE);
    private TString name;
    private int value;

    protected TLevel(TString name, int value) {
        this.name = name;
        this.value = value;
    }

    public TString getName() {
        return name;
    }

    @Override
    @Rename("toString")
    public final TString toString0() {
        return name;
    }

    public final int intValue() {
        return value;
    }
}
