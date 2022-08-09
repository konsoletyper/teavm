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

import static org.teavm.interop.wasi.Wasi.ERRNO_SUCCESS;
import java.nio.charset.StandardCharsets;
import org.teavm.interop.Address;
import org.teavm.interop.wasi.Wasi;
import org.teavm.interop.wasi.Wasi.ErrnoException;

public final class WasiMain {
    private static final byte[] TWELVE_BYTE_BUFFER = new byte[12];

    private WasiMain() {
    }

    static void startMain() {
        byte[] sizesBuffer = TWELVE_BYTE_BUFFER;
        Address sizes = Address.align(Address.ofData(sizesBuffer), 4);
        short errno = Wasi.argsSizesGet(sizes, sizes.add(4));

        if (errno == ERRNO_SUCCESS) {
            int argvSize = sizes.getInt();
            int argvBufSize = sizes.add(4).getInt();

            byte[] argvBuffer = new byte[(argvSize * 4) + 4];
            Address argv = Address.align(Address.ofData(argvBuffer), 4);
            byte[] argvBuf = new byte[argvBufSize];
            errno = Wasi.argsGet(argv, Address.ofData(argvBuf));

            if (errno == ERRNO_SUCCESS) {
                String[] args = new String[argvSize - 1];
                for (int i = 1; i < argvSize; ++i) {
                    int offset = argv.add(i * 4).getInt() - Address.ofData(argvBuf).toInt();
                    int length = (i == argvSize - 1
                                  ? argvBufSize
                                  : (argv.add((i + 1) * 4).getInt() - Address.ofData(argvBuf).toInt()))
                        - 1 - offset;

                    // TODO: this is probably not guaranteed to be UTF-8:
                    args[i - 1] = new String(argvBuf, offset, length, StandardCharsets.UTF_8);
                }

                Fiber.startMain(args);
            } else {
                throw new ErrnoException("args_get", errno);
            }
        } else {
            throw new ErrnoException("args_sizes_get", errno);
        }
    }
}
