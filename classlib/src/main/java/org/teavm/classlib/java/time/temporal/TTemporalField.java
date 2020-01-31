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

import org.teavm.classlib.java.util.TLocale;
import java.util.Map;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.format.TResolverStyle;

public interface TTemporalField {

    TTemporalUnit getBaseUnit();

    TTemporalUnit getRangeUnit();

    //-----------------------------------------------------------------------
    TValueRange range();

    //-----------------------------------------------------------------------
    boolean isDateBased();

    boolean isTimeBased();

    //-----------------------------------------------------------------------
    boolean isSupportedBy(TTemporalAccessor temporal);

    TValueRange rangeRefinedBy(TTemporalAccessor temporal);

    long getFrom(TTemporalAccessor temporal);

    String getDisplayName(TLocale locale);

    <R extends TTemporal> R adjustInto(R temporal, long newValue);

    TTemporalAccessor resolve(
                    Map<TTemporalField, Long> fieldValues,
                    TTemporalAccessor partialTemporal,
                    TResolverStyle resolverStyle);

}
