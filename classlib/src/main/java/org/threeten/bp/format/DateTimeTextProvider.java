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

import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import org.threeten.bp.temporal.TemporalField;

/**
 * The Service Provider Interface (SPI) to be implemented by classes providing
 * the textual form of a date-time field.
 *
 * <h3>Specification for implementors</h3>
 * This interface is a service provider that can be called by multiple threads.
 * Implementations must be thread-safe.
 * Implementations should cache the textual information.
 * <p>
 * This class has been made pubilc primarily for the benefit of Android.
 */
public abstract class DateTimeTextProvider {

    private static DateTimeTextProvider mutableProvider;

    /**
     * Gets the provider.
     *
     * @return the provider, not null
     */
    static DateTimeTextProvider getInstance() {
        return ProviderSingleton.PROVIDER;
    }

    /**
     * Sets the provider to use.
     * <p>
     * This can only be invoked before {@link DateTimeTextProvider} class is used for formatting/parsing.
     * Invoking this method at a later point will throw an exception.
     * 
     * @param provider  the provider to use
     * @throws IllegalStateException if initialization has already occurred or another provider has been set
     */
    public static void setInitializer(DateTimeTextProvider provider) {
        if (mutableProvider != null) {
            throw new IllegalStateException("Provider was already set, possibly with a default during initialization");
        }
        mutableProvider = provider;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the text for the specified field, locale and style
     * for the purpose of printing.
     * <p>
     * The text associated with the value is returned.
     * The null return value should be used if there is no applicable text, or
     * if the text would be a numeric representation of the value.
     *
     * @param field  the field to get text for, not null
     * @param value  the field value to get text for, not null
     * @param style  the style to get text for, not null
     * @param locale  the locale to get text for, not null
     * @return the text for the field value, null if no text found
     */
    public abstract String getText(TemporalField field, long value, TextStyle style, Locale locale);

    /**
     * Gets an iterator of text to field for the specified field, locale and style
     * for the purpose of parsing.
     * <p>
     * The iterator must be returned in order from the longest text to the shortest.
     * <p>
     * The null return value should be used if there is no applicable parsable text, or
     * if the text would be a numeric representation of the value.
     * Text can only be parsed if all the values for that field-style-locale combination are unique.
     *
     * @param field  the field to get text for, not null
     * @param style  the style to get text for, null for all parsable text
     * @param locale  the locale to get text for, not null
     * @return the iterator of text to field pairs, in order from longest text to shortest text,
     *  null if the field or style is not parsable
     */
    public abstract Iterator<Entry<String, Long>> getTextIterator(TemporalField field, TextStyle style, Locale locale);

    //-----------------------------------------------------------------------
    // use JVM class initializtion to lock the singleton without additional synchronization
    static class ProviderSingleton {
        static final DateTimeTextProvider PROVIDER = initialize();

        // initialize the provider
        static DateTimeTextProvider initialize() {
            // Set the default initializer if none has been provided yet
            mutableProvider = new SimpleDateTimeTextProvider();
            return mutableProvider;
        }
    }

}
