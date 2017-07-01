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
package org.teavm.classlib.java.util;

import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TRunnable;

public abstract class TTimerTask extends TObject implements TRunnable {
    TTimer timer;
    int nativeTimerId = -1;

    public boolean cancel() {
        if (timer == null) {
            return false;
        }
        timer = null;
        return true;
    }

    static void performOnce(TTimerTask task) {
        if (task.timer != null) {
            task.run();
            task.timer.tasks.remove(task);
            task.timer = null;
        }
    }
}
