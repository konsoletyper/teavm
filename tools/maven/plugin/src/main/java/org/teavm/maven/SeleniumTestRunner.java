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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.teavm.model.MethodReference;
import org.teavm.tooling.testing.TestCase;
import org.teavm.tooling.testing.TestGroup;
import org.teavm.tooling.testing.TestPlan;

/**
 *
 * @author Alexey Andreev
 */
public class SeleniumTestRunner {
    private URL url;
    private int numThreads = 1;
    private ThreadLocal<WebDriver> webDriver = new ThreadLocal<>();
    private BlockingQueue<Runnable> seleniumTaskQueue = new LinkedBlockingQueue<>();
    private CountDownLatch latch;
    private volatile boolean seleniumStopped = false;
    private Log log;
    private List<TestResult> report = new CopyOnWriteArrayList<>();
    private ThreadLocal<List<TestResult>> localReport = new ThreadLocal<>();
    private File directory = new File(".");

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public File getDirectory() {
        return directory;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public void run(TestPlan testPlan) {
        initSelenium();
        for (TestGroup group : testPlan.getGroups()) {
            for (TestCase testCase : group.getTestCases()) {
                run(testPlan.getRuntimeScript(), testCase);
            }
        }
        stopSelenium();
        waitForCompletion();
    }

    private void initSelenium() {
        latch = new CountDownLatch(numThreads);
        for (int i = 0; i < numThreads; ++i) {
            new Thread(() -> {
                ChromeDriver driver = new ChromeDriver();
                webDriver.set(driver);
                localReport.set(new ArrayList<>());
                while (!seleniumStopped || !seleniumTaskQueue.isEmpty()) {
                    Runnable task;
                    try {
                        task = seleniumTaskQueue.poll(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (task != null) {
                        task.run();
                    }
                }
                report.addAll(localReport.get());
                localReport.remove();
                webDriver.get().close();
                webDriver.remove();
                latch.countDown();
            }).start();
        }
    }

    private void addSeleniumTask(Runnable runnable) {
        seleniumTaskQueue.add(runnable);
    }

    private void stopSelenium() {
        seleniumStopped = true;
    }

    private void waitForCompletion() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            return;
        }
    }

    private void run(String runtimeScript, TestCase testCase) {
        addSeleniumTask(() -> runImpl(runtimeScript, testCase));
    }

    private void runImpl(String runtimeScript, TestCase testCase) {
        webDriver.get().manage().timeouts().setScriptTimeout(2, TimeUnit.SECONDS);
        JavascriptExecutor js = (JavascriptExecutor) webDriver.get();
        try {
            String result = (String) js.executeAsyncScript(
                    readResource("teavm-selenium.js"),
                    readFile(new File(directory, runtimeScript)),
                    readFile(new File(directory, testCase.getTestScript())),
                    readResource("teavm-selenium-adapter.js"));
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode resultObject = (ObjectNode) mapper.readTree(result);
            String status = resultObject.get("status").asText();
            MethodReference ref = MethodReference.parse(testCase.getTestMethod());
            switch (status) {
                case "ok":
                    log.info("Test passed: " + testCase.getTestMethod());
                    localReport.get().add(TestResult.passed(ref));
                    break;
                case "exception": {
                    String stack = resultObject.get("stack").asText();
                    log.info("Test failed: " + testCase.getTestMethod());
                    localReport.get().add(TestResult.error(ref, stack));
                    break;
                }
            }
        } catch (IOException e) {
            log.error(e);
        } catch (WebDriverException e) {
            log.error("Error occured running test " + testCase.getTestMethod(), e);
            @SuppressWarnings("unchecked")
            List<Object> errors = (List<Object>) js.executeScript("return window.jsErrors;");
            for (Object error : errors) {
                log.error("  -- additional error: " + error);
            }
        }
    }

    private String readFile(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return IOUtils.toString(input, "UTF-8");
        }
    }

    private String readResource(String resourceName) throws IOException {
        try (InputStream input = BuildJavascriptTestMojo.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                return "";
            }
            return IOUtils.toString(input, "UTF-8");
        }
    }

    public List<TestResult> getReport() {
        return new ArrayList<>(report);
    }
}
