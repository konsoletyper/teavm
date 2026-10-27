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
package org.teavm.classlib.java.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

// The C runtime has no entropy source, so it must not hand out a SecureRandom.
@TeaVMTest
@SkipJVM
@OnlyPlatform(TestPlatform.C)
public class SecureRandomCTest {
    // getInstanceStrong: "Throws: NoSuchAlgorithmException - if no algorithm is available"
    @Test
    public void strongInstanceIsUnavailable() {
        assertThrows(NoSuchAlgorithmException.class, SecureRandom::getInstanceStrong);
    }

    @Test
    public void getInstanceIsUnavailable() {
        assertThrows(NoSuchAlgorithmException.class, () -> SecureRandom.getInstance("NativePRNG"));
    }
}
