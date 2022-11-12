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

import static org.teavm.backend.wasm.wasi.Wasi.DIRFLAGS_FOLLOW_SYMLINKS;
import static org.teavm.backend.wasm.wasi.Wasi.ERRNO_EXIST;
import static org.teavm.backend.wasm.wasi.Wasi.ERRNO_SUCCESS;
import static org.teavm.backend.wasm.wasi.Wasi.FDFLAGS_APPEND;
import static org.teavm.backend.wasm.wasi.Wasi.FILETYPE_DIRECTORY;
import static org.teavm.backend.wasm.wasi.Wasi.FILETYPE_REGULAR_FILE;
import static org.teavm.backend.wasm.wasi.Wasi.FSTFLAGS_MTIME;
import static org.teavm.backend.wasm.wasi.Wasi.OFLAGS_CREATE;
import static org.teavm.backend.wasm.wasi.Wasi.OFLAGS_DIRECTORY;
import static org.teavm.backend.wasm.wasi.Wasi.OFLAGS_EXCLUSIVE;
import static org.teavm.backend.wasm.wasi.Wasi.RIGHTS_FD_FILESTAT_GET;
import static org.teavm.backend.wasm.wasi.Wasi.RIGHTS_FD_FILESTAT_SET_SIZE;
import static org.teavm.backend.wasm.wasi.Wasi.RIGHTS_FD_READDIR;
import static org.teavm.backend.wasm.wasi.Wasi.RIGHTS_READ;
import static org.teavm.backend.wasm.wasi.Wasi.RIGHTS_SEEK;
import static org.teavm.backend.wasm.wasi.Wasi.RIGHTS_SYNC;
import static org.teavm.backend.wasm.wasi.Wasi.RIGHTS_TELL;
import static org.teavm.backend.wasm.wasi.Wasi.RIGHTS_WRITE;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.teavm.backend.wasm.runtime.WasiBuffer;
import org.teavm.backend.wasm.wasi.Dirent;
import org.teavm.backend.wasm.wasi.FdResult;
import org.teavm.backend.wasm.wasi.Filestat;
import org.teavm.backend.wasm.wasi.IntResult;
import org.teavm.backend.wasm.wasi.Wasi;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;

public class WasiVirtualFile implements VirtualFile {
    private final WasiFileSystem fs;
    private final String fullPath;
    private boolean initialized;
    private int baseFd;
    private String path;

    WasiVirtualFile(WasiFileSystem fs, String path) {
        this.fs = fs;
        fullPath = path;
    }

