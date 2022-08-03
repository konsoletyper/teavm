/*
 *  Copyright 2022 TeaVM Contributors.
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
package org.teavm.runtime;

import static org.teavm.interop.wasi.Memory.free;
import static org.teavm.interop.wasi.Memory.malloc;
import static org.teavm.interop.wasi.Wasi.ERRNO_SUCCESS;
import java.nio.charset.StandardCharsets;
import org.teavm.interop.Address;
import org.teavm.interop.wasi.Wasi;
import org.teavm.interop.wasi.Wasi.ErrnoException;

public final class WasiMain {
    private WasiMain() {
    }

    static void startMain() {
        final int sizesSize = 8;
        final int sizesAlign = 4;
        Address sizes = malloc(sizesSize, sizesAlign);
        short errno = Wasi.argsSizesGet(sizes, sizes.add(4));

        if (errno == ERRNO_SUCCESS) {
            int argvSize = sizes.getInt();
            int argvBufSize = sizes.add(4).getInt();
            free(sizes, sizesSize, sizesAlign);

            Address argv = malloc(argvSize * 4, 4);
            Address argvBuf = malloc(argvBufSize, 1);
            errno = Wasi.argsGet(argv, argvBuf);

            if (errno == ERRNO_SUCCESS) {
                int[] argvOffsets = new int[argvSize];
                byte[] argvBufBytes = new byte[argvBufSize];
                for (int i = 0; i < argvSize; ++i) {
                    argvOffsets[i] = argv.add(i * 4).getInt() - argvBuf.toInt();
                }
                Wasi.getBytes(argvBuf, argvBufBytes, 0, argvBufSize);
                free(argv, argvSize * 4, 4);
                free(argvBuf, argvBufSize, 1);

                String[] args = new String[argvSize - 1];
                for (int i = 1; i < argvSize; ++i) {
                    int offset = argvOffsets[i];
                    int length = (i == argvSize - 1 ? argvBufSize : argvOffsets[i + 1]) - 1 - offset;
                    args[i - 1] = new String(argvBufBytes, offset, length, StandardCharsets.UTF_8);
                }

                Fiber.startMain(args);
            } else {
                free(argv, argvSize * 4, 4);
                free(argvBuf, argvBufSize, 1);
                throw new ErrnoException("args_get", errno);
            }
        } else {
            free(sizes, sizesSize, sizesAlign);
            throw new ErrnoException("args_sizes_get", errno);
        }
    }
}
