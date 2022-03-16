/*
 *  Copyright 2018 Alexey Andreev.
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

import org.teavm.classlib.java.nio.charset.impl.TAsciiCharset;
import org.teavm.classlib.java.nio.charset.impl.TIso8859Charset;
import org.teavm.classlib.java.nio.charset.impl.TUTF16Charset;
import org.teavm.classlib.java.nio.charset.impl.TUTF8Charset;

public final class TStandardCharsets {
    private TStandardCharsets() {
    }

    public static final TCharset UTF_8 = TUTF8Charset.INSTANCE;
    public static final TCharset US_ASCII = new TAsciiCharset();
    public static final TCharset ISO_8859_1 = new TIso8859Charset();
    public static final TCharset UTF_16 = new TUTF16Charset("UTF-16", true, false);
    public static final TCharset UTF_16BE = new TUTF16Charset("UTF-16BE", false, false);
    public static final TCharset UTF_16LE = new TUTF16Charset("UTF-16LE", false, true);
}
