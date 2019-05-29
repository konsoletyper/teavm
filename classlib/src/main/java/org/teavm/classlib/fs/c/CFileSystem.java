/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.classlib.fs.c;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import org.teavm.classlib.fs.VirtualFile;
import org.teavm.classlib.fs.VirtualFileSystem;
import org.teavm.classlib.impl.c.Memory;
import org.teavm.classlib.impl.c.StringList;
import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.interop.Unmanaged;

public class CFileSystem implements VirtualFileSystem {
    private Map<String, Entry> cache = new HashMap<>();
    private ReferenceQueue<? super CVirtualFile> referenceQueue = new ReferenceQueue<>();

    @Override
    public String getUserDir() {
        Address resultPtr = Memory.malloc(Address.sizeOf());
        int length = workDirectory(resultPtr);
        Address result = resultPtr.getAddress();
        Memory.free(resultPtr);

        int realLength = length > 0 ? length : -length - 1;
        char[] chars = new char[realLength];
        Memory.memcpy(Address.ofData(chars), result, chars.length * 2);
        Memory.free(result);

        String s = new String(chars);
        if (length < 0) {
            throw new RuntimeException(s);
        } else {
            return s;
        }
    }

    @Override
    public VirtualFile getFile(String path) {
        return getByPath(path);
    }

    CVirtualFile getByPath(String path) {
        Entry entry = cache.get(path);
        if (entry == null || entry.get() == null) {
            entry = new Entry(new CVirtualFile(this, path), referenceQueue);
            cache.put(path, entry);
        }
        while (true) {
            Entry staleEntry = (Entry) referenceQueue.poll();
            if (staleEntry == null) {
                break;
            }
            if (!staleEntry.path.equals(path)) {
                cache.remove(staleEntry.path);
            }
        }
        return entry.get();
    }

    static class Entry extends WeakReference<CVirtualFile> {
        String path;

        Entry(CVirtualFile referent, ReferenceQueue<? super CVirtualFile> q) {
            super(referent, q);
            this.path = referent.path;
        }
    }

    @Import(name = "teavm_file_homeDirectory")
    @Unmanaged
    public static native int homeDirectory(Address resultPtr);

    @Import(name = "teavm_file_workDirectory")
    @Unmanaged
    static native int workDirectory(Address resultPtr);

    @Import(name = "teavm_file_isDir")
    @Unmanaged
    static native boolean isDir(char[] name, int nameSize);

    @Import(name = "teavm_file_isFile")
    @Unmanaged
    static native boolean isFile(char[] name, int nameSize);

    @Import(name = "teavm_file_canRead")
    @Unmanaged
    static native boolean canRead(char[] name, int nameSize);

    @Import(name = "teavm_file_canWrite")
    @Unmanaged
    static native boolean canWrite(char[] name, int nameSize);

    @Import(name = "teavm_file_setReadonly")
    @Unmanaged
    static native boolean setReadonly(char[] name, int nameSize, boolean readonly);

    @Import(name = "teavm_file_listFiles")
    @Unmanaged
    static native StringList listFiles(char[] name, int nameSize);

    @Import(name = "teavm_file_createDirectory")
    @Unmanaged
    static native boolean createDirectory(char[] name, int nameSize);

    @Import(name = "teavm_file_createFile")
    @Unmanaged
    static native int createFile(char[] name, int nameSize);

    @Import(name = "teavm_file_delete")
    @Unmanaged
    static native boolean delete(char[] name, int nameSize);

    @Import(name = "teavm_file_rename")
    @Unmanaged
    static native boolean rename(char[] name, int nameSize, char[] newName, int newNameSize);

    @Import(name = "teavm_file_length")
    @Unmanaged
    static native int length(char[] name, int nameSize);

    @Import(name = "teavm_file_lastModified")
    @Unmanaged
    static native long lastModified(char[] name, int nameSize);

    @Import(name = "teavm_file_setLastModified")
    @Unmanaged
    static native boolean setLastModified(char[] name, int nameSize, long lastModified);

    @Import(name = "teavm_file_open")
    @Unmanaged
    static native long open(char[] name, int nameSize, int mode);

    @Import(name = "teavm_file_close")
    @Unmanaged
    static native boolean close(long file);

    @Import(name = "teavm_file_flush")
    @Unmanaged
    static native boolean flush(long file);

    @Import(name = "teavm_file_seek")
    @Unmanaged
    static native boolean seek(long file, int where, int offset);

    @Import(name = "teavm_file_tell")
    @Unmanaged
    static native int tell(long file);

    @Import(name = "teavm_file_read")
    @Unmanaged
    static native int read(long file, byte[] data, int offset, int count);

    @Import(name = "teavm_file_write")
    @Unmanaged
    static native int write(long file, byte[] data, int offset, int count);
}
