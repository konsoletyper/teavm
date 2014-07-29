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
package org.teavm.chromerpd;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Alexey Andreev
 */
public class Deferred {
    private volatile Object calculatedValue;
    private AtomicBoolean calculated = new AtomicBoolean();
    private CountDownLatch latch = new CountDownLatch(1);

    public Object get() {
        while (latch.getCount() > 0) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while awaiting for value");
            }
        }
        return calculatedValue;
    }

    public void set(Object value) {
        if (!calculated.compareAndSet(false, true)) {
            throw new IllegalStateException("Future already calculated");
        }
        calculatedValue = value;
        latch.countDown();
    }
}
