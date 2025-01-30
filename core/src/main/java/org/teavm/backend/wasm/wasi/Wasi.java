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

    public static final int AF_INET = 0;
    public static final int AF_INET6 = 1;
    public static final int AF_UNIX = 2;

    public static final int INET4 = 0;
    public static final int INET6 = 1;
    public static final int INET_UNSPEC = 2;

    public static final int SOCK_ANY = -1;
    public static final int SOCK_DGRAM = 0;
    public static final int SOCK_STREAM = 1;

    public static final int SHUT_RD = 0;
    public static final int SHUT_WR = 1;
    public static final int SHUT_RDWR = 2;

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

    @Import(name = "sock_open", module = "wasi_snapshot_preview1")
    public static native int sockOpen(int fd, int af, int socktype, Address sockfd);

    @Import(name = "sock_bind", module = "wasi_snapshot_preview1")
    public static native int sockBind(int fd, Address addr);

    @Import(name = "sock_listen", module = "wasi_snapshot_preview1")
    public static native int sockListen(int fd, int backlog);

    @Import(name = "sock_connect", module = "wasi_snapshot_preview1")
    public static native int sockConnect(int fd, Address addr);

    @Import(name = "sock_set_reuse_addr", module = "wasi_snapshot_preview1")
    public static native int sockSetReuseAddr(int fd, int reuse);

    @Import(name = "sock_set_broadcast", module = "wasi_snapshot_preview1")
    public static native int sockSetBroadcast(int fd, int option);

    @Import(name = "sock_addr_local", module = "wasi_snapshot_preview1")
    public static native int sockAddrLocal(int fd, Address addr);

    @Import(name = "sock_addr_remote", module = "wasi_snapshot_preview1")
    public static native int sockAddrRemote(int fd, Address addr);

    @Import(name = "sock_recv_from", module = "wasi_snapshot_preview1")
    public static native int sockRecvFrom(int fd, Address riData, int riDataLen,
            int riFlags, Address srcAddr, Address addrLen);

    @Import(name = "sock_send_to", module = "wasi_snapshot_preview1")
    public static native int sockSendTo(int fd, Address siData, int siDataLen,
            int siFlags, Address destAddr, Address addrLen);

    @Import(name = "sock_addr_resolve", module = "wasi_snapshot_preview1")
    public static native int sockAddrResolve(Address node, Address service, Address hints,
            Address res, int maxResLen, Address resLen);

    @Import(name = "sock_shutdown", module = "wasi_snapshot_preview1")
    public static native int sockShutdown(int fd, int how);

    @Import(name = "sock_accept", module = "wasi_snapshot_preview1")
    public static native int sockAccept(int fd, int flags, Address fdNew);

    @Import(name = "sock_get_keep_alive", module = "wasi_snapshot_preview1")
    public static native int sockGetKeepAlive(int fd, Address option);

    @Import(name = "sock_set_keep_alive", module = "wasi_snapshot_preview1")
    public static native int sockSetKeepAlive(int fd, int option);

    @Import(name = "sock_get_reuse_addr", module = "wasi_snapshot_preview1")
    public static native int sockGetReuseAddr(int fd, Address option);

    @Import(name = "sock_get_recv_buf_size", module = "wasi_snapshot_preview1")
    public static native int sockGetRecvBufSize(int fd, Address size);

    @Import(name = "sock_set_recv_buf_size", module = "wasi_snapshot_preview1")
    public static native int sockSetRecvBufSize(int fd, int size);

    @Import(name = "sock_get_send_buf_size", module = "wasi_snapshot_preview1")
    public static native int sockGetSendBufSize(int fd, Address size);

    @Import(name = "sock_set_send_buf_size", module = "wasi_snapshot_preview1")
    public static native int sockSetSendBufSize(int fd, int size);

    @Import(name = "sock_get_linger", module = "wasi_snapshot_preview1")
    public static native int sockGetLinger(int fd, Address isEnabled, Address linger);

    @Import(name = "sock_set_linger", module = "wasi_snapshot_preview1")
    public static native int sockSetLinger(int fd, int isEnabled, int linger);

    @Import(name = "sock_get_recv_timeout", module = "wasi_snapshot_preview1")
    public static native int sockGetRecvTimeout(int fd, Address timeout);

    @Import(name = "sock_set_recv_timeout", module = "wasi_snapshot_preview1")
    public static native int sockSetRecvTimeout(int fd, int timeout);

    @Import(name = "sock_get_tcp_no_delay", module = "wasi_snapshot_preview1")
    public static native int sockGetTcpNoDelay(int fd, Address option);

    @Import(name = "sock_set_tcp_no_delay", module = "wasi_snapshot_preview1")
    public static native int sockSetTcpNoDelay(int fd, int option);
}