    private void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        fs.findBestPreopened(fullPath);
        path = fs.bestPreopenedPath;
        baseFd = fs.bestPreopenedId;
        fs.bestPreopenedPath = null;
    }

    @Override
    public String getName() {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    Stat stat() {
        init();
        if (baseFd < 0) {
            return null;
        }
        Filestat filestat = WasiBuffer.getBuffer().toStructure();
        int errno;
        if (path != null) {
            byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
            errno = Wasi.pathFilestatGet(baseFd, DIRFLAGS_FOLLOW_SYMLINKS, Address.ofData(bytes), bytes.length,
                    filestat);
        } else {
            errno = Wasi.fdFilestatGet(baseFd, filestat);
        }
        if (errno == ERRNO_SUCCESS) {
            return new Stat((byte) filestat.fileType, filestat.lastModified, filestat.size);
        } else {
            return null;
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

    private String[] listFiles(int fd) {
        IntResult sizePtr = WasiBuffer.getBuffer().toStructure();
        ArrayList<String> list = new ArrayList<>();
        final int direntSize = Structure.sizeOf(Dirent.class);
        byte[] direntArray = fs.buffer;
        Address direntBuffer = Address.align(Address.ofData(direntArray), 8);
        int direntArrayOffset = (int) direntBuffer.diff(Address.ofData(direntArray));
        int bufferSize = direntArray.length - direntArrayOffset;
        long cookie = 0;

        outer:
        while (true) {
            short errno = Wasi.fdReaddir(fd, direntBuffer, bufferSize, cookie, sizePtr);
            if (errno != ERRNO_SUCCESS) {
                return null;
            }
            int size = sizePtr.value;
            int remainingSize = bufferSize;
            int entryOffset = 0;
            while (true) {
                if (remainingSize < direntSize) {
                    break;
                }
                Address direntPtr = direntBuffer.add(entryOffset);
                Dirent dirent = direntPtr.toStructure();
                int entryLength = direntSize + dirent.nameLength;
                if (entryLength > bufferSize) {
                    direntArray = new byte[entryLength * 3 / 2 + 8];
                    direntBuffer = Address.align(Address.ofData(direntArray), 8);
                    direntArrayOffset = (int) direntBuffer.diff(Address.ofData(direntArray));
                    bufferSize = direntArray.length - direntArrayOffset;
                    break;
                } else if (entryOffset + entryLength > bufferSize) {
                    break;
                }
                cookie = dirent.next;
                String name = new String(direntArray, direntArrayOffset + entryOffset + direntSize, dirent.nameLength,
                        StandardCharsets.UTF_8);
                if (!name.equals(".") && !name.equals("..")) {
                    list.add(name);
                }
                remainingSize -= entryLength;
                entryOffset += entryLength;
                if (entryOffset == size) {
                    break outer;
                }
            }
        }

        return list.toArray(new String[0]);
    }

    @Override
    public String[] listFiles() {
        init();
        if (baseFd < 0) {
            return null;
        }
        int fd = path != null ? open(OFLAGS_DIRECTORY, RIGHTS_READ | RIGHTS_FD_READDIR, (short) 0) : baseFd;
        if (fd < 0) {
            return null;
        }
        try {
            return listFiles(fd);
        } finally {
            if (path != null) {
                close(fd);
            }
        }
    }
    private int open(int fd, String path, short oflags, long rights, short fdflags) {
        byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
        FdResult fdPtr = WasiBuffer.getBuffer().toStructure();
        fs.errno = Wasi.pathOpen(fd, DIRFLAGS_FOLLOW_SYMLINKS, Address.ofData(bytes), bytes.length, oflags,
                rights, rights, fdflags, fdPtr);
        return fs.errno == ERRNO_SUCCESS ? fdPtr.value : -1;
    }


    private int open(short oflags, long rights, short fdflags) {
        return open(baseFd, path, oflags, rights, fdflags);
    }

    private static void close(int fd) {
        // Ignore errno since this is only called in contexts where there's no need to handle the error.
        Wasi.fdClose(fd);
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        init();
        if (baseFd < 0) {
            return null;
        }
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
        int fd = open((short) 0, rights, fdflags);
        return fd >= 0 ? new WasiVirtualFileAccessor(this, fd) : null;
    }

    @Override
    public boolean createFile(String fileName) throws IOException {
        init();
        if (baseFd < 0) {
            throw new IOException("Can't create file: access to directory not granted by WASI runtime");
        }
        int fd = open(baseFd, constructPath(path, fileName), (short) (OFLAGS_CREATE | OFLAGS_EXCLUSIVE), 0, (short) 0);
        if (fs.errno == ERRNO_EXIST) {
            return false;
        }
        if (fs.errno != ERRNO_SUCCESS) {
            throw new IOException("fd_open: " + fs.errno);
        }
        close(fd);
        return true;
    }

    @Override
    public boolean createDirectory(String fileName) {
        init();
        if (baseFd < 0) {
            return false;
        }
        String filePath = constructPath(path, fileName);
        byte[] bytes = filePath.getBytes(StandardCharsets.UTF_8);
        return Wasi.pathCreateDirectory(baseFd, Address.ofData(bytes), bytes.length) == ERRNO_SUCCESS;
    }

    @Override
    public boolean delete() {
        init();
        if (path == null || baseFd < 0) {
            return false;
        }
        if (isFile()) {
            byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
            return Wasi.pathUnlinkFile(baseFd, Address.ofData(bytes), bytes.length) == ERRNO_SUCCESS;
        } else if (isDirectory()) {
            byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
            return Wasi.pathRemoveDirectory(baseFd, Address.ofData(bytes), bytes.length) == ERRNO_SUCCESS;
        }
        return false;
    }

    private static boolean adopt(int fd, WasiVirtualFile file, String fileName) {
        byte[] newBytes = fileName.getBytes(StandardCharsets.UTF_8);
        byte[] oldBytes = file.path.getBytes(StandardCharsets.UTF_8);
        short errno = Wasi.pathRename(file.baseFd, Address.ofData(oldBytes), oldBytes.length, fd,
                Address.ofData(newBytes), newBytes.length);
        return errno == ERRNO_SUCCESS;
    }

    @Override
    public boolean adopt(VirtualFile file, String fileName) {
        if (!(file instanceof WasiVirtualFile)) {
            return false;
        }
        WasiVirtualFile wasiFile = (WasiVirtualFile) file;
        wasiFile.init();

        if (wasiFile.path == null || fileName == null) {
            return false;
        }

        init();
        if (baseFd < 0) {
            return false;
        }

        if (path == null) {
            return adopt(baseFd, wasiFile, fileName);
        } else {
            int fd = open(OFLAGS_DIRECTORY, RIGHTS_READ | RIGHTS_WRITE, (short) 0);
            try {
                return adopt(fd, wasiFile, fileName);
            } finally {
                close(fd);
            }
        }
    }

    @Override
    public boolean canRead() {
        init();
        int fd = open((short) 0, RIGHTS_READ, (short) 0);
        if (fd >= 0) {
            close(fd);
            return true;
        }
        return false;
    }

    @Override
    public boolean canWrite() {
        init();
        int fd = open((short) 0, RIGHTS_WRITE, (short) 0);
        if (fd >= 0) {
            close(fd);
            return true;
        }
        return false;
    }

    @Override
    public long lastModified() {
        Stat stat = stat();
        return stat == null ? 0 : stat.mtime / 1000000L;
    }

    @Override
    public boolean setLastModified(long lastModified) {
        init();
        if (baseFd < 0) {
            return false;
        }

        short errno;
        if (path == null) {
            errno = Wasi.fdFilestatSetTimes(baseFd, 0, lastModified * 1000000L, FSTFLAGS_MTIME);
        } else {
            byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
            errno = Wasi.pathFilestatSetTimes(baseFd, DIRFLAGS_FOLLOW_SYMLINKS, Address.ofData(bytes), bytes.length,
                    0, lastModified * 1000000L, FSTFLAGS_MTIME);
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

    private String constructPath(String parent, String child) {
        if (parent == null) {
            return child;
        }
        return !parent.isEmpty() && parent.charAt(parent.length() - 1) == '/'
                ? parent + child
                : parent + '/' + child;
    }

    static class Stat {
        final byte filetype;
        final long mtime;
        final long filesize;

        Stat(byte filetype, long mtime, long filesize) {
            this.filetype = filetype;
            this.mtime = mtime;
            this.filesize = filesize;
        }
    }
}