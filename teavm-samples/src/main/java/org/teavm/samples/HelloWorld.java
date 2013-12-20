/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.samples;

/**
 *
 * @author Alexey Andreev
 */
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, world!");
        System.out.println("Here is the Fibonacci sequence:");
        long a = 0;
        long b = 1;
        for (int i = 0; i < 70; ++i) {
            System.out.println(a);
            long c = a + b;
            a = b;
            b = c;
        }
        System.out.println("And so on...");
    }
}
