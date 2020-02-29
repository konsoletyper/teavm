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

/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.teavm.classlib.java.util.regex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

/**
 * TODO Type description
 */
@SuppressWarnings("nls")
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class PatternSyntaxExceptionTest {
    @Test
    public void testCase() {
        String regex = "(";
        try {
            Pattern.compile(regex);
            fail("PatternSyntaxException expected");
        } catch (PatternSyntaxException e) {
            // TOFIX: Commented out assertEquals tests...
            // TOFIX: should we match exception strings?
            // assertEquals("Unclosed group", e.getDescription());
            assertEquals(1, e.getIndex());
            // assertEquals("Unclosed group near index 1\n(\n ^",
            // e.getMessage());
            assertEquals(regex, e.getPattern());
        }
    }

    @Test
    public void testCase2() {
        String regex = "[4-";
        try {
            Pattern.compile(regex);
            fail("PatternSyntaxException expected");
        } catch (PatternSyntaxException e) {
            // TOFIX: Commented out assertEquals tests...
            // TOFIX: should we match exception strings?
            // assertEquals("Illegal character range", e.getDescription());
            assertEquals(3, e.getIndex());
            // assertEquals("Illegal character range near index 3\n[4-\n ^",
            // e.getMessage());
            assertEquals(regex, e.getPattern());
        }
    }
}
