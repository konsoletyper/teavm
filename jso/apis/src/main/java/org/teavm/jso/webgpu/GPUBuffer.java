package org.teavm.jso.webgpu;

import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.typedarrays.ArrayBuffer;

public interface GPUBuffer extends GPU.ObjectBase {
    @JSProperty
    double getSize();

    @JSProperty
    int getUsage();

    @JSProperty
    String getMapState();

    JSPromise<JSUndefined> mapAsync(int mode);

    JSPromise<JSUndefined> mapAsync(int mode, double offset);

    JSPromise<JSUndefined> mapAsync(int mode, double offset, double size);

    ArrayBuffer getMappedRange();

    ArrayBuffer getMappedRange(double offset);

    ArrayBuffer getMappedRange(double offset, double size);

    void unmap();

    void destroy();

    final class Usage {
        public static final int MAP_READ = 0x0001;
        public static final int MAP_WRITE = 0x0002;
        public static final int COPY_SRC = 0x0004;
        public static final int COPY_DST = 0x0008;
        public static final int INDEX = 0x0010;
        public static final int VERTEX = 0x0020;
        public static final int UNIFORM = 0x0040;
        public static final int STORAGE = 0x0080;
        public static final int INDIRECT = 0x0100;
        public static final int QUERY_RESOLVE = 0x0200;

        private Usage() {
            // le ✨stub✨
        }
    }

    final class MapMode {
        public static final int READ = 0x1;
        public static final int WRITE = 0x2;

        private MapMode() {
            // le ✨stub✨
        }
    }
}

