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

package org.teavm.compiler.bootstrap.tools;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

public class ArchiveBuilder implements Closeable {
    private final DataOutputStream output;

    public ArchiveBuilder(OutputStream output) throws IOException {
        this.output = new DataOutputStream(new GZIPOutputStream(output));
    }

    @Override
    public void close() throws IOException {
        output.close();
    }

    public void append(String entryName, byte[] data) throws IOException {
        var name = entryName.getBytes(StandardCharsets.UTF_8);
        output.writeShort(name.length);
        output.write(name);
        output.writeInt(data.length);
        output.write(data);
    }

    public static void main(String[] args) throws IOException {
        var root = Path.of(args[0]);
        var outPath = Path.of(args[1]);
        Files.createDirectories(outPath.getParent());
        try (var input = Files.walk(root);
                var output = new ArchiveBuilder(Files.newOutputStream(outPath))) {
            input.forEach(file -> {
                if (Files.isRegularFile(file)) {
                    var relName = root.relativize(file).toString().replace('\\', '/');
                    try {
                        output.append(relName, Files.readAllBytes(file));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        }
    }
}
