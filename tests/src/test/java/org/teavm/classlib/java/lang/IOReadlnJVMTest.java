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
package org.teavm.classlib.java.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class IOReadlnJVMTest {
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "lines, first|한글🙂||tail|null",
            "bare-cr, first|한글🙂||tail|null",
            "charset, é",
            "missing-charset, é",
            "unsupported-charset, é",
            "invalid-charset, é",
            "malformed, �(�",
            "prompt, name?answer",
            "null-prompt, nullanswer",
            "io-error, java.io.IOException:input-failed",
            "lazy-charset, é",
            "frozen-charset, é|é",
            "set-in, first|second|true",
            "host-input, host 한글🙂|null"
    })
    public void comparesWithFreshJdk(String mode, String expected) throws Exception {
        // IO caches its input reader. The JVM side of @TeaVMTest runs in the shared
        // test JVM, so reference each initial-reader scenario in a fresh process.
        var classes = Path.of(IOReadlnScenarios.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        var executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
                "-Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir"), "-cp", classes.toString(),
                IOReadlnScenarios.class.getName(), mode).redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "JDK launcher timed out for " + mode);
            var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n");
            assertEquals(0, process.exitValue(), output);
            assertEquals(expected + "\n", output, mode);
        } finally {
            process.destroyForcibly();
        }
    }
}
