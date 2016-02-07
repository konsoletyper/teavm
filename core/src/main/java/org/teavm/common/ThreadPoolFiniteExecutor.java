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
package org.teavm.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Alexey Andreev
 */
public class ThreadPoolFiniteExecutor implements FiniteExecutor {
    private List<Thread> threads = new ArrayList<>();
    private BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private AtomicInteger runningTasks = new AtomicInteger();
    private final Object monitor = new Object();
    private AtomicReference<RuntimeException> thrownException = new AtomicReference<>();
    private ThreadLocal<Queue<Runnable>> localQueueues = new ThreadLocal<>();

    public ThreadPoolFiniteExecutor(int numThreads) {
        for (int i = 0; i < numThreads; ++i) {
            Thread thread = new Thread(this::takeTask);
            threads.add(thread);
            thread.start();
        }
    }

    @Override
    public void execute(Runnable command) {
        runningTasks.incrementAndGet();
        try {
            queue.put(command);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void executeFast(Runnable runnable) {
        localQueueues.get().add(runnable);
    }

    @Override
    public void complete() {
        synchronized (monitor) {
            while (true) {
                if (thrownException.get() != null) {
                    throw thrownException.get();
                }
                if (runningTasks.get() == 0) {
                    return;
                }
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void takeTask() {
        Queue<Runnable> localQueue = new ArrayDeque<>();
        localQueueues.set(localQueue);
        try {
            while (true) {
                Runnable task = queue.take();
                try {
                    task.run();
                    while (!localQueue.isEmpty()) {
                        localQueue.remove().run();
                    }
                } catch (RuntimeException e) {
                    thrownException.set(e);
                } finally {
                    if (runningTasks.decrementAndGet() == 0 || thrownException.get() != null) {
                        synchronized (monitor) {
                            monitor.notifyAll();
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }
}
