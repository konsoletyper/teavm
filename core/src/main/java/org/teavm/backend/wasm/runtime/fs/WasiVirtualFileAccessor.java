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
package org.teavm.backend.wasm.runtime.fs;

import static org.teavm.backend.wasm.wasi.Wasi.ERRNO_SUCCESS;
import static org.teavm.backend.wasm.wasi.Wasi.WHENCE_CURRENT;
import static org.teavm.backend.wasm.wasi.Wasi.WHENCE_START;
import java.io.IOException;
import org.teavm.backend.wasm.runtime.WasiBuffer;
import org.teavm.backend.wasm.wasi.IOVec;
import org.teavm.backend.wasm.wasi.SizeResult;
import org.teavm.backend.wasm.wasi.Wasi;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;
import org.teavm.runtime.fs.VirtualFileAccessor;

public class WasiVirtualFileAccessor implements VirtualFileAccessor {
    private WasiVirtualFile file;
    private int fd;

    WasiVirtualFileAccessor(WasiVirtualFile file, int fd) {
        this.file = file;
        this.fd = fd;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        Address buf = WasiBuffer.getBuffer();
        IOVec vec = buf.toStructure();
        vec.buffer = Address.ofData(buffer).add(offset);
        vec.bufferLength = length;

        SizeResult sizeResult = Address.align(buf.add(Structure.sizeOf(IOVec.class)), 16).toStructure();
        short errno = Wasi.fdRead(fd, vec, 1, sizeResult);

        if (errno == ERRNO_SUCCESS) {
            return (int) sizeResult.value;
        } else {
            throw new IOException("fd_read: " + errno);
        }
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        Address buf = WasiBuffer.getBuffer();
        IOVec vec = buf.toStructure();
        SizeResult sizeResult = Address.align(buf.add(Structure.sizeOf(IOVec.class)), 16).toStructure();

        while (true) {
            vec.buffer = Address.ofData(buffer).add(offset);
            vec.bufferLength = length;
            short errno = Wasi.fdWrite(fd, vec, 1, sizeResult);

            if (errno == ERRNO_SUCCESS) {
                int size = (int) sizeResult.value;
                offset += size;
                length -= size;
                if (length <= 0) {
                    return;
                }
            } else {
                throw new IOException("fd_write: " + errno);
            }
        }
    }

    @Override
    public int tell() throws IOException {
        SizeResult filesize = WasiBuffer.getBuffer().toStructure();
        short errno = Wasi.fdTell(fd, filesize);

        if (errno == ERRNO_SUCCESS) {
            return (int) filesize.value;
        } else {
            throw new IOException("fd_tell: " + errno);
        }
    }

    private long seek(long offset, byte whence) throws IOException {
        SizeResult filesize = WasiBuffer.getBuffer().toStructure();
        short errno = Wasi.fdSeek(fd, offset, whence, filesize);

        if (errno == ERRNO_SUCCESS) {
            return filesize.value;
        } else {
            throw new IOException("fd_seek: " + errno);
        }
    }

    @Override
    public void skip(int amount) throws IOException {
        seek(amount, WHENCE_CURRENT);
    }

    @Override
    public void seek(int target) throws IOException {
        seek(target, WHENCE_START);
    }

    @Override
    public int size() throws IOException {
        return (int) file.stat().filesize;
    }

    @Override
    public void resize(int size) throws IOException {
        short errno = Wasi.fdFilestatSetSize(fd, size);

        if (errno != ERRNO_SUCCESS) {
            throw new IOException("fd_filestat_set_size" + errno);
        }
    }

    @Override
    public void close() throws IOException {
        if (this.fd >= 0) {
            int fd = this.fd;
            this.fd = -1;

            short errno = Wasi.fdClose(fd);

            if (errno != ERRNO_SUCCESS) {
                throw new IOException("fd_close: " + errno);
            }
        }
    }

    @Override
    public void flush() throws IOException {
        short errno = Wasi.fdSync(fd);

        if (errno != ERRNO_SUCCESS) {
            throw new IOException("fd_sync: " + errno);
        }
    }
}