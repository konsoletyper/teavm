/*
 *  Copyright 2013 Steve Hannah.
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
package org.teavm.classlib.java.lang;

import org.teavm.interop.DelegateTo;
import org.teavm.interop.Platforms;
import org.teavm.interop.SupportedOn;
import org.teavm.jso.browser.Navigator;
import org.teavm.runtime.GC;

/**
 * A dummy class for compatibility.  Currently these methods don't actually
 * do anything.
 *
 * @author shannah
 */
public class TRuntime {
    private static final TRuntime instance = new TRuntime();

    /**
     * Terminates the currently running Java application. This method never returns normally.
     * The argument serves as a status code; by convention, a nonzero status code indicates abnormal termination.
     */
    public void exit(@SuppressWarnings("unused") int status) {
    }

    /**
     * Returns the amount of free memory in the system. Calling the gc method
     * may result in increasing the value returned by freeMemory.
     */
    @DelegateTo("freeMemoryLowLevel")
    public long freeMemory() {
        return Integer.MAX_VALUE;
    }

    private long freeMemoryLowLevel() {
        return GC.getFreeMemory();
    }

    /**
     * Runs the garbage collector. Calling this method suggests that the Java
     * Virtual Machine expend effort toward recycling unused objects in order to
     * make the memory they currently occupy available for quick reuse. When
     * control returns from the method call, the Java Virtual Machine has made
     * its best effort to recycle all discarded objects. The name gc stands for
     * "garbage collector". The Java Virtual Machine performs this recycling
     * process automatically as needed even if the gc method is not invoked
     * explicitly. The method System.js.gc() is the conventional and convenient
     * means of invoking this method.
     */
    public void gc() {
        System.gc();
    }

    /**
     * Returns the runtime object associated with the current Java application.
     * Most of the methods of class Runtime are instance methods and must be
     * invoked with respect to the current runtime object.
     */
    public static TRuntime getRuntime() {
        return instance;
    }

    /**
     * Returns the total amount of memory in the Java Virtual Machine. The value
     * returned by this method may vary over time, depending on the host
     * environment. Note that the amount of memory required to hold an object of
     * any given type may be implementation-dependent.
     */
    @DelegateTo("totalMemoryLowLevel")
    public long totalMemory() {
        return Integer.MAX_VALUE;
    }

    private long totalMemoryLowLevel() {
        return GC.availableBytes();
    }

    @SupportedOn(Platforms.JAVASCRIPT)
    public int availableProcessors() {
        return Navigator.hardwareConcurrency();
    }
}