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

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public final class AsyncProgram {
    private AsyncProgram() {
    }

    public static void main(String[] args) throws InterruptedException {
        withoutAsync();
        System.out.println();
        withAsync();
    }

    private static void withoutAsync() {
        System.out.println("Start sync");
        for (int i = 0; i < 20; ++i) {
            for (int j = 0; j <= i; ++j) {
                System.out.print(j);
                System.out.print(' ');
            }
            System.out.println();
        }
        System.out.println("Complete sync");
    }

    private static void withAsync() throws InterruptedException {
        System.out.println("Start async");
        for (int i = 0; i < 20; ++i) {
            for (int j = 0; j <= i; ++j) {
                System.out.print(j);
                System.out.print(' ');
            }
            System.out.println();
            if (i % 3 == 0) {
                System.out.println("Suspend for a second");
                Thread.sleep(1000);
            }
        }
        System.out.println("Complete async");

        System.out.println("Throwing exception");
        try {
            throwException();
        } catch (IllegalStateException e) {
            System.out.println("Exception caught");
        }
    }

    private static void throwException() {
        Thread.yield();
        System.out.println("Thread.yield called");
        throw new IllegalStateException();
    }
}
