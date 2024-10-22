/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.classlib.java.util.zip;

import static org.teavm.classlib.java.util.zip.ZipTestUtil.writeHex;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ZipFileTest {
    private static final String DATA1 = "504b030414000808080036a25659000000000000000000000000070000006578616d706c65e"
            + "dc1310d000000c3a0d4bfe9d9d801140000f06e504b07086a6536d214000000b80b0000504b0102140014000808080036a256"
            + "596a6536d214000000b80b00000700000000000000000000000000000000006578616d706c65504b050600000000010001003"
            + "5000000490000000000";

    @BeforeClass
    public void initAll() {

    }

    @Test
    public void readEntry() throws IOException {
        var file = new File("test.zip");

        try (var input = new FileInputStream(file)) {
            var bytes = input.readAllBytes();
            System.out.println(writeHex(bytes));
        }
    }
}
