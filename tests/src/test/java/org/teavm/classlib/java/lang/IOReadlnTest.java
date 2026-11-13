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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.JsModuleTest;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.ServeJS;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
@EachTestCompiledSeparately
@SkipJVM
@OnlyPlatform({TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
public class IOReadlnTest {
    @BeforeEach
    public void initializeInput() {
        IOReadlnScenarios.initialize();
    }

    @Test
    public void readsLinesAndSplitMultibyteInput() {
        assertEquals("first|한글🙂||tail|null", IOReadlnScenarios.lines());
    }

    @Test
    public void readsBareCarriageReturns() {
        assertEquals("first|한글🙂||tail|null", IOReadlnScenarios.bareCarriageReturns());
    }

    @Test
    public void usesConfiguredCharset() {
        assertEquals("é", IOReadlnScenarios.configuredCharset());
    }

    @Test
    public void usesUtf8WhenCharsetIsMissing() {
        assertEquals("é", IOReadlnScenarios.missingCharset());
    }

    @Test
    public void usesUtf8WhenCharsetIsUnsupported() {
        assertEquals("é", IOReadlnScenarios.unsupportedCharset());
    }

    @Test
    public void usesUtf8WhenCharsetNameIsInvalid() {
        assertEquals("é", IOReadlnScenarios.invalidCharset());
    }

    @Test
    public void replacesMalformedInput() {
        assertEquals("�(�", IOReadlnScenarios.malformedInput());
    }

    @Test
    public void writesPromptBeforeReading() {
        assertEquals("name?answer", IOReadlnScenarios.prompt("name?"));
    }

    @Test
    public void printsNullPrompt() {
        assertEquals("nullanswer", IOReadlnScenarios.prompt(null));
    }

    @Test
    public void preservesInputFailureCause() {
        assertEquals("java.io.IOException:input-failed", IOReadlnScenarios.inputFailure());
    }

    @Test
    public void initializesCharsetOnFirstRead() {
        assertEquals("é", IOReadlnScenarios.lazyCharset());
    }

    @Test
    public void retainsCharsetAfterFirstRead() {
        assertEquals("é|é", IOReadlnScenarios.frozenCharset());
    }

    @Test
    public void replacesStandardInputAndPreservesNull() throws Exception {
        assertEquals("first|second|true", IOReadlnScenarios.replaceInput());
    }

    @Test
    @JsModuleTest
    @ServeJS(from = "org/teavm/classlib/java/lang/io-readln/host-input.js", as = "host-input.js")
    public void readsHostInput() {
        assertEquals("host 한글🙂|null", IOReadlnScenarios.hostInput());
        assertTrue(IOReadlnScenarios.hostReads() > 0, "The JavaScript host callback must supply the input");
    }
}
