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

import static org.teavm.backend.wasm.wasi.Wasi.ERRNO_BADF;
import static org.teavm.backend.wasm.wasi.Wasi.ERRNO_SUCCESS;
import static org.teavm.backend.wasm.wasi.Wasi.PRESTAT_DIR;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.teavm.backend.wasm.wasi.Prestat;
import org.teavm.backend.wasm.wasi.PrestatDir;
import org.teavm.backend.wasm.wasi.Wasi;
import org.teavm.interop.Address;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileSystem;

public class WasiFileSystem implements VirtualFileSystem {
    private List<Preopened> preopenedList;
    final byte[] buffer = new byte[256];
    short errno;
    String bestPreopenedPath;
    int bestPreopenedId;

    @Override
    public String getUserDir() {
        return "/";
    }

    @Override
    public VirtualFile getFile(String path) {
        return new WasiVirtualFile(this, path);
    }

    void findBestPreopened(String path) {
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        List<Preopened> list = getPreopenedList();
        int bestFd = -1;
        int bestNameLength = -1;
        for (Preopened preopened : list) {
            if (path.equals(preopened.name)) {
                bestFd = preopened.fd;
                path = null;
                break;
            }

            int prefixLen = getPrefixLength(path, preopened.name);
            if (prefixLen > bestNameLength) {
                if (prefixLen >= 0) {
                    bestFd = preopened.fd;
                    bestNameLength = prefixLen;
                }
            }
        }

        if (bestFd == -1) {
            path = null;
        } else if (path != null) {
            path = path.substring(bestNameLength);
        }

        bestPreopenedPath = path;
        bestPreopenedId = bestFd;
    }

    private static int getPrefixLength(String name, String dir) {
        if (dir.equals("/") && name.startsWith("/")) {
            return 1;
        }
        return name.startsWith(dir) && name.length() > dir.length() && name.charAt(dir.length()) == '/'
                ? dir.length() + 1 : -1;
    }

    @Override
    public boolean isWindows() {
        return false;
    }

    @Override
    public String canonicalize(String path) {
        return path;
    }

    private List<Preopened> getPreopenedList() {
        if (preopenedList == null) {
            preopenedList = createPreopenedList();
        }
        return preopenedList;
    }

    private List<Preopened> createPreopenedList() {
        byte[] buffer = this.buffer;
        Prestat prestat = Address.align(Address.ofData(buffer), 4).toStructure();
        List<Preopened> list = new ArrayList<>();
        int fd = 3; // skip stdin, stdout, and stderr
        while (true) {
            short errno = Wasi.fdPrestatGet(fd, prestat);
            if (errno == ERRNO_BADF) {
                break;
            }
            if (errno != ERRNO_SUCCESS) {
                return Collections.emptyList();
            }

            if (prestat.kind == PRESTAT_DIR) {
                PrestatDir prestatDir = (PrestatDir) prestat;
                int length = prestatDir.nameLength;
                byte[] bytes = length <= buffer.length ? buffer : new byte[length];
                errno = Wasi.fdPrestatDirName(fd, Address.ofData(bytes), length);

                if (errno == ERRNO_SUCCESS && length > 0) {
                    if (bytes[length - 1] == 0) {
                        length -= 1;
                    }

                    list.add(new Preopened(fd, new String(bytes, 0, length, StandardCharsets.UTF_8)));
                } else {
                    return Collections.emptyList();
                }
            }

            fd += 1;
        }

        return list;
    }

    static class Preopened {
        final int fd;
        final String name;

        Preopened(int fd, String name) {
            this.fd = fd;
            this.name = name;
        }
    }
}