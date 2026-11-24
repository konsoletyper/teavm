/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.classlib.java.nio.charset.impl;

import org.teavm.classlib.java.nio.charset.TCharset;
import org.teavm.classlib.java.nio.charset.TCharsetDecoder;
import org.teavm.classlib.java.nio.charset.TCharsetEncoder;

public class TAsciiCharset extends TCharset {
    public TAsciiCharset() {
        super("US-ASCII", new String[] { "646", "ANSI_X3.4-1968", "ANSI_X3.4-1986", "ASCII", "IBM367", "ISO646-US",
                "ISO_646.irv:1991", "ascii7", "cp367", "csASCII", "iso-ir-6", "iso_646.irv:1983", "us" });
    }

    @Override
    public boolean contains(TCharset cs) {
        return cs == this;
    }

    @Override
    public TCharsetDecoder newDecoder() {
        return new TAsciiDecoder(this);
    }

    @Override
    public TCharsetEncoder newEncoder() {
        return new TAsciiEncoder(this);
    }
}
