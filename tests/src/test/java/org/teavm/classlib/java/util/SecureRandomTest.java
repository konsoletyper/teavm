/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
@OnlyPlatform({TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
public class SecureRandomTest {
    @Test
    public void nextInt() {
        var array = new int[10];
        var random = new SecureRandom();
        for (var i = 0; i < 1000; ++i) {
            var n = Integer.remainderUnsigned(random.nextInt(), array.length);
            array[n]++;
        }
        for (var elem : array) {
            assertTrue(elem > 0, "Each number must be generated at least once");
        }
    }

    // getInstanceStrong: "Returns a SecureRandom object that was selected by using the algorithms/providers
    // specified in the securerandom.strongAlgorithms Security property." On Linux and macOS the JDK's
    // default value of that property starts with NativePRNGBlocking, the name TeaVM also uses.
    @Test
    public void strongInstanceReportsItsAlgorithm() throws NoSuchAlgorithmException {
        var random = SecureRandom.getInstanceStrong();
        assertEquals(SecureRandom.class, random.getClass());
        assertEquals("NativePRNGBlocking", random.getAlgorithm());
    }

    @Test
    public void strongInstanceGivesRandomBytes() throws NoSuchAlgorithmException {
        var first = SecureRandom.getInstanceStrong();
        var second = SecureRandom.getInstanceStrong();
        var seen = new HashSet<String>();
        for (var i = 0; i < 20; ++i) {
            for (var random : List.of(first, second)) {
                var bytes = new byte[32];
                random.nextBytes(bytes);
                assertFalse(Arrays.equals(new byte[32], bytes), "All bytes are zero");
                assertTrue(seen.add(Arrays.toString(bytes)), "Repeated output");
            }
        }
    }

    // getAlgorithm: "Returns the name of the algorithm implemented by this SecureRandom object."
    @Test
    public void getInstanceReportsRequestedAlgorithm() throws NoSuchAlgorithmException {
        for (var algorithm : List.of("NativePRNG", "NativePRNGBlocking", "NativePRNGNonBlocking")) {
            assertEquals(algorithm, SecureRandom.getInstance(algorithm).getAlgorithm());
        }
    }
}
