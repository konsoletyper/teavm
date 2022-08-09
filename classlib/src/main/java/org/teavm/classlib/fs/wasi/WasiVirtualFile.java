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

import static org.teavm.interop.wasi.Wasi.DIRFLAGS_FOLLOW_SYMLINKS;
import static org.teavm.interop.wasi.Wasi.ERRNO_BADF;
import static org.teavm.interop.wasi.Wasi.ERRNO_EXIST;
import static org.teavm.interop.wasi.Wasi.ERRNO_NOENT;
import static org.teavm.interop.wasi.Wasi.ERRNO_SUCCESS;
import static org.teavm.interop.wasi.Wasi.FDFLAGS_APPEND;
import static org.teavm.interop.wasi.Wasi.FILETYPE_DIRECTORY;
import static org.teavm.interop.wasi.Wasi.FILETYPE_REGULAR_FILE;
import static org.teavm.interop.wasi.Wasi.FSTFLAGS_MTIME;
import static org.teavm.interop.wasi.Wasi.OFLAGS_CREATE;
import static org.teavm.interop.wasi.Wasi.OFLAGS_DIRECTORY;
import static org.teavm.interop.wasi.Wasi.OFLAGS_EXCLUSIVE;
import static org.teavm.interop.wasi.Wasi.PRESTAT_DIR;
import static org.teavm.interop.wasi.Wasi.RIGHTS_CREATE_DIRECTORY;
import static org.teavm.interop.wasi.Wasi.RIGHTS_CREATE_FILE;
import static org.teavm.interop.wasi.Wasi.RIGHTS_FD_FILESTAT_GET;
import static org.teavm.interop.wasi.Wasi.RIGHTS_FD_FILESTAT_SET_SIZE;
import static org.teavm.interop.wasi.Wasi.RIGHTS_READ;
import static org.teavm.interop.wasi.Wasi.RIGHTS_SEEK;
import static org.teavm.interop.wasi.Wasi.RIGHTS_SYNC;
import static org.teavm.interop.wasi.Wasi.RIGHTS_TELL;
import static org.teavm.interop.wasi.Wasi.RIGHTS_WRITE;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.teavm.classlib.fs.VirtualFile;
import org.teavm.classlib.fs.VirtualFileAccessor;
import org.teavm.interop.Address;
import org.teavm.interop.wasi.Wasi;
import org.teavm.interop.wasi.Wasi.ErrnoException;

public class WasiVirtualFile implements VirtualFile {
    // Enough room for a 64-byte WASI `filestat`, plus 8 bytes of padding for alignment:
    private static final byte[] SEVENTY_TWO_BYTE_BUFFER = new byte[72];

    private final int dirFd;
    private final String path;

    public WasiVirtualFile(int dirFd, String path) {
        this.dirFd = dirFd;
        this.path = path;
    }

    public WasiVirtualFile(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        List<Preopened> list = getPreopened();
        int bestFd = -1;
        String bestName = "";
        for (Preopened preopened : list) {
            if (path.equals(preopened.name)) {
                bestFd = preopened.fd;
                bestName = "";
                path = null;
                break;
            }

            if (preopened.name.length() > bestName.length() && path.startsWith(preopened.name + "/")) {
                bestFd = preopened.fd;
                bestName = preopened.name;
            }
        }

        if (bestFd == -1) {
            path = null;
        } else if (path != null && path.startsWith(bestName + "/")) {
            path = path.substring(bestName.length() + 1);
        }

        this.dirFd = bestFd;
        this.path = path;
    }

    static List<Preopened> getPreopened() {
        byte[] prestatBuffer = SEVENTY_TWO_BYTE_BUFFER;
        Address prestat = Address.align(Address.ofData(prestatBuffer), 4);
        List<Preopened> list = new ArrayList<>();
        int fd = 3; // skip stdin, stdout, and stderr
        while (true) {
            short errno = Wasi.fdPrestatGet(fd, prestat);

            if (errno == ERRNO_SUCCESS) {
                if (prestat.getByte() == PRESTAT_DIR) {
                    int length = prestat.add(4).getInt();
                    byte[] bytes = new byte[length];
                    errno = Wasi.fdPrestatDirName(fd, Address.ofData(bytes), length);

                    if (errno == ERRNO_SUCCESS && length > 0) {
                        if (bytes[length - 1] == 0) {
                            length -= 1;
                        }

                        list.add(new Preopened(fd, new String(bytes, 0, length, StandardCharsets.UTF_8)));
                    } else {
                        throw new ErrnoException("fd_prestat_dir_name", errno);
                    }
                }

                fd += 1;
            } else {
                if (errno == ERRNO_BADF) {
                    return list;
                } else {
                    throw new ErrnoException("fd_prestat_get", errno);
                }
            }
        }
    }

