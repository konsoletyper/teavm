/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.jso.export;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import org.teavm.jso.JSExport;

public class MainWithInstanceArgs implements IntSupplier {
    private static List<String> log = new ArrayList<>();
    private int value = 40;

    MainWithInstanceArgs() {
        value += 2;
        log.add("constructor");
    }

    @Override
    public int getAsInt() {
        return value;
    }

    void main(String[] args) {
        IntSupplier receiver = this;
        log.add(receiver.getAsInt() + ":" + String.join(",", args));
    }

    @JSExport
    public static String log() {
        return String.join(";", log);
    }
}
