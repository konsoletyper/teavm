/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import static org.teavm.interop.wasi.Wasi.CLOCKID_REALTIME;
import static org.teavm.interop.wasi.Wasi.ERRNO_SUCCESS;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import org.teavm.backend.c.intrinsic.RuntimeInclude;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.fs.VirtualFileSystemProvider;
import org.teavm.classlib.fs.c.CFileSystem;
import org.teavm.classlib.impl.c.Memory;
import org.teavm.classlib.impl.console.StderrOutputStream;
import org.teavm.classlib.impl.console.StdoutOutputStream;
import org.teavm.classlib.java.io.TConsole;
import org.teavm.classlib.java.io.TFileInputStream;
import org.teavm.classlib.java.io.TFileOutputStream;
import org.teavm.classlib.java.io.TInputStream;
import org.teavm.classlib.java.io.TOutputStream;
import org.teavm.classlib.java.io.TPrintStream;
import org.teavm.classlib.java.lang.reflect.TArray;
import org.teavm.interop.Address;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.Import;
import org.teavm.interop.NoSideEffects;
import org.teavm.interop.Unmanaged;
import org.teavm.interop.wasi.Wasi;
import org.teavm.interop.wasi.Wasi.ErrnoException;
import org.teavm.jso.browser.Performance;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.GC;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;

public final class TSystem extends TObject {
    // Enough room for an I64 plus padding for alignment:
    private static final byte[] SIXTEEN_BYTE_BUFFER = new byte[16];

    private static HashMap<String, String> envMap;

    private static TPrintStream outCache;
    private static TPrintStream errCache;
    private static TInputStream inCache;
    private static Properties properties;

    private TSystem() {
    }

    public static TPrintStream out() {
        if (outCache == null) {
            if (PlatformDetector.isWebAssembly()) {
                outCache = new TPrintStream((TOutputStream) (Object) new TFileOutputStream(1));
            } else {
                outCache = new TPrintStream((TOutputStream) (Object) StdoutOutputStream.INSTANCE, false);
            }
        }
        return outCache;
    }

    public static TPrintStream err() {
        if (errCache == null) {
            if (PlatformDetector.isWebAssembly()) {
                errCache = new TPrintStream((TOutputStream) (Object) new TFileOutputStream(2));
            } else {
                errCache = new TPrintStream((TOutputStream) (Object) StderrOutputStream.INSTANCE, false);
            }
        }
        return errCache;
    }

    public static TInputStream in() {
        if (inCache == null) {
            if (PlatformDetector.isWebAssembly()) {
                inCache = (TInputStream) (Object) new TFileInputStream(0);
            } else {
                inCache = new TConsoleInputStream();
            }
        }
        return inCache;
    }

    public static TConsole console() {
        return null;
    }

    public static TSecurityManager getSecurityManager() {
        return new TSecurityManager();
    }

