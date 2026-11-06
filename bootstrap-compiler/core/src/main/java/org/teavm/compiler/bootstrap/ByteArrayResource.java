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
package org.teavm.compiler.bootstrap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import org.teavm.parsing.resource.Resource;

public class ByteArrayResource implements Resource {
    private byte[] data;

    public ByteArrayResource(byte[] data) {
        this.data = data;
    }

    @Override
    public InputStream open() {
        return new ByteArrayInputStream(data);
    }

    @Override
    public Date getModificationDate() {
        return null;
    }
}
