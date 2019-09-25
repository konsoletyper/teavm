/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.classlib.fs;

import java.io.IOException;

public interface VirtualFileAccessor {
    int read(byte[] buffer, int offset, int limit) throws IOException;

    void write(byte[] buffer, int offset, int limit) throws IOException;

    int tell() throws IOException;

    void seek(int target) throws IOException;

    void skip(int amount) throws IOException;

    int size() throws IOException;

    void resize(int size) throws IOException;

    void close() throws IOException;

    void flush() throws IOException;
}
