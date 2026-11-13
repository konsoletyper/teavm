/*
 *  Copyright 2025 ihromant.
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
package org.teavm.classlib.java.lang;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class TIO {
    private static BufferedReader input;

    private TIO() {
        throw new Error();
    }

    public static void println(Object obj) {
        System.out.println(obj);
    }

    public static void println() {
        System.out.println();
    }

    public static void print(Object obj) {
        System.out.print(obj);
    }

    public static String readln() {
        if (input == null) {
            Charset charset = StandardCharsets.UTF_8;
            String encoding = System.getProperty("stdin.encoding");
            if (encoding != null) {
                try {
                    charset = Charset.forName(encoding);
                } catch (IllegalArgumentException e) {
                    // An unavailable charset falls back to UTF-8.
                }
            }
            input = new BufferedReader(new InputStreamReader(System.in, charset));
        }
        try {
            return input.readLine();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static String readln(String prompt) {
        print(prompt);
        return readln();
    }
}
