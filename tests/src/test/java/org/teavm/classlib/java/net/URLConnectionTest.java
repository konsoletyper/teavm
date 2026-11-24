/*
 *  Copyright 2026 ryo kishida.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class URLConnectionTest {
    @Test
    public void contentLengthLong() throws MalformedURLException {
        assertEquals(42L, connection("42").getContentLengthLong());
        assertEquals(5_000_000_000L, connection("5000000000").getContentLengthLong());
        assertEquals(-1L, connection(null).getContentLengthLong());
        assertEquals(-1L, connection("abc").getContentLengthLong());
    }

    private static URLConnection connection(String contentLength) throws MalformedURLException {
        return new URLConnection(new URL("http://example.com/")) {
            @Override
            public void connect() throws IOException {
            }

            @Override
            public String getHeaderField(String name) {
                return "content-length".equalsIgnoreCase(name) ? contentLength : null;
            }
        };
    }
}
