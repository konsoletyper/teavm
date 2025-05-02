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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.nio.file.FileSystems;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
public class PathTest {
    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void root() {
        var path = FileSystems.getDefault().getPath("/a", "b", "c");
        assertEquals("/", path.getRoot().toString());

        path = FileSystems.getDefault().getPath("a", "b", "c");
        assertNull(path.getRoot());
    }

    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void fileName() {
        var path = FileSystems.getDefault().getPath("/a", "b", "c");
        assertEquals("c", path.getFileName().toString());

        path = FileSystems.getDefault().getPath("/");
        assertNull(path.getFileName());
    }

    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void parent() {
        var path = FileSystems.getDefault().getPath("/a", "b", "c");
        assertEquals("/a/b", path.getParent().toString());

        path = FileSystems.getDefault().getPath("a");
        assertNull(path.getParent());

        path = FileSystems.getDefault().getPath("/");
        assertNull(path.getParent());

        path = FileSystems.getDefault().getPath("/a");
        assertEquals("/", path.getParent().toString());
    }

    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void nameCount() {
        var path = FileSystems.getDefault().getPath("/a", "b", "c");
        assertEquals(3, path.getNameCount());

        path = FileSystems.getDefault().getPath("/a", "b", "c/");
        assertEquals(3, path.getNameCount());

        path = FileSystems.getDefault().getPath("/");
        assertEquals(0, path.getNameCount());

        path = FileSystems.getDefault().getPath("");
        assertEquals(1, path.getNameCount());

        path = FileSystems.getDefault().getPath("a/b");
        assertEquals(2, path.getNameCount());
    }

    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void getName() {
        var path = FileSystems.getDefault().getPath("/a", "b", "c");
        assertEquals("a", path.getName(0).toString());
        assertEquals("b", path.getName(1).toString());
        assertEquals("c", path.getName(2).toString());
        try {
            path.getName(3);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        path = FileSystems.getDefault().getPath("a", "b", "c");
        assertEquals("a", path.getName(0).toString());
        assertEquals("b", path.getName(1).toString());
        assertEquals("c", path.getName(2).toString());
        try {
            path.getName(3);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        path = FileSystems.getDefault().getPath("");
        assertEquals("", path.getName(0).toString());
        try {
            path.getName(1);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        path = FileSystems.getDefault().getPath("/");
        try {
            path.getName(0);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void subpath() {
        var path = FileSystems.getDefault().getPath("/a", "b", "c");
        assertEquals("a/b/c", path.subpath(0, 3).toString());
        assertEquals("a", path.subpath(0, 1).toString());
        assertEquals("b/c", path.subpath(1, 3).toString());
    }

    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void startsWith() {
        var fs = FileSystems.getDefault();
        assertTrue(fs.getPath("/a/b/c").startsWith(fs.getPath("/a/b")));
        assertTrue(fs.getPath("/a/b/c").startsWith(fs.getPath("/a/b/c")));
        assertTrue(fs.getPath("/a/b/c").startsWith(fs.getPath("/")));
        assertFalse(fs.getPath("a/b/c").startsWith(fs.getPath("")));
        assertTrue(fs.getPath("").startsWith(fs.getPath("")));
        assertFalse(fs.getPath("/a/b/c").startsWith(fs.getPath("")));
        assertFalse(fs.getPath("/a/bc").startsWith(fs.getPath("/a/b")));
        assertFalse(fs.getPath("/a/b/c").startsWith(fs.getPath("a/b")));
    }

    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void endsWith() {
        var fs = FileSystems.getDefault();
        assertTrue(fs.getPath("/a/b/c").endsWith(fs.getPath("b/c")));
        assertTrue(fs.getPath("/a/b/c").endsWith(fs.getPath("a/b/c")));
        assertTrue(fs.getPath("/a/b/c").endsWith(fs.getPath("/a/b/c")));
        assertFalse(fs.getPath("/a/b/c").endsWith(fs.getPath("/")));
        assertFalse(fs.getPath("a/b/c").endsWith(fs.getPath("")));
        assertTrue(fs.getPath("").endsWith(fs.getPath("")));
        assertFalse(fs.getPath("ab/c").endsWith(fs.getPath("b/c")));
    }

    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void normalize() {
        var fs = FileSystems.getDefault();
        assertEquals("/a/b/c", fs.getPath("/a/b/c").normalize().toString());
        assertEquals("/a/c", fs.getPath("/a/b/../c").normalize().toString());
        assertEquals("/a/b/c", fs.getPath("/a/b/./c").normalize().toString());
        assertEquals("/c", fs.getPath("/a/b/../../c").normalize().toString());
        assertEquals("/c", fs.getPath("/../../c").normalize().toString());
        assertEquals("../../c", fs.getPath("../../c").normalize().toString());
        assertEquals("/a/d", fs.getPath("/a/b/../c/../d").normalize().toString());
        assertEquals("/a/c/d", fs.getPath("/a/b/../c/x/../d").normalize().toString());
        assertEquals("/a/b", fs.getPath("/a/./b").normalize().toString());
        assertEquals("/", fs.getPath("/.").normalize().toString());
        assertEquals("/a", fs.getPath("/./a").normalize().toString());
        assertEquals("/", fs.getPath("/./.").normalize().toString());
        assertEquals("/", fs.getPath("/./.").normalize().toString());
        assertEquals("/", fs.getPath("/./..").normalize().toString());
        assertEquals("", fs.getPath(".").normalize().toString());
    }

    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void resolve() {
        var fs = FileSystems.getDefault();
        assertEquals("/a/b/c/d", fs.getPath("/a/b/c").resolve(fs.getPath("d")).toString());
        assertEquals("/a/b/c", fs.getPath("/a/b/c").resolve(fs.getPath("")).toString());
        assertEquals("/d", fs.getPath("/a/b/c").resolve(fs.getPath("/d")).toString());
        assertEquals("/a/b/c/..", fs.getPath("/a/b/c").resolve(fs.getPath("..")).toString());
    }

    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void relativize() {
        var fs = FileSystems.getDefault();
        assertEquals("c", fs.getPath("/a/b").relativize(fs.getPath("/a/b/c")).toString());
        assertEquals("c", fs.getPath("a/b").relativize(fs.getPath("a/b/c")).toString());
        assertEquals("", fs.getPath("/a/b/c").relativize(fs.getPath("/a/b/c")).toString());
        assertEquals("../../c/d", fs.getPath("a/b").relativize(fs.getPath("c/d")).toString());

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            fs.getPath("/a/b").relativize(fs.getPath("a/b/c")).toString();
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            fs.getPath("a/b").relativize(fs.getPath("/a/b/c")).toString();
        });
    }

    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void compareTo() {
        var fs = FileSystems.getDefault();
        assertEquals(0, fs.getPath("/a/b/c").compareTo(fs.getPath("/a/b/c")));
        assertTrue(fs.getPath("/a/b/c").compareTo(fs.getPath("a/b/c")) < 0);
        assertTrue(fs.getPath("a/b/c").compareTo(fs.getPath("/a/b/c")) > 0);
    }

    @Test
    @SkipJVM
    @SkipPlatform({ TestPlatform.C, TestPlatform.WASI })
    public void toStringWorks() {
        var path = FileSystems.getDefault().getPath("");
        assertEquals("", path.toString());
    }
}
