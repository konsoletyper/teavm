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
package org.threeten.bp.format;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.threeten.bp.chrono.Chronology;

/**
 * The Service Provider Implementation to obtain date-time formatters for a style.
 * <p>
 * This implementation is based on extraction of data from a {@link SimpleDateFormat}.
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 */
final class SimpleDateTimeFormatStyleProvider extends DateTimeFormatStyleProvider {
    // TODO: Better implementation based on CLDR

    /** Cache of formatters. */
    private static final Map<String, Object> FORMATTER_CACHE = new HashMap<>();

    @Override
    public Locale[] getAvailableLocales() {
        return DateFormat.getAvailableLocales();
    }

    @Override
    public DateTimeFormatter getFormatter(
            FormatStyle dateStyle, FormatStyle timeStyle, Chronology chrono, Locale locale) {
        if (dateStyle == null && timeStyle == null) {
            throw new IllegalArgumentException("Date and Time style must not both be null");
        }
        String key = chrono.getId() + '|' + locale.toString() + '|' + dateStyle + timeStyle;
        Object cached = FORMATTER_CACHE.get(key);
        if (cached != null) {
            if (cached.equals("")) {
                throw new IllegalArgumentException("Unable to convert DateFormat to DateTimeFormatter");
            }
            return (DateTimeFormatter) cached;
        }
        DateFormat dateFormat;
        if (dateStyle != null) {
            if (timeStyle != null) {
                dateFormat = DateFormat.getDateTimeInstance(convertStyle(dateStyle), convertStyle(timeStyle), locale);
            } else {
                dateFormat = DateFormat.getDateInstance(convertStyle(dateStyle), locale);
            }
        } else {
            dateFormat = DateFormat.getTimeInstance(convertStyle(timeStyle), locale);
        }
        if (dateFormat instanceof SimpleDateFormat) {
            String pattern = ((SimpleDateFormat) dateFormat).toPattern();
            DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern(pattern).toFormatter(locale);
            FORMATTER_CACHE.putIfAbsent(key, formatter);
            return formatter;
        }
        FORMATTER_CACHE.putIfAbsent(key, "");
        throw new IllegalArgumentException("Unable to convert DateFormat to DateTimeFormatter");
    }

    /**
     * Converts the enum style to the old format style.
     * @param style  the enum style, not null
     * @return the int style
     */
    private int convertStyle(FormatStyle style) {
        return style.ordinal();  // indices happen to align
    }

}
