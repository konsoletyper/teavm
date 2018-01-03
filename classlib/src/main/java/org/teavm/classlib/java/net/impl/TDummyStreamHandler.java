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
package org.teavm.classlib.java.net.impl;

import java.io.IOException;
import org.teavm.classlib.java.net.TURL;
import org.teavm.classlib.java.net.TURLConnection;
import org.teavm.classlib.java.net.TURLStreamHandler;

public class TDummyStreamHandler extends TURLStreamHandler {
    private int defaultPort;

    public TDummyStreamHandler(int defaultPort) {
        this.defaultPort = defaultPort;
    }

    @Override
    protected TURLConnection openConnection(TURL u) throws IOException {
        throw new IOException("Unsupported protocol: " + u.getProtocol());
    }

    @Override
    public int getDefaultPort() {
        return defaultPort;
    }
}
