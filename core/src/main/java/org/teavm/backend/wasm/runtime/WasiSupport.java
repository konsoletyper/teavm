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
package org.teavm.backend.wasm.runtime;

import static org.teavm.backend.wasm.wasi.Wasi.ERRNO_SUCCESS;
import java.nio.charset.StandardCharsets;
import org.teavm.backend.wasm.runtime.math.WasmPow;
import org.teavm.backend.wasm.wasi.IOVec;
import org.teavm.backend.wasm.wasi.IntResult;
import org.teavm.backend.wasm.wasi.LongResult;
import org.teavm.backend.wasm.wasi.SizeResult;
import org.teavm.backend.wasm.wasi.Wasi;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;
import org.teavm.interop.Unmanaged;

public class WasiSupport {
    private static long nextRandom;
    private static boolean randomInitialized;

    private WasiSupport() {
    }

    @Unmanaged
    public static void putCharsStdout(Address address, int count) {
        putChars(1, address, count);
    }

    @Unmanaged
    public static void putCharsStderr(Address address, int count) {
        putChars(2, address, count);
    }

    @Unmanaged
    public static void putChars(int fd, Address address, int count) {
        Address argsAddress = WasiBuffer.getBuffer();
        IOVec ioVec = argsAddress.toStructure();
        SizeResult result = Address.align(argsAddress.add(Structure.sizeOf(IOVec.class)), 16).toStructure();
        ioVec.buffer = address;
        ioVec.bufferLength = count;
        Wasi.fdWrite(fd, ioVec, 1, result);
    }

    @Unmanaged
    public static long currentTimeMillis() {
        LongResult result = WasiBuffer.getBuffer().toStructure();
        Wasi.clockTimeGet(Wasi.CLOCKID_REALTIME, 10, result);
        return result.value / 1000000;
    }

    @Unmanaged
    public static void printString(String s) {
        int charsInChunk = 128;
        int offsetInBuffer = 128;
        Address buffer = WasiBuffer.getBuffer().add(offsetInBuffer);
        for (int i = 0; i < s.length(); i += charsInChunk) {
            int end = Math.min(s.length(), i + charsInChunk);
            int count = end - i;
            for (int j = 0; j < count; ++j) {
                buffer.add(j).putByte((byte) s.charAt(i + j));
            }
            putCharsStderr(buffer, count);
        }
    }

    @Unmanaged
    public static void printInt(int i) {
        int count = 0;
        boolean negative = i < 0;
        i = Math.abs(i);
        Address buffer = WasiBuffer.getBuffer().add(WasiBuffer.getBufferSize());
        do {
            ++count;
            buffer = buffer.add(-1);
            buffer.putByte((byte) ((i % 10) + (int) '0'));
            i /= 10;
        } while (i > 0);
        if (negative) {
            ++count;
            buffer = buffer.add(-1);
            buffer.putByte((byte) '-');
        }
        putCharsStderr(buffer, count);
    }

    @Unmanaged
    public static void printOutOfMemory() {
        printString("Out of memory");
    }

    public static String[] getArgs() {
        Address buffer = WasiBuffer.getBuffer();
        IntResult sizesReceiver = buffer.toStructure();
        IntResult bufferSizeReceiver = Address.align(buffer.add(Structure.sizeOf(IntResult.class)), 16)
                .toStructure();
        short errno = Wasi.argsSizesGet(sizesReceiver, bufferSizeReceiver);

        if (errno != ERRNO_SUCCESS) {
            throw new RuntimeException("Could not get command line arguments");
        }
        int argvSize = sizesReceiver.value;
        int argvBufSize = bufferSizeReceiver.value;

        int[] argvOffsets = new int[argvSize];
        byte[] argvBuffer = new byte[argvBufSize];
        errno = Wasi.argsGet(Address.ofData(argvOffsets), Address.ofData(argvBuffer));

        if (errno != ERRNO_SUCCESS) {
            throw new RuntimeException("Could not get command line arguments");
        }

        String[] args = new String[argvSize - 1];
        for (int i = 1; i < argvSize; ++i) {
            int offset = argvOffsets[i] - Address.ofData(argvBuffer).toInt();
            int length = (i == argvSize - 1 ? argvBufSize - offset : argvOffsets[i + 1] - argvOffsets[i]) - 1;
            args[i - 1] = new String(argvBuffer, offset, length, StandardCharsets.UTF_8);
        }

        return args;
    }

    public static double random() {
        return (((long) nextRandom(26) << 27) + nextRandom(27)) / (double) (1L << 53);
    }

    private static int nextRandom(int bits) {
        if (!randomInitialized) {
            randomInitialized = true;
            short errno = Wasi.randomGet(WasiBuffer.getBuffer(), 8);

            if (errno != ERRNO_SUCCESS) {
                throw new RuntimeException("random_get: " + errno);
            }

            nextRandom = WasiBuffer.getBuffer().getLong();
        }

        nextRandom = ((nextRandom * 0x5DEECE66DL) + 0xBL) & ((1L << 48) - 1);
        return (int) (nextRandom >>> (48 - bits));
    }

    public static double pow(double x, double y) {
        return WasmPow.pow(x, y);
    }
}
