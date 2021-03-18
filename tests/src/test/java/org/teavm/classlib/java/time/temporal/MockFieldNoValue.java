/*
 *  Copyright 2020 Alexey Andreev.
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
package org.teavm.classlib.java.time.temporal;

import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.WEEKS;
import java.time.DateTimeException;
import java.time.format.ResolverStyle;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;
import java.util.Locale;
import java.util.Map;

/**
 * Mock DateTimeField that returns null.
 */
public enum MockFieldNoValue implements TemporalField {

    INSTANCE;

    @Override
    public String toString() {
        return null;
    }

    @Override
    public TemporalUnit getBaseUnit() {
        return WEEKS;
    }

    @Override
    public TemporalUnit getRangeUnit() {
        return MONTHS;
    }

    @Override
    public ValueRange range() {
        return ValueRange.of(1, 20);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isDateBased() {
        return true;
    }

    @Override
    public boolean isTimeBased() {
        return false;
    }

    @Override
    public boolean isSupportedBy(TemporalAccessor dateTime) {
        return true;
    }

    @Override
    public ValueRange rangeRefinedBy(TemporalAccessor dateTime) {
        return ValueRange.of(1, 20);
    }

    @Override
    public long getFrom(TemporalAccessor dateTime) {
        throw new DateTimeException("Mock");
    }

    @Override
    public <R extends Temporal> R adjustInto(R dateTime, long newValue) {
        throw new DateTimeException("Mock");
    }

    @Override
    public String getDisplayName(Locale locale) {
        return "Mock";
    }

    //-----------------------------------------------------------------------
    @Override
    public TemporalAccessor resolve(Map<TemporalField, Long> fieldValues,
                    TemporalAccessor partialTemporal, ResolverStyle resolverStyle) {
        return null;
    }

}
