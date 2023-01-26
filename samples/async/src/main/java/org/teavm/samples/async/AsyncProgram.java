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
package org.teavm.samples.async;

import java.util.Arrays;

public final class AsyncProgram {
    private static long start = System.currentTimeMillis();

    private AsyncProgram() {
    }

    public static void main(String[] args) throws InterruptedException {
        report(Arrays.toString(args));
        findPrimes();
        withoutAsync();
        report("");
        withAsync();

        report("");
        final var lock = new Object();
        new Thread(() -> {
            try {
                doRun(lock);
            } catch (InterruptedException ex) {
                report("Exception caught: " + ex.getMessage());
            }
        }, "Test Thread").start();

        new Thread(() -> {
            try {
                doRun(lock);
            } catch (InterruptedException ex) {
                report("Exception caught: " + ex.getMessage());
            }
        }, "Test Thread 2").start();

        report("Should be main");
        report("Now trying wait...");

        synchronized (lock) {
            report("Lock acquired");
            lock.wait(20000);
        }
        report("Finished main thread");
    }

    private static void findPrimes() {
        report("Finding primes");
        var prime = new boolean[1000];
        prime[2] = true;
        prime[3] = true;
        nextPrime: for (var i = 5; i < prime.length; i += 2) {
            var maxPrime = (int) Math.sqrt(i);
            for (var j = 3; j <= maxPrime; j += 2) {
                Thread.yield();
                if (prime[j] && i % j == 0) {
                    continue nextPrime;
                }
            }
            prime[i] = true;
        }
        var sb = new StringBuilder();
        for (var i = 0; i < 1000; ++i) {
            if (prime[i]) {
                sb.append(i).append(' ');
            }
        }
        report(sb.toString());
    }

    private static void report(String message) {
        var current = System.currentTimeMillis() - start;
        System.out.println("[" + Thread.currentThread().getName() + "]/" + current + ": " + message);
    }

    private static void doRun(Object lock) throws InterruptedException {
        report("Executing timer task");
        Thread.sleep(2000);
        report("Calling lock.notify()");
        synchronized (lock) {
            lock.notify();
        }
        report("Finished calling lock.notify()");
        report("Waiting 5 seconds");
        Thread.sleep(5000);
        report("Finished another 5 second sleep");

        synchronized (lock) {
            report("Sleep inside locked section");
            Thread.sleep(2000);
            report("Finished locked section");
        }
    }

    private static void withoutAsync() {
        report("Start sync");
        for (var i = 0; i < 20; ++i) {
            var sb = new StringBuilder();
            for (var j = 0; j <= i; ++j) {
                sb.append(j);
                sb.append(' ');
            }
            report(sb.toString());
        }
        report("Complete sync");
    }

    private static void withAsync() throws InterruptedException {
        report("Start async");
        for (var i = 0; i < 20; ++i) {
            var sb = new StringBuilder();
            for (int j = 0; j <= i; ++j) {
                sb.append(j);
                sb.append(' ');
            }
            report(sb.toString());
            if (i % 3 == 0) {
                report("Suspend for a second");
                Thread.sleep(1000);
            }
        }
        report("2nd Thread.sleep in same method");
        Thread.sleep(1000);

        report("Throwing exception");
        try {
            throwException();
        } catch (IllegalStateException e) {
            report("Exception caught");
        }
        report("Complete async");
    }

    private static void throwException() {
        Thread.yield();
        report("Thread.yield called");
        throw new IllegalStateException();
    }
}