    public static void arraycopy(TObject src, int srcPos, TObject dest, int destPos, int length) {
        if (src == null || dest == null) {
            throw new TNullPointerException("Either src or dest is null");
        }
        if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > TArray.getLength(src)
                || destPos + length > TArray.getLength(dest)) {
            throw new TIndexOutOfBoundsException();
        }
        if (src != dest) {
            Class<?> srcType = src.getClass().getComponentType();
            Class<?> targetType = dest.getClass().getComponentType();
            if (srcType == null || targetType == null) {
                throw new TArrayStoreException();
            }
            if (srcType != targetType) {
                if (!srcType.isPrimitive() && !targetType.isPrimitive()) {
                    Object[] srcArray = (Object[]) (Object) src;
                    int pos = srcPos;
                    for (int i = 0; i < length; ++i) {
                        Object elem = srcArray[pos++];
                        if (!targetType.isInstance(elem)) {
                            doArrayCopy(src, srcPos, dest, destPos, i);
                            throw new TArrayStoreException();
                        }
                    }
                    doArrayCopy(src, srcPos, dest, destPos, length);
                    return;
                } else if (!srcType.isPrimitive() || !targetType.isPrimitive()) {
                    throw new TArrayStoreException();
                }
            }
        }
        doArrayCopy(src, srcPos, dest, destPos, length);
    }

    @GeneratedBy(SystemNativeGenerator.class)
    @DelegateTo("doArrayCopyLowLevel")
    @NoSideEffects
    private static native void doArrayCopy(Object src, int srcPos, Object dest, int destPos, int length);

    @Unmanaged
    static void doArrayCopyLowLevel(RuntimeArray src, int srcPos, RuntimeArray dest, int destPos, int length) {
        RuntimeClass type = RuntimeClass.getClass(src);
        int itemSize = type.itemType.size;
        if ((type.itemType.flags & RuntimeClass.PRIMITIVE) == 0) {
            itemSize = Address.sizeOf();
            GC.writeBarrier(dest);
        }

        Address srcAddress = Address.align(src.toAddress().add(RuntimeArray.class, 1), itemSize);
        srcAddress = srcAddress.add(itemSize * srcPos);

        Address destAddress = Address.align(dest.toAddress().add(RuntimeArray.class, 1), itemSize);
        destAddress = destAddress.add(itemSize * destPos);

        Allocator.moveMemoryBlock(srcAddress, destAddress, length * itemSize);
    }

    @DelegateTo("currentTimeMillisLowLevel")
    @GeneratedBy(SystemNativeGenerator.class)
    @NoSideEffects
    public static native long currentTimeMillis();

    private static long currentTimeMillisLowLevel() {
        if (PlatformDetector.isWebAssembly()) {
            return currentTimeMillisWasi();
        } else {
            return (long) currentTimeMillisC();
        }
    }

    private static long currentTimeMillisWasi() {
        return nanoTimeWasi() / 1000000;
    }

    @Import(name = "teavm_currentTimeMillis")
    @RuntimeInclude("time.h")
    private static native long currentTimeMillisC();

    private static void initPropertiesIfNeeded() {
        if (properties == null) {
            Properties defaults = new Properties();
            defaults.put("java.version", "1.8");
            defaults.put("os.name", "TeaVM");
            defaults.put("file.separator", "/");
            defaults.put("path.separator", ":");
            defaults.put("line.separator", lineSeparator());
            defaults.put("java.io.tmpdir", getTempDir());
            defaults.put("java.vm.version", "1.8");
            defaults.put("user.home", getHomeDir());
            properties = new Properties(defaults);
        }
    }

    private static String getTempDir() {
        if (!PlatformDetector.isC()) {
            return "/tmp";
        }
        Address resultPtr = Memory.malloc(Address.sizeOf());
        int length = CFileSystem.tempDirectory(resultPtr);
        return VirtualFileSystemProvider.getInstance().canonicalize(toJavaString(resultPtr, length));
    }

    private static String getHomeDir() {
        if (!PlatformDetector.isC()) {
            return "/";
        }

        Address resultPtr = Memory.malloc(Address.sizeOf());
        int length = CFileSystem.homeDirectory(resultPtr);
        return VirtualFileSystemProvider.getInstance().canonicalize(toJavaString(resultPtr, length));
    }

    private static String toJavaString(Address resultPtr, int length) {
        Address result = resultPtr.getAddress();
        Memory.free(resultPtr);

        char[] chars = new char[length];
        Memory.memcpy(Address.ofData(chars), result, chars.length * 2);
        Memory.free(result);

        return new String(chars);
    }

    public static String getProperty(@SuppressWarnings("unused") String key) {
        initPropertiesIfNeeded();
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String def) {
        String value = getProperty(key);
        return value != null ? value : def;
    }

    public static Properties getProperties() {
        initPropertiesIfNeeded();
        Properties result = new Properties();
        copyProperties(properties, result);
        return result;
    }

    public static void setProperties(Properties props) {
        initPropertiesIfNeeded();
        copyProperties(props, properties);
    }

    private static void copyProperties(Properties from, Properties to) {
        to.clear();
        if (from != null) {
            Enumeration<?> e = from.propertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                to.setProperty(key, from.getProperty(key));
            }
        }
    }

    public static String setProperty(String key, String value) {
        initPropertiesIfNeeded();
        return (String) properties.put(key, value);
    }

    public static String clearProperty(String key) {
        return (String) properties.remove(key);
    }

    public static void setErr(TPrintStream err) {
        errCache = err;
    }

    public static void setOut(TPrintStream out) {
        outCache = out;
    }

    @DelegateTo("gcLowLevel")
    public static void gc() {
        // Do nothing
    }

    private static void gcLowLevel() {
        GC.collectGarbageFull();
    }

    public static void runFinalization() {
        // Do nothing
    }

    public static long nanoTime() {
        if (PlatformDetector.isWebAssembly()) {
            return (long) nanoTimeWasi();
        } else if (PlatformDetector.isLowLevel()) {
            return nanoTimeLowLevel();
        } else {
            return (long) (Performance.now() * 1000000);
        }
    }

    private static long nanoTimeWasi() {
        byte[] timestampBuffer = SIXTEEN_BYTE_BUFFER;
        Address timestamp = Address.align(Address.ofData(timestampBuffer), 8);
        short errno = Wasi.clockTimeGet(CLOCKID_REALTIME, 10, timestamp);

        if (errno == ERRNO_SUCCESS) {
            return timestamp.getLong();
        } else {
            throw new ErrnoException("clock_time_get", errno);
        }
    }

    @Import(name = "teavm_currentTimeNano")
    @RuntimeInclude("time.h")
    private static native long nanoTimeLowLevel();

    public static int identityHashCode(Object x) {
        return ((TObject) x).identity();
    }

    public static String lineSeparator() {
        return "\n";
    }

    public static String getenv(String name) {
        if (PlatformDetector.isWebAssembly()) {
            return getEnvWasi(name);
        } else {
            return null;
        }
    }

    private static String getEnvWasi(String name) {
        if (envMap == null) {
            envMap = new HashMap<>();
            byte[] sizesBuffer = SIXTEEN_BYTE_BUFFER;
            Address sizes = Address.align(Address.ofData(sizesBuffer), 4);
            short errno = Wasi.environSizesGet(sizes, sizes.add(4));

            if (errno == ERRNO_SUCCESS) {
                int environSize = sizes.getInt();
                int environBufSize = sizes.add(4).getInt();

                byte[] environBuffer = new byte[(environSize * 4) + 4];
                Address environ = Address.align(Address.ofData(environBuffer), 4);
                byte[] environBuf = new byte[environBufSize];
                errno = Wasi.environGet(environ, Address.ofData(environBuf));

                if (errno == ERRNO_SUCCESS) {
                    for (int i = 0; i < environSize; ++i) {
                        int offset = environ.add(i * 4).getInt() - Address.ofData(environBuf).toInt();
                        int length = (i == environSize - 1
                                      ? environBufSize
                                      : (environ.add((i + 1) * 4).getInt() - Address.ofData(environBuf).toInt()))
                            - 1 - offset;

                        // TODO: this is probably not guaranteed to be UTF-8:
                        String var = new String(environBuf, offset, length, StandardCharsets.UTF_8);
                        int index = var.indexOf('=');
                        if (index != -1) {
                            envMap.put(var.substring(0, index), var.substring(index + 1));
                        }
                    }
                } else {
                    throw new ErrnoException("environ_get", errno);
                }
            } else {
                throw new ErrnoException("environ_sizes_get", errno);
            }
        }

        return envMap.get(name);
    }
}
