/*
 *  Copyright 2026 TeaVM contributors.
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
package repro;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public final class RuntimeApiContract {
    private static volatile boolean release;
    private static volatile boolean entered;
    private static volatile Throwable workerFailure;

    private RuntimeApiContract() {
    }

    public static void main(String[] args) throws Exception {
        resources();
        threads();
        System.out.println("Runtime API contract passed");
    }

    private static void resources() throws Exception {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        List<URL> urls = Collections.list(loader.getResources("fixtures/shared.txt"));
        check(urls.size() == 2, "both classpath resource occurrences");
        check("first".equals(read(urls.get(0))), "first classpath resource");
        check("second".equals(read(urls.get(1))), "second classpath resource");
        check(!urls.get(0).toExternalForm().equals(urls.get(1).toExternalForm()), "distinct resource URLs");
        check("first".equals(read(loader.getResource("fixtures/shared.txt"))), "getResource first");
        try (InputStream first = urls.get(0).openStream(); InputStream again = urls.get(0).openStream()) {
            check(first.read() == 'f' && first.read() == 'i' && again.read() == 'f', "independent streams");
        }
        URL encoded = loader.getResource("fixtures/odd name #.txt");
        check(encoded != null && encoded.getRef() == null, "resource hash escaped");
        check("encoded".equals(read(encoded)), "escaped resource contents");
        check(loader.getResource("fixtures/missing.txt") == null, "missing resource");
        check(!loader.getResources("fixtures/missing.txt").hasMoreElements(), "missing enumeration");
        check(loader.getResourceAsStream("fixtures/missing.txt") == null, "missing stream");
        ClassLoader child = new ClassLoader(loader) { };
        check(Collections.list(child.getResources("fixtures/shared.txt")).size() == 2, "parent delegation");
        ClassLoader defaultChild = new ClassLoader() { };
        check(defaultChild.getParent() == loader, "default parent");
        ClassLoader isolated = new ClassLoader(null) { };
        check(isolated.getResource("fixtures/shared.txt") == null, "no app resources without parent");
        check(Collections.list(ClassLoader.getSystemResources("fixtures/shared.txt")).size() == 2,
                "system resource enumeration");
        check(loader.loadClass("java.lang.String") == String.class, "load known class");
        try {
            loader.loadClass("repro.DoesNotExist");
            throw new AssertionError("missing class accepted");
        } catch (ClassNotFoundException expected) {
            // Expected closed-world lookup failure.
        }
        try {
            loader.getResources(null);
            throw new AssertionError("null resource accepted");
        } catch (NullPointerException expected) {
            // Required null contract.
        }
    }

    private static String read(URL url) throws Exception {
        try (InputStream input = url.openStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private static void threads() throws Exception {
        Thread main = Thread.currentThread();
        ClassLoader original = main.getContextClassLoader();
        ClassLoader custom = new ClassLoader(original) { };
        main.setContextClassLoader(custom);
        Thread worker;
        try {
            worker = new Thread(() -> {
                try {
                    check(Thread.currentThread().getContextClassLoader() == custom, "inherited context loader");
                    entered = true;
                    while (!release) {
                        Thread.sleep(1);
                    }
                } catch (Throwable failure) {
                    workerFailure = failure;
                }
            });
            main.setContextClassLoader(null);
            Thread nullContext = new Thread(() -> { });
            check(nullContext.getContextClassLoader() == null, "explicit null loader inherited");
        } finally {
            main.setContextClassLoader(original);
        }
        check(!worker.isAlive() && !enumerated(worker), "unstarted thread not alive");
        worker.start();
        long deadline = System.currentTimeMillis() + 2000;
        while (!entered && workerFailure == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(1);
        }
        check(entered && workerFailure == null, "worker entered");
        check(worker.isAlive() && enumerated(worker) && enumerated(main), "live threads enumerated");
        rejectRestart(worker);
        release = true;
        worker.join(2000);
        check(!worker.isAlive() && !enumerated(worker), "terminated thread not alive");
        check(workerFailure == null, "worker completed normally");
        rejectRestart(worker);
        check(Thread.enumerate(new Thread[0]) == 0, "empty enumeration target");
        try {
            Thread.enumerate(null);
            throw new AssertionError("null thread array accepted");
        } catch (NullPointerException expected) {
            // Required null contract.
        }
    }

    private static boolean enumerated(Thread expected) {
        Thread[] threads = new Thread[Thread.activeCount() + 8];
        int count = Thread.enumerate(threads);
        for (int i = 0; i < count; ++i) {
            if (threads[i] == expected) {
                return true;
            }
        }
        return false;
    }

    private static void rejectRestart(Thread thread) {
        try {
            thread.start();
            throw new AssertionError("thread restarted");
        } catch (IllegalThreadStateException expected) {
            // A Thread instance can start only once.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

