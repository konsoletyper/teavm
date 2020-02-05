/*
 *  Copyright 2020, adopted to TeaVM by Joerg Hohwiller
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
/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.teavm.classlib.java.time.format;

import java.util.Locale;

import org.junit.Before;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZonedDateTime;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;

public class AbstractTestPrinterParser {

    protected TDateTimePrintContext printEmptyContext;

    protected TDateTimePrintContext printContext;

    protected TDateTimeParseContext parseContext;

    protected StringBuilder buf;

    @Before
    public void setUp() {

        this.printEmptyContext = new TDateTimePrintContext(EMPTY, Locale.ENGLISH, TDecimalStyle.STANDARD);
        TZonedDateTime zdt = TLocalDateTime.of(2011, 6, 30, 12, 30, 40, 0).atZone(TZoneId.of("Europe/Paris"));
        this.printContext = new TDateTimePrintContext(zdt, Locale.ENGLISH, TDecimalStyle.STANDARD);
        this.parseContext = new TDateTimeParseContext(Locale.ENGLISH, TDecimalStyle.STANDARD, TIsoChronology.INSTANCE);
        this.buf = new StringBuilder();
    }

    private static final TTemporalAccessor EMPTY = new TTemporalAccessor() {
        @Override
        public boolean isSupported(TTemporalField field) {

            return true;
        }

        @Override
        public long getLong(TTemporalField field) {

            throw new TDateTimeException("Mock");
        }
    };
}
