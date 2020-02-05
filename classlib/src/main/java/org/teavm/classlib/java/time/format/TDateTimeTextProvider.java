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

import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;

import org.teavm.classlib.java.time.temporal.TTemporalField;

public abstract class TDateTimeTextProvider {

    private static TDateTimeTextProvider MUTABLE_PROVIDER = null;

    static TDateTimeTextProvider getInstance() {

        return ProviderSingleton.PROVIDER;
    }

    public static void setInitializer(TDateTimeTextProvider provider) {

        if (MUTABLE_PROVIDER != null) {
            throw new IllegalStateException("Provider was already set, possibly with a default during initialization");
        }
    }

    public abstract String getText(TTemporalField field, long value, TTextStyle style, Locale locale);

    public abstract Iterator<Entry<String, Long>> getTextIterator(TTemporalField field, TTextStyle style,
            Locale locale);

    // use JVM class initializtion to lock the singleton without additional synchronization
    static class ProviderSingleton {
        static final TDateTimeTextProvider PROVIDER = initialize();

        // initialize the provider
        static TDateTimeTextProvider initialize() {

            // Set the default initializer if none has been provided yet
            MUTABLE_PROVIDER = new TSimpleDateTimeTextProvider();
            return MUTABLE_PROVIDER;
        }
    }

}
