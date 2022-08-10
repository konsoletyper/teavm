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
package org.teavm.interop.wasi;

import static org.teavm.interop.wasi.Memory.free;
import static org.teavm.interop.wasi.Memory.malloc;
import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.interop.Unmanaged;

public final class Wasi {
    public static final int CLOCKID_REALTIME = 0;
    public static final short ERRNO_SUCCESS = 0;
    public static final short ERRNO_BADF = 8;
    public static final short ERRNO_EXIST = 20;
    public static final short ERRNO_NOENT = 44;
    public static final byte PRESTAT_DIR = 0;
    public static final byte FILETYPE_DIRECTORY = 3;
    public static final byte FILETYPE_REGULAR_FILE = 4;
    public static final int DIRFLAGS_FOLLOW_SYMLINKS = 1;
    public static final short OFLAGS_CREATE = 1 << 0;
    public static final short OFLAGS_DIRECTORY = 1 << 1;
    public static final short OFLAGS_EXCLUSIVE = 1 << 2;
    public static final long RIGHTS_READ = 1L << 1;
    public static final long RIGHTS_SEEK = 1L << 2;
    public static final long RIGHTS_TELL = 1L << 5;
    public static final long RIGHTS_WRITE = 1L << 6;
    public static final long RIGHTS_SYNC = 1L << 4;
    public static final long RIGHTS_CREATE_DIRECTORY = 1L << 9;
    public static final long RIGHTS_CREATE_FILE = 1L << 10;
    public static final long RIGHTS_FD_FILESTAT_GET = 1L << 21;
    public static final long RIGHTS_FD_FILESTAT_SET_SIZE = 1L << 22;
    public static final short FDFLAGS_APPEND = 1 << 0;
    public static final short FSTFLAGS_MTIME = 1 << 2;
    public static final byte WHENCE_START = 0;
    public static final byte WHENCE_CURRENT = 1;
    public static final byte WHENCE_END = 2;

    private static byte[] initialRandom;
    private static long nextRandom;

    private Wasi() {
    }

    @Import(name = "args_sizes_get", module = "wasi_snapshot_preview1")
    public static native short argsSizesGet(Address argvSize, Address argvBufSize);

    @Import(name = "args_get", module = "wasi_snapshot_preview1")
    public static native short argsGet(Address argv, Address argvBuf);

    @Import(name = "clock_time_get", module = "wasi_snapshot_preview1")
    public static native short clockTimeGet(int clockid, long precision, Address timestamp);

    @Import(name = "environ_sizes_get", module = "wasi_snapshot_preview1")
    public static native short environSizesGet(Address environSize, Address environBufSize);

    @Import(name = "environ_get", module = "wasi_snapshot_preview1")
    public static native short environGet(Address environ, Address environBuf);

    @Import(name = "path_create_directory", module = "wasi_snapshot_preview1")
    public static native short pathCreateDirectory(int fd, Address path, int pathLength);

    @Import(name = "path_filestat_set_times", module = "wasi_snapshot_preview1")
    public static native short pathFilestatSetTimes(int fd, int lookupFlags, Address path, int pathLength,
                                                    long atime, long mtime, short fstflags);

    @Import(name = "path_rename", module = "wasi_snapshot_preview1")
    public static native short pathRename(int oldFd, Address oldPath, int oldPathLength, int newFd, Address newPath,
                                          int newPathLength);

    @Import(name = "path_unlink_file", module = "wasi_snapshot_preview1")
    public static native short pathUnlinkFile(int fd, Address path, int pathLength);

    @Import(name = "fd_close", module = "wasi_snapshot_preview1")
    public static native short fdClose(int fd);

    @Import(name = "fd_filestat_get", module = "wasi_snapshot_preview1")
    public static native short fdFilestatGet(int fd, Address filestat);

    @Import(name = "fd_filestat_set_size", module = "wasi_snapshot_preview1")
    public static native short fdFilestatSetSize(int fd, long size);

    @Import(name = "fd_filestat_set_times", module = "wasi_snapshot_preview1")
    public static native short fdFilestatSetTimes(int fd, long atime, long mtime, short fstflags);

    @Import(name = "fd_prestat_get", module = "wasi_snapshot_preview1")
    public static native short fdPrestatGet(int fd, Address prestat);

