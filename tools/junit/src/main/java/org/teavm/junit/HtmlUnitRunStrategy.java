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

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.NativeJavaObject;
import org.apache.commons.io.IOUtils;

public class HtmlUnitRunStrategy implements TestRunStrategy {
    @Override
    public void beforeThread() {
    }

    @Override
    public void afterThread() {
    }

    @Override
    public String runTest(TestRun run) throws IOException {
        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            HtmlPage page = webClient.getPage("about:blank");
            page.executeJavaScript(readFile(new File(run.getBaseDirectory(), "runtime.js")));

            AsyncResult asyncResult = new AsyncResult();
            Function function = (Function) page.executeJavaScript(readResource("teavm-htmlunit-adapter.js"))
                    .getJavaScriptResult();
            Object[] args = new Object[] { new NativeJavaObject(function, asyncResult, AsyncResult.class) };
            page.executeJavaScriptFunctionIfPossible(function, function, args, page);

            page.executeJavaScript(readFile(new File(run.getBaseDirectory(), "test.js")));
            page.cleanUp();
            for (WebWindow window : webClient.getWebWindows()) {
                window.getJobManager().removeAllJobs();
            }
            return (String) asyncResult.getResult();
        }
    }

    private String readFile(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return IOUtils.toString(input, "UTF-8");
        }
    }

    private String readResource(String resourceName) throws IOException {
        try (InputStream input = HtmlUnitRunStrategy.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                return "";
            }
            return IOUtils.toString(input, "UTF-8");
        }
    }

    public class AsyncResult {
        private CountDownLatch latch = new CountDownLatch(1);
        private Object result;

        public void complete(Object result) {
            this.result = result;
            latch.countDown();
        }

        public Object getResult() {
            try {
                latch.await(5, TimeUnit.SECONDS);
                return result;
            } catch (InterruptedException e) {
                return null;
            }
        }
    }
}
