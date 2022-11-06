/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.classlib.impl.console;

import java.io.IOException;
import java.io.OutputStream;

public abstract class ConsoleOutputStream extends OutputStream {
    private byte[] buffer = new byte[1];

    @Override
    public void write(int b) throws IOException {
        buffer[0] = (byte) b;
        write(buffer);
    }

    @Override
    public abstract void write(byte[] b, int off, int len) throws IOException;
}
