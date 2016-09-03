/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.vm;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DirectoryBuildTarget implements BuildTarget {
    private File directory;

    public DirectoryBuildTarget(File directory) {
        this.directory = directory;
    }

    @Override
    public OutputStream createResource(String fileName) throws IOException {
        int index = fileName.lastIndexOf('/');
        if (index >= 0) {
            File dir = new File(directory, fileName.substring(0, index));
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        return new BufferedOutputStream(new FileOutputStream(new File(directory, fileName)), 65536);
    }
}
