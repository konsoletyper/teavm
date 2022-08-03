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
package org.teavm.classlib.fs.wasi;

import static org.teavm.interop.wasi.Memory.free;
import static org.teavm.interop.wasi.Memory.malloc;
import static org.teavm.interop.wasi.Wasi.ERRNO_SUCCESS;
import static org.teavm.interop.wasi.Wasi.WHENCE_CURRENT;
import static org.teavm.interop.wasi.Wasi.WHENCE_START;
import static org.teavm.interop.wasi.Wasi.errnoMessage;
import java.io.IOException;
import org.teavm.classlib.fs.VirtualFileAccessor;
import org.teavm.interop.Address;
import org.teavm.interop.wasi.Wasi;

public class WasiVirtualFileAccessor implements VirtualFileAccessor {
    private int fd;

    public WasiVirtualFileAccessor(int fd) {
        this.fd = fd;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        Address myBuffer = malloc(length, 1);
        final int vecSize = 8;
        final int vecAlign = 4;
        Address vec = malloc(vecSize, vecAlign);
        vec.putInt(myBuffer.toInt());
        vec.add(4).putInt(length);
        final int sizeSize = 4;
        final int sizeAlign = 4;
        Address size = malloc(sizeSize, sizeAlign);
        short errno = Wasi.fdRead(fd, vec, 1, size);
        free(vec, vecSize, vecAlign);

        if (errno == ERRNO_SUCCESS) {
            int sizeValue = size.getInt();
            Wasi.getBytes(myBuffer, buffer, offset, sizeValue);
            free(size, sizeSize, sizeAlign);
            free(myBuffer, length, 1);
            return sizeValue;
        } else {
            free(size, sizeSize, sizeAlign);
            free(myBuffer, length, 1);
            throw new IOException(errnoMessage("fd_read", errno));
        }
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        Address myBuffer = malloc(length, 1);
        Wasi.putBytes(myBuffer, buffer, offset, length);
        final int vecSize = 8;
        final int vecAlign = 4;
        Address vec = malloc(vecSize, vecAlign);
        final int sizeSize = 4;
        final int sizeAlign = 4;
        Address size = malloc(sizeSize, sizeAlign);

        int index = 0;
        while (true) {
            vec.putInt(myBuffer.add(index).toInt());
            vec.add(4).putInt(length - index);
            short errno = Wasi.fdWrite(fd, vec, 1, size);

            if (errno == ERRNO_SUCCESS) {
                int sizeValue = size.getInt();
                index += sizeValue;
                if (index >= length) {
                    free(vec, vecSize, vecAlign);
                    free(size, sizeSize, sizeAlign);
                    free(myBuffer, length, 1);
                    return;
                }
            } else {
                free(vec, vecSize, vecAlign);
                free(size, sizeSize, sizeAlign);
                free(myBuffer, length, 1);
                throw new IOException(errnoMessage("fd_write", errno));
            }
        }
    }

    @Override
    public int tell() throws IOException {
        final int filesizeSize = 8;
        final int filesizeAlign = 8;
        Address filesize = malloc(filesizeSize, filesizeAlign);
        short errno = Wasi.fdTell(fd, filesize);

        if (errno == ERRNO_SUCCESS) {
            long sizeValue = filesize.getLong();
            free(filesize, filesizeSize, filesizeAlign);
            return (int) sizeValue;
        } else {
            free(filesize, filesizeSize, filesizeAlign);
            throw new IOException(errnoMessage("fd_tell", errno));
        }
    }

    private long seek(long offset, byte whence) throws IOException {
        final int filesizeSize = 8;
        final int filesizeAlign = 8;
        Address filesize = malloc(filesizeSize, filesizeAlign);
        short errno = Wasi.fdSeek(fd, offset, whence, filesize);

        if (errno == ERRNO_SUCCESS) {
            long sizeValue = filesize.getLong();
            free(filesize, filesizeSize, filesizeAlign);
            return sizeValue;
        } else {
            free(filesize, filesizeSize, filesizeAlign);
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
