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
package org.teavm.backend.wasm.wasi;

import org.teavm.interop.Import;

public final class Wasi {
    public static final int CLOCKID_REALTIME = 0;

    private Wasi() {
    }

    @Import(name = "fd_write", module = "wasi_snapshot_preview1")
    public static native short fdWrite(int fd, IOVec vectors, int vectorsCont, SizeResult result);

    @Import(name = "clock_time_get", module = "wasi_snapshot_preview1")
    public static native short clockTimeGet(int clockId, long precision, LongResult result);
}
