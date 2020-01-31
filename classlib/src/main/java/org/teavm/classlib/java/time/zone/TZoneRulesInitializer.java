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
package org.threeten.bp.zone;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controls how the time-zone rules are initialized.
 * <p>
 * The default behavior is to use {@link ServiceLoader} to find instances of {@link ZoneRulesProvider}.
 * Use the {@link #setInitializer(ZoneRulesInitializer)} method to replace this behavior.
 * The initializer instance must perform the work of creating the {@code ZoneRulesProvider} within
 * the {@link #initializeProviders()} method to ensure that the provider is not initialized too early.
 * <p>
 * <b>The initializer must be set before class loading of any other ThreeTen-Backport class to have any effect!</b>
 * <p>
 * This class has been added primarily for the benefit of Android.
 */
public abstract class ZoneRulesInitializer {

    /**
     * An instance that does nothing.
     * Call {@link #setInitializer(ZoneRulesInitializer)} with this instance to
     * block the service loader search. This will leave the system with no providers.
     */
    public static final ZoneRulesInitializer DO_NOTHING = new DoNothingZoneRulesInitializer();

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final AtomicReference<ZoneRulesInitializer> INITIALIZER = new AtomicReference<ZoneRulesInitializer>();

    /**
     * Sets the initializer to use.
     * <p>
     * This can only be invoked before the {@link ZoneRulesProvider} class is loaded.
     * Invoking this method at a later point will throw an exception.
     * 
     * @param initializer  the initializer to use
     * @throws IllegalStateException if initialization has already occurred or another initializer has been set
     */
    public static void setInitializer(ZoneRulesInitializer initializer) {
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

    /**
     * Initialize the providers.
     * <p>
     * The implementation should perform whatever work is necessary to initialize the providers.
     * This will result in one or more calls to {@link ZoneRulesProvider#registerProvider(ZoneRulesProvider)}.
     * <p>
     * It is vital that the instance of {@link ZoneRulesProvider} is not created until this method is invoked.
     * <p>
     * It is guaranteed that this method will be invoked once and only once.
     */
    protected abstract void initializeProviders();

    //-----------------------------------------------------------------------
    /**
     * Implementation that does nothing.
     */
    static class DoNothingZoneRulesInitializer extends ZoneRulesInitializer {

        @Override
        protected void initializeProviders() {
        }
    }

    /**
     * Implementation that uses the service loader.
     */
    static class ServiceLoaderZoneRulesInitializer extends ZoneRulesInitializer {

        @Override
        protected void initializeProviders() {
            ServiceLoader<ZoneRulesProvider> loader = ServiceLoader.load(ZoneRulesProvider.class, ZoneRulesProvider.class.getClassLoader());
            for (ZoneRulesProvider provider : loader) {
                try {
                    ZoneRulesProvider.registerProvider(provider);
                } catch (ServiceConfigurationError ex) {
                    if (!(ex.getCause() instanceof SecurityException)) {
                        throw ex;
                    }
                }
            }
        }
    }

}
