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
package org.teavm.classlib.java.time.zone;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class TZoneRulesInitializer {

    public static final TZoneRulesInitializer DO_NOTHING = new DoNothingZoneRulesInitializer();

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final AtomicReference<TZoneRulesInitializer> INITIALIZER = new AtomicReference<TZoneRulesInitializer>();

    public static void setInitializer(TZoneRulesInitializer initializer) {
        if (INITIALIZED.get()) {
            throw new IllegalStateException("Already initialized");
        }
        if (!INITIALIZER.compareAndSet(null, initializer)) {
            throw new IllegalStateException("Initializer was already set, possibly with a default during initialization");
        }
    }

    //-----------------------------------------------------------------------
    // initialize the providers
    static void initialize() {
        if (INITIALIZED.getAndSet(true)) {
            throw new IllegalStateException("Already initialized");
        }
        // Set the default initializer if none has been provided yet.
        INITIALIZER.compareAndSet(null, new ServiceLoaderZoneRulesInitializer());
        INITIALIZER.get().initializeProviders();
    }

    protected abstract void initializeProviders();

    //-----------------------------------------------------------------------
    static class DoNothingZoneRulesInitializer extends TZoneRulesInitializer {

        @Override
        protected void initializeProviders() {
        }
    }

    static class ServiceLoaderZoneRulesInitializer extends TZoneRulesInitializer {

        @Override
        protected void initializeProviders() {
            ServiceLoader<TZoneRulesProvider> loader = ServiceLoader.load(TZoneRulesProvider.class, TZoneRulesProvider.class.getClassLoader());
            for (TZoneRulesProvider provider : loader) {
                try {
                    TZoneRulesProvider.registerProvider(provider);
                } catch (ServiceConfigurationError ex) {
                    if (!(ex.getCause() instanceof SecurityException)) {
                        throw ex;
                    }
                }
            }
        }
    }

}
