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

public class TUTF16Charset extends TCharset {
    private boolean bom;
    private boolean littleEndian;

    public TUTF16Charset(String canonicalName, boolean bom, boolean littleEndian) {
        super(canonicalName, new String[0]);
        this.bom = bom;
        this.littleEndian = littleEndian;
    }

    @Override
    public boolean contains(TCharset cs) {
        return cs == this;
    }

    @Override
    public TCharsetDecoder newDecoder() {
        return new TUTF16Decoder(this, bom, littleEndian);
    }

    @Override
    public TCharsetEncoder newEncoder() {
        return new TUTF16Encoder(this, bom, littleEndian);
    }
}
