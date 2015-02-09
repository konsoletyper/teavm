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

<<<<<<< HEAD
import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.runtime.Async;
=======
import org.teavm.dom.browser.TimerHandler;
import org.teavm.dom.browser.Window;
import org.teavm.javascript.spi.Async;
import org.teavm.jso.JS;
import org.teavm.platform.async.AsyncCallback;
>>>>>>> dd25ae4759716d735fe6f93a54c8bfab2e7fc7bf

/**
 *
 * @author Alexey Andreev
 */
public class TThread extends TObject implements TRunnable {
<<<<<<< HEAD
    private static TThread mainThread = new TThread(TString.wrap("main"));
    private static TThread currentThread = mainThread;
    private static long nextId = 1;
    private static int activeCount = 1;
    private long id;
=======
    private static Window window = (Window)JS.getGlobal();
    private static TThread currentThread = new TThread(TString.wrap("main"));
>>>>>>> dd25ae4759716d735fe6f93a54c8bfab2e7fc7bf
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
    public static native void yield();

    private static void yield(final AsyncCallback<Void> callback) {
        window.setTimeout(new TimerHandler() {
            @Override public void onTimer() {
                callback.complete(null);
            }
        }, 0);
    }

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

    public static boolean holdsLock(TObject obj) {
        return TObject.holdsLock(obj);
    }

    @Async
<<<<<<< HEAD
    @GeneratedBy(ThreadNativeGenerator.class)
    private static native void sleep(double millis) throws TInterruptedException;
    
    
=======
    public static native void sleep(long millis) throws TInterruptedException;

    private static void sleep(long millis, final AsyncCallback<Void> callback) {
        window.setTimeout(new TimerHandler() {
            @Override public void onTimer() {
                callback.complete(null);
            }
        }, millis);
    }
>>>>>>> dd25ae4759716d735fe6f93a54c8bfab2e7fc7bf
}