    @Override
    public String getName() {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    Stat stat() {
        if (dirFd == -1) {
            return null;
        }

        byte[] filestatBuffer = SEVENTY_TWO_BYTE_BUFFER;
        Address filestat = Address.align(Address.ofData(filestatBuffer), 8);
        short errno;
        String sysCall;

        if (path == null) {
            sysCall = "fd_filestat_get";
            errno = Wasi.fdFilestatGet(dirFd, filestat);
        } else {
            sysCall = "path_filestat_get";
            byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
            errno = Wasi.pathFilestatGet(dirFd, DIRFLAGS_FOLLOW_SYMLINKS, Address.ofData(bytes), bytes.length,
                                         filestat);
        }

        if (errno == ERRNO_SUCCESS) {
            Stat stat = new Stat((byte) filestat.add(16).getByte(), filestat.add(48).getLong(),
                                 filestat.add(32).getLong());
            return stat;
        } else {
            if (errno == ERRNO_NOENT) {
                return null;
            } else {
                throw new ErrnoException(sysCall, errno);
            }
        }
    }

    @Override
    public boolean isDirectory() {
        Stat stat = stat();
        return stat != null && stat.filetype == FILETYPE_DIRECTORY;
    }

    @Override
    public boolean isFile() {
        Stat stat = stat();
        return stat != null && stat.filetype == FILETYPE_REGULAR_FILE;
    }

    private static String[] listFiles(int fd) {
        if (fd == -1) {
            return null;
        }

        byte[] sizeBuffer = SEVENTY_TWO_BYTE_BUFFER;
        Address size = Address.align(Address.ofData(sizeBuffer), 4);
        ArrayList<String> list = new ArrayList<>();
        final int direntSize = 24;
        int bufferSize = direntSize + 256;
        byte[] direntBuffer = new byte[bufferSize + 8];
        Address dirent = Address.align(Address.ofData(direntBuffer), 8);
        long cookie = 0;

        while (true) {
            short errno = Wasi.fdReaddir(fd, dirent, bufferSize, cookie, size);

            if (errno == ERRNO_SUCCESS) {
                int length = dirent.add(16).getInt();

                if (size.getInt() < length) {
                    return list.toArray(new String[list.size()]);
                } else if (direntSize + length > bufferSize) {
                    bufferSize = direntSize + length;
                    direntBuffer = new byte[bufferSize + 8];
                    dirent = Address.align(Address.ofData(direntBuffer), 8);
                } else {
                    cookie = dirent.getLong();
                    byte[] bytes = new byte[length];
                    Wasi.getBytes(dirent.add(direntSize), bytes, 0, length);
                    // TODO: This is probably not guaranteed to be UTF-8
                    String name = new String(bytes, StandardCharsets.UTF_8);
                    if (!name.startsWith(".")) {
                        list.add(name);
                    }
                }
            } else {
                throw new ErrnoException("fd_read_dir", errno);
            }
        }
    }

    @Override
    public String[] listFiles() {
        try {
            if (path == null) {
                return listFiles(dirFd);
            } else {
                int fd = open(OFLAGS_DIRECTORY, RIGHTS_READ, (short) 0);
                try {
                    return listFiles(fd);
                } finally {
                    close(fd);
                }
            }
        } catch (ErrnoException e) {
            return null;
        }
    }

    private int open(short oflags, long rights, short fdflags) {
        if (path == null || dirFd == -1) {
            throw new ErrnoException("path_open", ERRNO_BADF);
        }

        byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
        byte[] fdBuffer = SEVENTY_TWO_BYTE_BUFFER;
        Address fd = Address.align(Address.ofData(fdBuffer), 4);
        short errno = Wasi.pathOpen(dirFd, DIRFLAGS_FOLLOW_SYMLINKS, Address.ofData(bytes), bytes.length, oflags,
                                    rights, rights, fdflags, fd);

        if (errno == ERRNO_SUCCESS) {
            return fd.getInt();
        } else {
            throw new ErrnoException("path_open", errno);
        }
    }

    private static void close(int fd) {
        // Ignore errno since this is only called in contexts where there's no need to handle the error.
        Wasi.fdClose(fd);
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        long rights = 0;
        if (readable) {
            rights |= RIGHTS_FD_FILESTAT_GET | RIGHTS_SEEK | RIGHTS_TELL | RIGHTS_READ;
        }
        if (writable) {
            rights |= RIGHTS_FD_FILESTAT_SET_SIZE | RIGHTS_WRITE | RIGHTS_SYNC;
        }
        short fdflags = (short) 0;
        if (append) {
            fdflags |= FDFLAGS_APPEND;
        }
        try {
            return new WasiVirtualFileAccessor(open((short) 0, rights, fdflags));
        } catch (ErrnoException e) {
            return null;
        }
    }

    private static boolean createFile(int fd, String fileName) throws IOException {
        if (fd == -1) {
            throw new FileNotFoundException();
        }

        try {
            close(new WasiVirtualFile(fd, fileName).open((short) (OFLAGS_CREATE | OFLAGS_EXCLUSIVE), 0, (short) 0));
            return true;
        } catch (ErrnoException e) {
            if (e.getErrno() == ERRNO_EXIST) {
                return false;
            } else {
                throw new IOException(e);
            }
        }
    }

    @Override
    public boolean createFile(String fileName) throws IOException {
        if (path == null) {
            return createFile(dirFd, fileName);
        } else {
            try {
                int fd = open(OFLAGS_DIRECTORY, RIGHTS_CREATE_FILE, (short) 0);
                try {
                    return createFile(fd, fileName);
                } finally {
                    close(fd);
                }
            } catch (ErrnoException e) {
                if (e.getErrno() == ERRNO_EXIST) {
                    return false;
                } else {
                    throw new IOException(e);
                }
            }
        }
    }

    private static boolean createDirectory(int fd, String fileName) {
        if (fd == -1) {
            return false;
        }

        byte[] bytes = fileName.getBytes(StandardCharsets.UTF_8);
        short errno = Wasi.pathCreateDirectory(fd, Address.ofData(bytes), bytes.length);

        return errno == ERRNO_SUCCESS;
    }

    @Override
    public boolean createDirectory(String fileName) {
        if (path == null) {
            return createDirectory(dirFd, fileName);
        } else {
            try {
                int fd = open(OFLAGS_DIRECTORY, RIGHTS_CREATE_DIRECTORY, (short) 0);
                try {
                    return createDirectory(fd, fileName);
                } finally {
                    close(fd);
                }
            } catch (ErrnoException e) {
                return false;
            }
        }
    }

    @Override
    public boolean delete() {
        if (path == null || dirFd == -1) {
            return false;
        }

        byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
        short errno = Wasi.pathUnlinkFile(dirFd, Address.ofData(bytes), bytes.length);

        return errno == ERRNO_SUCCESS;
    }

    private static boolean adopt(int fd, WasiVirtualFile file, String fileName) {
        if (fd == -1 || file.dirFd == -1) {
            return false;
        }

        byte[] newBytes = fileName.getBytes(StandardCharsets.UTF_8);
        byte[] oldBytes = file.path.getBytes(StandardCharsets.UTF_8);
        short errno = Wasi.pathRename(file.dirFd, Address.ofData(oldBytes), oldBytes.length, fd,
                                      Address.ofData(newBytes), newBytes.length);

        return errno == ERRNO_SUCCESS;
    }

    @Override
    public boolean adopt(VirtualFile file, String fileName) {
        WasiVirtualFile wasiFile = (WasiVirtualFile) file;

        if (wasiFile.path == null || fileName == null) {
            return false;
        }

        if (path == null) {
            return adopt(dirFd, wasiFile, fileName);
        } else {
            try {
                int fd = open(OFLAGS_DIRECTORY, RIGHTS_READ | RIGHTS_WRITE, (short) 0);
                try {
                    return adopt(fd, wasiFile, fileName);
                } finally {
                    close(fd);
                }
            } catch (ErrnoException e) {
                return false;
            }
        }
    }

    @Override
    public boolean canRead() {
        try {
            close(open((short) 0, RIGHTS_READ, (short) 0));
            return true;
        } catch (ErrnoException e) {
            return false;
        }
    }

    @Override
    public boolean canWrite() {
        try {
            close(open((short) 0, RIGHTS_WRITE, (short) 0));
            return true;
        } catch (ErrnoException e) {
            return false;
        }
    }

    @Override
    public long lastModified() {
        Stat stat = stat();
        return stat == null ? 0 : stat.mtime / 1000000L;
    }

    @Override
    public boolean setLastModified(long lastModified) {
        if (dirFd == -1) {
            return false;
        }

        short errno;
        if (path == null) {
            errno = Wasi.fdFilestatSetTimes(dirFd, 0, lastModified * 1000000L, FSTFLAGS_MTIME);
        } else {
            byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
            errno = Wasi.pathFilestatSetTimes(dirFd, DIRFLAGS_FOLLOW_SYMLINKS, Address.ofData(bytes), bytes.length, 0,
                                              lastModified * 1000000L, FSTFLAGS_MTIME);
        }

        return errno == ERRNO_SUCCESS;
    }

    @Override
    public boolean setReadOnly(boolean readOnly) {
        // TODO: is this possible on WASI?
        return false;
    }

    @Override
    public int length() {
        Stat stat = stat();
        return stat == null ? 0 : (int) stat.filesize;
    }

    static class Stat {
        public final byte filetype;
        public final long mtime;
        public final long filesize;

        public Stat(byte filetype, long mtime, long filesize) {
            this.filetype = filetype;
            this.mtime = mtime;
            this.filesize = filesize;
        }
    }

    private static class Preopened {
        public final int fd;
        public final String name;

        public Preopened(int fd, String name) {
            this.fd = fd;
            this.name = name;
        }
    }
}
