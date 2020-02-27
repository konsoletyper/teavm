/*
 *  Copyright 2019 Alexey Andreev.
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

package org.teavm.classlib.java.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class URLEncoderTest {

    @Test
    @SuppressWarnings("deprecation")
    public void test_encodeLjava_lang_String() {
        final String url = "http://localhost.";
        final String url2 = "telnet://justWantToHaveFun.com:400";
        final String url3 = "file://myServer.org/a file with spaces.jpg";

        assertEquals("1. Incorrect encoding/decoding", url, URLDecoder.decode(URLEncoder.encode(url)));
        assertEquals("2. Incorrect encoding/decoding", url2, URLDecoder.decode(URLEncoder.encode(url2)));
        assertEquals("3. Incorrect encoding/decoding", url3, URLDecoder.decode(URLEncoder.encode(url3)));
    }

    @Test
    public void test_encodeLjava_lang_StringLjava_lang_String()
            throws Exception {
        // Regression for HARMONY-24
        try {
            URLEncoder.encode("str", "unknown_enc");
            fail("Assert 0: Should throw UEE for invalid encoding");
        } catch (UnsupportedEncodingException e) {
            // expected
        }
    }
}
