/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.devserver.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DefaultDevServerEventQueue implements DevServerEventQueue {
    private BlockingQueue<Runnable> eventQueue = new LinkedBlockingQueue<>();
    private boolean eventQueueDone;

    @Override
    public void schedule(Runnable runnable) {
        eventQueue.offer(runnable);
    }

    public void stopEventQueue() {
        schedule(() -> eventQueueDone = true);
    }

    public void runEventQueue() {
        eventQueueDone = false;

        while (!eventQueueDone) {
            Runnable command;
            try {
                command = eventQueue.take();
            } catch (InterruptedException e) {
                break;
            }
            command.run();
        }
    }
}
