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

import org.teavm.interop.Address;
import org.teavm.interop.Import;

public final class Wasi {
    public static final int CLOCKID_REALTIME = 0;

    public static final byte PRESTAT_DIR = 0;

    public static final short ERRNO_SUCCESS = 0;
    public static final short ERRNO_BADF = 8;
    public static final short ERRNO_EXIST = 20;
    public static final short ERRNO_NOENT = 44;

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
    public static final long RIGHTS_FD_READDIR = 1L << 14;
    public static final long RIGHTS_FD_FILESTAT_GET = 1L << 21;
    public static final long RIGHTS_FD_FILESTAT_SET_SIZE = 1L << 22;

    public static final short FDFLAGS_APPEND = 1 << 0;

    public static final short FSTFLAGS_MTIME = 1 << 2;

    public static final byte WHENCE_START = 0;
    public static final byte WHENCE_CURRENT = 1;
    public static final byte WHENCE_END = 2;

    private Wasi() {
    }

    @Import(name = "clock_time_get", module = "wasi_snapshot_preview1")
    public static native short clockTimeGet(int clockId, long precision, LongResult result);

    @Import(name = "args_sizes_get", module = "wasi_snapshot_preview1")
    public static native short argsSizesGet(IntResult argvSize, IntResult argvBufSize);

    @Import(name = "args_get", module = "wasi_snapshot_preview1")
    public static native short argsGet(Address argv, Address argvBuf);

    @Import(name = "fd_read", module = "wasi_snapshot_preview1")
    public static native short fdRead(int fd, IOVec vecArray, int vecArrayLength, SizeResult size);

    @Import(name = "fd_write", module = "wasi_snapshot_preview1")
    public static native short fdWrite(int fd, IOVec vectors, int vectorsCont, SizeResult result);

    @Import(name = "fd_tell", module = "wasi_snapshot_preview1")
    public static native short fdTell(int fd, SizeResult size);

    @Import(name = "fd_seek", module = "wasi_snapshot_preview1")
    public static native short fdSeek(int fd, long offset, byte whence, SizeResult size);

    @Import(name = "fd_prestat_get", module = "wasi_snapshot_preview1")
    public static native short fdPrestatGet(int fd, Prestat prestat);

    @Import(name = "fd_prestat_dir_name", module = "wasi_snapshot_preview1")
    public static native short fdPrestatDirName(int fd, Address buffer, int bufferLength);

    @Import(name = "fd_filestat_get", module = "wasi_snapshot_preview1")
    public static native short fdFilestatGet(int fd, Filestat filestat);

    @Import(name = "path_filestat_get", module = "wasi_snapshot_preview1")
    public static native short pathFilestatGet(int fd, int lookupFlags, Address path, int pathLength,
            Filestat filestat);

    @Import(name = "fd_readdir", module = "wasi_snapshot_preview1")
    public static native short fdReaddir(int fd, Address dirent, int direntSize, long cookie, IntResult size);

    @Import(name = "path_open", module = "wasi_snapshot_preview1")
    public static native short pathOpen(int dirFd, int lookupFlags, Address path, int pathLength, short oflags,
            long baseRights, long inheritingRights, short fdflags, FdResult fd);

    @Import(name = "fd_close", module = "wasi_snapshot_preview1")
    public static native short fdClose(int fd);

    @Import(name = "path_create_directory", module = "wasi_snapshot_preview1")
    public static native short pathCreateDirectory(int fd, Address path, int pathLength);

    @Import(name = "path_unlink_file", module = "wasi_snapshot_preview1")
    public static native short pathUnlinkFile(int fd, Address path, int pathLength);

    @Import(name = "path_remove_directory", module = "wasi_snapshot_preview1")
    public static native short pathRemoveDirectory(int fd, Address path, int pathLength);

    @Import(name = "path_rename", module = "wasi_snapshot_preview1")
    public static native short pathRename(int oldFd, Address oldPath, int oldPathLength, int newFd, Address newPath,
            int newPathLength);

    @Import(name = "fd_filestat_set_times", module = "wasi_snapshot_preview1")
    public static native short fdFilestatSetTimes(int fd, long atime, long mtime, short fstflags);

    @Import(name = "path_filestat_set_times", module = "wasi_snapshot_preview1")
    public static native short pathFilestatSetTimes(int fd, int lookupFlags, Address path, int pathLength,
            long atime, long mtime, short fstflags);

    @Import(name = "fd_filestat_set_size", module = "wasi_snapshot_preview1")
    public static native short fdFilestatSetSize(int fd, long size);

    @Import(name = "fd_sync", module = "wasi_snapshot_preview1")
    public static native short fdSync(int fd);

    @Import(name = "random_get", module = "wasi_snapshot_preview1")
    public static native short randomGet(Address buffer, int bufferLength);

}
