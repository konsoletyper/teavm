/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.maven.plugin.logging.Log;
import org.teavm.model.MethodReference;
import org.teavm.tooling.testing.TestCase;
import org.teavm.tooling.testing.TestGroup;
import org.teavm.tooling.testing.TestPlan;

/**
 *
 * @author Alexey Andreev
 */
public class TestRunner {
    private int numThreads = 1;
    private TestRunStrategy strategy;
    private BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private CountDownLatch latch;
    private volatile boolean stopped;
    private Log log;
    private List<TestResult> report = new CopyOnWriteArrayList<>();
    private ThreadLocal<List<TestResult>> localReport = new ThreadLocal<>();

    public TestRunner(TestRunStrategy strategy) {
        this.strategy = strategy;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public void run(TestPlan testPlan) {
        init();
        for (TestGroup group : testPlan.getGroups()) {
            for (TestCase testCase : group.getTestCases()) {
                run(testPlan.getRuntimeScript(), testCase);
            }
        }
        stop();
        waitForCompletion();
    }

    private void init() {
        latch = new CountDownLatch(numThreads);
        for (int i = 0; i < numThreads; ++i) {
            new Thread(() -> {
                strategy.beforeThread();
                localReport.set(new ArrayList<>());
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
                report.addAll(localReport.get());
                localReport.remove();
                strategy.afterThread();
                latch.countDown();
            }).start();
        }
    }

    private void addTask(Runnable runnable) {
        taskQueue.add(runnable);
    }

    private void stop() {
        stopped = true;
    }

    private void waitForCompletion() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            return;
        }
    }

    private void run(String runtimeScript, TestCase testCase) {
        addTask(() -> runImpl(runtimeScript, testCase));
    }

    private void runImpl(String runtimeScript, TestCase testCase) {
        MethodReference ref = MethodReference.parse(testCase.getTestMethod());
        try {
            String result = strategy.runTest(log, runtimeScript, testCase);
            if (result == null) {
                log.info("Test failed: " + testCase.getTestMethod());
                localReport.get().add(TestResult.error(ref, null, null));
                return;
            }
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode resultObject = (ObjectNode) mapper.readTree(result);
            String status = resultObject.get("status").asText();
            switch (status) {
                case "ok":
                    if (testCase.getExpectedExceptions().isEmpty()) {
                        log.info("Test passed: " + testCase.getTestMethod());
                        localReport.get().add(TestResult.passed(ref));
                    } else {
                        log.info("Test failed: " + testCase.getTestMethod());
                        localReport.get().add(TestResult.exceptionNotThrown(ref));
                    }
                    break;
                case "exception": {
                    String stack = resultObject.get("stack").asText();
                    String exception = resultObject.get("exception").asText();
                    if (!testCase.getExpectedExceptions().contains(exception)) {
                        log.info("Test failed: " + testCase.getTestMethod());
                        localReport.get().add(TestResult.error(ref, exception, stack));
                    } else {
                        log.info("Test passed: " + testCase.getTestMethod());
                        localReport.get().add(TestResult.passed(ref));
                    }
                    break;
                }
            }
        } catch (IOException e) {
            log.error(e);
        }
    }

    public TestReport getReport() {
        TestReport report = new TestReport();
        report.getResults().addAll(this.report);
        return report;
    }
}