    @Import(name = "fd_prestat_dir_name", module = "wasi_snapshot_preview1")
    public static native short fdPrestatDirName(int fd, Address buffer, int bufferLength);

    @Import(name = "fd_read", module = "wasi_snapshot_preview1")
    public static native short fdRead(int fd, Address vec, int vecCount, Address size);

    @Import(name = "fd_seek", module = "wasi_snapshot_preview1")
    public static native short fdSeek(int fd, long offset, byte whence, Address size);

    @Import(name = "fd_sync", module = "wasi_snapshot_preview1")
    public static native short fdSync(int fd);

    @Import(name = "fd_readdir", module = "wasi_snapshot_preview1")
    public static native short fdReaddir(int fd, Address dirent, int direntSize, long cookie, Address size);

    @Import(name = "fd_tell", module = "wasi_snapshot_preview1")
    public static native short fdTell(int fd, Address size);

    @Import(name = "fd_write", module = "wasi_snapshot_preview1")
    public static native short fdWrite(int fd, Address vec, int vecCount, Address size);

    @Import(name = "path_filestat_get", module = "wasi_snapshot_preview1")
    public static native short pathFilestatGet(int fd, int lookupFlags, Address path, int pathLength,
                                               Address filestat);

    @Import(name = "path_open", module = "wasi_snapshot_preview1")
    public static native short pathOpen(int dirFd, int lookupFlags, Address path, int pathLength, short oflags,
                                        long baseRights, long inheritingRights, short fdflags, Address fd);

    @Import(name = "random_get", module = "wasi_snapshot_preview1")
    public static native short randomGet(Address buffer, int bufferLength);

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void getBytes(Address address, byte[] bytes, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            bytes[offset + i] = address.add(i).getByte();
        }
    }

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void putBytes(Address address, byte[] bytes, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            address.add(i).putByte(bytes[offset + i]);
        }
    }

    public static String errnoMessage(String sysCall, short errno) {
        // TODO: Provide a friendly message for each case.
        return "errno for " + sysCall + ": " + errno;
    }

    @Unmanaged
    public static void printBuffer(int fd, Address buffer, int length) {
        final int vecSize = 8;
        final int vecAlign = 4;
        Address vec = malloc(vecSize, vecAlign);
        final int sizeSize = 4;
        final int sizeAlign = 4;
        Address size = malloc(sizeSize, sizeAlign);

        int index = 0;
        while (true) {
            vec.putInt(buffer.add(index).toInt());
            vec.add(4).putInt(length - index);
            short errno = fdWrite(fd, vec, 1, size);

            if (errno == 0) {
                int sizeValue = size.getInt();
                index += sizeValue;
                if (index >= length) {
                    free(vec, vecSize, vecAlign);
                    free(size, sizeSize, sizeAlign);
                    return;
                }
            } else {
                free(vec, vecSize, vecAlign);
                free(size, sizeSize, sizeAlign);
                return;
            }
        }
    }

    public static double random() {
        return (((long) nextRandom(26) << 27) + nextRandom(27)) / (double) (1L << 53);
    }

    private static int nextRandom(int bits) {
        if (initialRandom == null) {
            initialRandom = new byte[8];
            short errno = Wasi.randomGet(Address.ofData(initialRandom), 8);

            if (errno != ERRNO_SUCCESS) {
                throw new ErrnoException("random_get", errno);
            }

            nextRandom = (initialRandom[0] & 0xFFL)
                | ((initialRandom[1] & 0xFFL) << 8)
                | ((initialRandom[2] & 0xFFL) << 16)
                | ((initialRandom[3] & 0xFFL) << 24)
                | ((initialRandom[4] & 0xFFL) << 32)
                | ((initialRandom[5] & 0xFFL) << 40)
                | ((initialRandom[6] & 0xFFL) << 48)
                | ((initialRandom[7] & 0xFFL) << 56);
        }

        nextRandom = ((nextRandom * 0x5DEECE66DL) + 0xBL) & ((1L << 48) - 1);
        return (int) (nextRandom >>> (48 - bits));
    }

    public static class ErrnoException extends RuntimeException {
        private final String sysCall;
        private final short errno;

        public ErrnoException(String sysCall, short errno) {
            this.sysCall = sysCall;
            this.errno = errno;
        }

        public String getMessage() {
            return errnoMessage(sysCall, errno);
        }

        public short getErrno() {
            return errno;
        }
    }
}
