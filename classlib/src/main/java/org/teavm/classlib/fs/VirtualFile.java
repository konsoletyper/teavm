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

public interface VirtualFile {
    String getName();

    boolean isDirectory();

    boolean isFile();

    String[] listFiles();

    VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append);

    boolean createFile(String fileName) throws IOException;

    boolean createDirectory(String fileName);

    boolean delete();

    boolean adopt(VirtualFile file, String fileName);

    boolean canRead();

    boolean canWrite();

    long lastModified();

    boolean setLastModified(long lastModified);

    boolean setReadOnly(boolean readOnly);

    int length();
}
