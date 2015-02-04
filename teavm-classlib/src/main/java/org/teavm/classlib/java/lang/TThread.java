/*
 *  Copyright 2014 Alexey Andreev.
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

import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.runtime.Async;

/**
 *
 * @author Alexey Andreev
 */
public class TThread extends TObject implements TRunnable {
    private static TThread mainThread = new TThread(TString.wrap("main"));
    private static TThread currentThread = mainThread;
    private static long nextId = 1;
    private static int activeCount = 1;
    private long id;
    private TString name;
    private TRunnable target;

    public TThread() {
        this(null, null);
    }

    public TThread(TString name) {
        this(null, name);
    }

    public TThread(TRunnable target) {
        this(target, null );
    }

    public TThread(TRunnable target, TString name ) {
        this.name = name;
        this.target = target;
        id=nextId++;
    }

    @PluggableDependency(ThreadNativeGenerator.class)
    @GeneratedBy(ThreadNativeGenerator.class)
    public native void start();

    private static void launch(TThread thread) {
        try {
            activeCount++;
            setCurrentThread(thread);
            thread.run();
        } finally {
            activeCount--;
            setCurrentThread(mainThread);
        }
        
        
    }
    
    private static void setCurrentThread(TThread thread){
        currentThread = thread;
    }
    private static TThread getMainThread(){
        return mainThread;
    }

    @Override
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    public static TThread currentThread() {
        return currentThread;
    }

    public TString getName() {
        return name;
    }

    @Async
    @GeneratedBy(ThreadNativeGenerator.class)
    public static native void yield();

    public void interrupt() {
    }

    public static boolean interrupted() {
        return false;
    }

    public boolean isInterrupted() {
        return false;
    }

    public static int activeCount() {
        return activeCount;
    }

    public long getId() {
        return id;
    }

    public static boolean holdsLock(@SuppressWarnings("unused") TObject obj) {
        return true;
    }

    public static void sleep(long millis) throws TInterruptedException {
        sleep((double)millis);
    }

    @Async
    @GeneratedBy(ThreadNativeGenerator.class)
    private static native void sleep(double millis) throws TInterruptedException;
}
