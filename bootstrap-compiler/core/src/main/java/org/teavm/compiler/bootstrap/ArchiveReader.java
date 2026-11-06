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
package org.teavm.compiler.bootstrap;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class ArchiveReader implements Closeable {
    private final DataInputStream input;

    public ArchiveReader(InputStream input) throws IOException {
        this.input = new DataInputStream(new GZIPInputStream(input));
    }

    @Override
    public void close() throws IOException {
        input.close();
    }

    public String readNext() throws IOException {
        int nameLength;
        try {
            nameLength = input.readShort();
        } catch (EOFException e) {
            return null;
        }
        var bytes = new byte[nameLength];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] readData() throws IOException {
        byte[] data = new byte[input.readInt()];
        input.readFully(data);
        return data;
    }
}
