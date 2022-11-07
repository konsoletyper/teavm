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

import java.io.IOException;
import org.teavm.backend.wasm.runtime.WasiBuffer;
import org.teavm.interop.Address;
import org.teavm.runtime.fs.VirtualFileAccessor;

public class WasiVirtualFileAccessor implements VirtualFileAccessor {
    // Enough room for an I32 plus padding for alignment:
    private static final byte[] EIGHT_BYTE_BUFFER = new byte[8];
    // Enough room for an I64 plus padding for alignment:
    private static final byte[] SIXTEEN_BYTE_BUFFER = new byte[16];

    private WasiFileSystem fs;
    private int fd;

    public WasiVirtualFileAccessor(WasiFileSystem fs, int fd) {
        this.fs = fs;
        this.fd = fd;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        byte[] vecBuffer = SIXTEEN_BYTE_BUFFER;
        Address vec = WasiBuffer.getBuffer();
        vec.putInt(Address.ofData(buffer).add(offset).toInt());
        vec.add(4).putInt(length);
        byte[] sizeBuffer = EIGHT_BYTE_BUFFER;
        Address size = Address.align(Address.ofData(sizeBuffer), 4);
        short errno = Wasi.fdRead(fd, vec, 1, size);

        if (errno == ERRNO_SUCCESS) {
            return size.getInt();
        } else {
            throw new IOException(errnoMessage("fd_read", errno));
        }
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        byte[] vecBuffer = SIXTEEN_BYTE_BUFFER;
        Address vec = Address.align(Address.ofData(vecBuffer), 4);
        byte[] sizeBuffer = EIGHT_BYTE_BUFFER;
        Address size = Address.align(Address.ofData(sizeBuffer), 4);

        int index = 0;
        while (true) {
            vec.putInt(Address.ofData(buffer).add(offset + index).toInt());
            vec.add(4).putInt(length - index);
            short errno = Wasi.fdWrite(fd, vec, 1, size);

            if (errno == ERRNO_SUCCESS) {
                index += size.getInt();
                if (index >= length) {
                    return;
                }
            } else {
                throw new IOException(errnoMessage("fd_write", errno));
            }
        }
    }

    @Override
    public int tell() throws IOException {
        byte[] filesizeBuffer = SIXTEEN_BYTE_BUFFER;
        Address filesize = Address.align(Address.ofData(filesizeBuffer), 8);
        short errno = Wasi.fdTell(fd, filesize);

        if (errno == ERRNO_SUCCESS) {
            return (int) filesize.getLong();
        } else {
            throw new IOException(errnoMessage("fd_tell", errno));
        }
    }

    private long seek(long offset, byte whence) throws IOException {
        byte[] filesizeBuffer = SIXTEEN_BYTE_BUFFER;
        Address filesize = Address.align(Address.ofData(filesizeBuffer), 8);
        short errno = Wasi.fdSeek(fd, offset, whence, filesize);

        if (errno == ERRNO_SUCCESS) {
            return filesize.getLong();
        } else {
            throw new IOException(errnoMessage("fd_seek", errno));
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
        return (int) new WasiVirtualFile(fd, null).stat().filesize;
    }

    @Override
    public void resize(int size) throws IOException {
        short errno = Wasi.fdFilestatSetSize(fd, size);

        if (errno != ERRNO_SUCCESS) {
            throw new IOException(errnoMessage("fd_filestat_set_size", errno));
        }
    }

    @Override
    public void close() throws IOException {
        if (this.fd >= 0) {
            int fd = this.fd;
            this.fd = -1;

            short errno = Wasi.fdClose(fd);

            if (errno != ERRNO_SUCCESS) {
                throw new IOException(errnoMessage("fd_close", errno));
            }
        }
    }

    @Override
    public void flush() throws IOException {
        short errno = Wasi.fdSync(fd);

        if (errno != ERRNO_SUCCESS) {
            throw new IOException(errnoMessage("fd_sync", errno));
        }
    }
}