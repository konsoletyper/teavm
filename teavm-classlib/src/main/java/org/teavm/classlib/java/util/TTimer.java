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

import org.teavm.classlib.java.lang.TIllegalStateException;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TString;
import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.spi.GeneratedBy;

/**
 *
 * @author Alexey Andreev
 */
public class TTimer extends TObject {
    TSet<TTimerTask> tasks = new THashSet<>();
    private boolean cancelled;

    public TTimer() {
    }

    public TTimer(@SuppressWarnings("unused") TString name) {
    }

    public void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;
        for (TTimerTask task : tasks.toArray(new TTimerTask[0])) {
            task.cancel();
        }
    }

    public void schedule(TTimerTask task, long delay) {
        if (cancelled || task.timer != null || task.nativeTimerId >= 0) {
            throw new TIllegalStateException();
        }
        task.timer = this;
        task.nativeTimerId = scheduleOnce(task, (int)delay);
    }

    @GeneratedBy(TimerNativeGenerator.class)
    @PluggableDependency(TimerNativeGenerator.class)
    private static native int scheduleOnce(TTimerTask task, int delay);
}
