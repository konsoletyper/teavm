/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.junit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.teavm.model.MethodReference;

public class TestRunner {
    private int numThreads = 1;
    private TestRunStrategy strategy;
    private BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private CountDownLatch latch;
    private volatile boolean stopped;

    public TestRunner(TestRunStrategy strategy) {
        this.strategy = strategy;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public void init() {
        latch = new CountDownLatch(numThreads);
        for (int i = 0; i < numThreads; ++i) {
            new Thread(() -> {
                strategy.beforeThread();
                while (!stopped || !taskQueue.isEmpty()) {
                    Runnable task;
                    try {
                        task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (task != null) {
                        task.run();
                    }
                }
                strategy.afterThread();
                latch.countDown();
            }).start();
        }
    }

    private void addTask(Runnable runnable) {
        taskQueue.add(runnable);
    }

    public void stop() {
        stopped = true;
        taskQueue.add(null);
    }

    public void run(TestRun run) {
        addTask(() -> runImpl(run));
    }

    private void runImpl(TestRun run) {
        MethodReference ref = run.getReference();
        try {
            String result = strategy.runTest(run);
            if (result == null) {
                run.getCallback().complete();
                return;
            }
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode resultObject = (ObjectNode) mapper.readTree(result);
            String status = resultObject.get("status").asText();
            switch (status) {
                case "ok":
                    run.getCallback().complete();
                    break;
                case "exception": {
                    String stack = resultObject.get("stack").asText();
                    String exception = resultObject.get("exception").asText();
                    run.getCallback().error(new AssertionError(exception + "\n" + exception));
                    run.getCallback().complete();
                    break;
                }
            }
        } catch (IOException e) {
            run.getCallback().error(e);
            run.getCallback().complete();
        }
    }
}
