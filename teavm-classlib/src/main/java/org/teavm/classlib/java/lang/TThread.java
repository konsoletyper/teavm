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

/**
 *
 * @author Alexey Andreev
 */
public class TThread extends TObject implements TRunnable {
    private static TThread currentThread = new TThread(TString.wrap("main"));
    private TString name;
    private TRunnable target;

    public TThread() {
        this(null, null);
    }

    public TThread(TString name) {
        this(name, null);
    }

    public TThread(TRunnable target) {
        this(null, target);
    }

    public TThread(TString name, TRunnable target) {
        this.name = name;
        this.target = target;
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

    public static void yield() {
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
        return 1;
    }
}
