/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.chromerdp;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class WasmChromeRDPRunner {
    private ChromeRDPServer server;
    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    private WasmChromeRDPRunner() {
        server = new ChromeRDPServer();
        server.setPort(2357);
        var wasmDebugger = new WasmChromeRDPDebugger(queue::offer);
        server.setExchangeConsumer(wasmDebugger);

        new Thread(server::start).start();

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Uncaught exception in thread " + t);
            e.printStackTrace();
        });
    }

    public static void main(String[] args) {
        var runner = new WasmChromeRDPRunner();
        try {
            while (true) {
                runner.run();
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
    }

    public void run() throws InterruptedException {
        while (true) {
            queue.take().run();
        }
    }
}
