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

import java.util.Locale;
import org.threeten.bp.chrono.Chronology;

/**
 * The Service Provider Interface (SPI) to be implemented by classes providing
 * date-time formatting information.
 *
 * <h3>Specification for implementors</h3>
 * This interface is a service provider that can be called by multiple threads.
 * Implementations must be thread-safe.
 * Implementations should cache the returned formatters.
 */
 abstract class DateTimeFormatStyleProvider {

     /**
      * Gets the provider.
      *
      * @return the provider, not null
      */
     static DateTimeFormatStyleProvider getInstance() {
         return new SimpleDateTimeFormatStyleProvider();
     }

     /**
      * Gets the available locales.
      * 
      * @return the locales
      */
     public Locale[] getAvailableLocales() {
         throw new UnsupportedOperationException();
     }

   /**
     * Gets a localized date, time or date-time formatter.
     * <p>
     * The formatter will be the most appropriate to use for the date and time style in the locale.
     * For example, some locales will use the month name while others will use the number.
     *
     * @param dateStyle  the date formatter style to obtain, null to obtain a time formatter
     * @param timeStyle  the time formatter style to obtain, null to obtain a date formatter
     * @param chrono  the chronology to use, not null
     * @param locale  the locale to use, not null
     * @return the date-time formatter, not null
     * @throws IllegalArgumentException if both format styles are null
     * @throws IllegalArgumentException if the locale is not a recognized locale
     */
    public abstract DateTimeFormatter getFormatter(
            FormatStyle dateStyle, FormatStyle timeStyle, Chronology chrono, Locale locale);

}
