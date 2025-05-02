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
package org.teavm.classlib.java.nio.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.nio.file.FileSystems;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
public class FileSystemTest {
    @Test(expected = UnsupportedOperationException.class)
    public void closeDefault() throws IOException {
        FileSystems.getDefault().close();
    }

    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void roots() {
        var roots = FileSystems.getDefault().getRootDirectories().iterator();
        assertTrue(roots.hasNext());
        var root = roots.next();
        assertEquals("/", root.toString());
        assertFalse(roots.hasNext());
    }
    
    @Test
    @SkipJVM
    @SkipPlatform(TestPlatform.C)
    public void separator() {
        var separator = FileSystems.getDefault().getSeparator();
        assertEquals("/", separator);
    }

    @Test
    @SkipJVM
    @SkipPlatform(TestPlatform.C)
    public void getPath() {
        var path = FileSystems.getDefault().getPath("a", "b", "c");
        assertEquals("a/b/c", path.toString());
        assertFalse(path.isAbsolute());

        path = FileSystems.getDefault().getPath("/a", "b", "c");
        assertEquals("/a/b/c", path.toString());
        assertTrue(path.isAbsolute());

        path = FileSystems.getDefault().getPath("/");
        assertEquals("/", path.toString());

        path = FileSystems.getDefault().getPath("", "/");
        assertEquals("/", path.toString());

        path = FileSystems.getDefault().getPath("");
        assertEquals("", path.toString());

        path = FileSystems.getDefault().getPath("/a", "/b//c");
        assertEquals("/a/b/c", path.toString());

        path = FileSystems.getDefault().getPath("a", "", "b");
        assertEquals("a/b", path.toString());

        path = FileSystems.getDefault().getPath("a", "/", "b");
        assertEquals("a/b", path.toString());

        path = FileSystems.getDefault().getPath("a", "/", "b/");
        assertEquals("a/b", path.toString());

        path = FileSystems.getDefault().getPath("", "/a", "");
        assertEquals("/a", path.toString());

        path = FileSystems.getDefault().getPath("/a", "/", "/");
        assertEquals("/a", path.toString());
    }
}
