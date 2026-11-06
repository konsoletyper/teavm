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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public final class PrintOnlyContract {
    private PrintOnlyContract() {
    }

    public static void main(String[] args) {
        var output = new ByteArrayOutputStream();
        new RuntimeException("print-only").printStackTrace(new PrintStream(output));
        check(output.toString().contains("<JS>."), "printStackTrace includes captured native frames");
        System.out.println("Throwable print-only contract passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
