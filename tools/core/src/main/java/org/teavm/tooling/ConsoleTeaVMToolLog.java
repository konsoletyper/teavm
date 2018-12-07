/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.tooling;

public class ConsoleTeaVMToolLog implements TeaVMToolLog {
    private boolean debug;

    public ConsoleTeaVMToolLog(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void info(String text) {
        System.out.println("INFO: " + text);
    }

    @Override
    public void debug(String text) {
        if (debug) {
            System.out.println("DEBUG: " + text);
        }
    }

    @Override
    public void warning(String text) {
        System.out.println("WARNING: " + text);
    }

    @Override
    public void error(String text) {
        System.out.println("ERROR: " + text);
    }

    @Override
    public void info(String text, Throwable e) {
        System.out.println("INFO: " + text);
        e.printStackTrace(System.out);
    }

    @Override
    public void debug(String text, Throwable e) {
        if (debug) {
            System.out.println("DEBUG: " + text);
            e.printStackTrace(System.out);
        }
    }

    @Override
    public void warning(String text, Throwable e) {
        System.out.println("WARNING: " + text);
        e.printStackTrace(System.out);
    }

    @Override
    public void error(String text, Throwable e) {
        System.out.println("ERROR: " + text);
        e.printStackTrace(System.out);
    }
}
