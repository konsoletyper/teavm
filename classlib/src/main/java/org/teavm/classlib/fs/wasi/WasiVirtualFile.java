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
import static org.teavm.interop.wasi.Wasi.DIRFLAGS_FOLLOW_SYMLINKS;
import static org.teavm.interop.wasi.Wasi.ERRNO_BADF;
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
        List<Preopened> list = new ArrayList<>();
        // skip stdin, stdout, and stderr
        int fd = 3;
        final int prestatSize = 8;
        final int prestatAlign = 4;
        Address prestat = malloc(prestatSize, prestatAlign);
        while (true) {
            short errno = Wasi.fdPrestatGet(fd, prestat);

            if (errno == ERRNO_SUCCESS) {
                final int prestatTag = 0;
                if (prestat.add(prestatTag).getByte() == PRESTAT_DIR) {
                    final int prestatBody = 4;
                    final int prestatDirDir = 0;
                    int length = prestat.add(prestatBody + prestatDirDir).getInt();
                    Address buffer = malloc(length, 1);
                    errno = Wasi.fdPrestatDirName(fd, buffer, length);

                    if (errno == ERRNO_SUCCESS) {
                        byte[] bytes = new byte[length];
                        Wasi.getBytes(buffer, bytes, 0, length);
                        free(buffer, length, 1);
                        list.add(new Preopened(fd, new String(bytes, StandardCharsets.UTF_8)));
                    } else {
                        free(buffer, length, 1);
                        free(prestat, prestatSize, prestatAlign);
                        throw new ErrnoException("fd_prestat_dir_name", errno);
                    }
                }

                fd += 1;
            } else {
                free(prestat, prestatSize, prestatAlign);
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

        final int filestatSize = 64;
        final int filestatAlign = 8;
        Address filestat = malloc(filestatSize, filestatAlign);
        short errno;
        String sysCall;

        if (path == null) {
            sysCall = "fd_filestat_get";
            errno = Wasi.fdFilestatGet(dirFd, filestat);
        } else {
            sysCall = "path_filestat_get";
            byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
            Address path = malloc(bytes.length, 1);
            Wasi.putBytes(path, bytes, 0, bytes.length);
            errno = Wasi.pathFilestatGet(dirFd, DIRFLAGS_FOLLOW_SYMLINKS, path, bytes.length, filestat);
            free(path, bytes.length, 1);
        }

        if (errno == ERRNO_SUCCESS) {
            final int filestatFileType = 16;
            final int filestatFileSize = 32;
            final int filestatMtime = 48;
            Stat stat = new Stat(filestat.add(filestatFileType).getByte(), filestat.add(filestatMtime).getLong(),
                                 filestat.add(filestatFileSize).getLong());
            free(filestat, filestatSize, filestatAlign);
            return stat;
        } else {
            free(filestat, filestatSize, filestatAlign);

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
        ArrayList<String> list = new ArrayList<>();
        final int direntSize = 24;
        final int direntAlign = 8;
        int bufferSize = direntSize + 256;
        Address dirent = malloc(bufferSize, direntAlign);
        final int sizeSize = 4;
        final int sizeAlign = 4;
        Address size = malloc(sizeSize, sizeAlign);
        long cookie = 0;

        while (true) {
            short errno = Wasi.fdReaddir(fd, dirent, bufferSize, cookie, size);

            if (errno == ERRNO_SUCCESS) {
                int sizeValue = size.getInt();

                final int direntNameLength = 16;
                int length = dirent.add(direntNameLength).getInt();

                if (sizeValue < length) {
                    free(size, sizeSize, sizeAlign);
                    free(dirent, bufferSize, direntAlign);
                    return list.toArray(new String[list.size()]);
                } else {
                    if (direntSize + length > bufferSize) {
                        free(dirent, bufferSize, direntAlign);
                        bufferSize = direntSize + length;
                        dirent = malloc(bufferSize, direntAlign);
                    } else {
                        final int direntCookie = 0;
                        cookie = dirent.add(direntCookie).getLong();
                        byte[] bytes = new byte[length];
                        Wasi.getBytes(dirent.add(direntSize), bytes, 0, length);
                        // TODO: This is probably not guaranteed to be UTF-8
                        String name = new String(bytes, StandardCharsets.UTF_8);
                        if (!name.startsWith(".")) {
                            list.add(name);
                        }
                    }
                }
            } else {
                free(size, sizeSize, sizeAlign);
                free(dirent, bufferSize, direntAlign);
                throw new ErrnoException("fd_read_dir", errno);
            }
        }
    }

    @Override
    public String[] listFiles() {
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
    }

    private int open(short oflags, long rights, short fdflags) {
        if (path == null) {
            throw new UnsupportedOperationException();
        }

        byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
        Address path = malloc(bytes.length, 1);
        Wasi.putBytes(path, bytes, 0, bytes.length);
        final int fdSize = 4;
        final int fdAlign = 4;
        Address fd = malloc(fdSize, fdAlign);
        short errno = Wasi.pathOpen(dirFd, DIRFLAGS_FOLLOW_SYMLINKS, path, bytes.length, oflags, rights, rights,
                                    fdflags, fd);
        free(path, bytes.length, 1);

        if (errno == ERRNO_SUCCESS) {
            int fdValue = fd.getInt();
            free(fd, fdSize, fdAlign);
            return fdValue;
        } else {
            free(fd, fdSize, fdAlign);
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
        return new WasiVirtualFileAccessor(open((short) 0, rights, fdflags));
    }

    private static boolean createFile(int fd, String fileName) {
        try {
            close(new WasiVirtualFile(fd, fileName).open((short) (OFLAGS_CREATE | OFLAGS_EXCLUSIVE), 0, (short) 0));
            return true;
        } catch (ErrnoException e) {
            return false;
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
                return false;
            }
        }
    }

    private static boolean createDirectory(int fd, String fileName) {
        byte[] bytes = fileName.getBytes(StandardCharsets.UTF_8);
        Address path = malloc(bytes.length, 1);
        Wasi.putBytes(path, bytes, 0, bytes.length);
        short errno = Wasi.pathCreateDirectory(fd, path, bytes.length);
        free(path, bytes.length, 1);

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
        if (path == null) {
            throw new UnsupportedOperationException();
        }

        byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
        Address path = malloc(bytes.length, 1);
        Wasi.putBytes(path, bytes, 0, bytes.length);
        short errno = Wasi.pathUnlinkFile(dirFd, path, bytes.length);
        free(path, bytes.length, 1);

        return errno == ERRNO_SUCCESS;
    }

    private static boolean adopt(int fd, WasiVirtualFile file, String fileName) {
        byte[] newBytes = fileName.getBytes(StandardCharsets.UTF_8);
        Address newPath = malloc(newBytes.length, 1);
        Wasi.putBytes(newPath, newBytes, 0, newBytes.length);
        byte[] oldBytes = file.path.getBytes(StandardCharsets.UTF_8);
        Address oldPath = malloc(oldBytes.length, 1);
        Wasi.putBytes(oldPath, oldBytes, 0, oldBytes.length);
        short errno = Wasi.pathRename(file.dirFd, oldPath, oldBytes.length, fd, newPath, newBytes.length);
        free(oldPath, oldBytes.length, 1);
        free(newPath, newBytes.length, 1);

        return errno == ERRNO_SUCCESS;
    }

    @Override
    public boolean adopt(VirtualFile file, String fileName) {
        WasiVirtualFile wasiFile = (WasiVirtualFile) file;

        if (wasiFile.path == null || fileName == null) {
            throw new UnsupportedOperationException();
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
        return stat().mtime / 1000000L;
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
            Address path = malloc(bytes.length, 1);
            Wasi.putBytes(path, bytes, 0, bytes.length);
            errno = Wasi.pathFilestatSetTimes(dirFd, DIRFLAGS_FOLLOW_SYMLINKS, path, bytes.length, 0,
                                              lastModified * 1000000L, FSTFLAGS_MTIME);
            free(path, bytes.length, 1);
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
