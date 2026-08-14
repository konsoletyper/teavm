package org.teavm.jso.webgpu;

import org.teavm.jso.JSProperty;

public interface GPUTexture extends GPU.ObjectBase {
    View createView();

    View createView(GPUDescriptor.TextureView descriptor);

    void destroy();

    @JSProperty
    int getWidth();

    @JSProperty
    int getHeight();

    @JSProperty
    int getDepthOrArrayLayers();

    @JSProperty
    int getMipLevelCount();

    @JSProperty
    int getSampleCount();

    @JSProperty
    String getDimension();

    @JSProperty
    String getFormat();

    @JSProperty
    int getUsage();

    interface View extends GPU.ObjectBase {
    }

    final class Usage {
        public static final int COPY_SRC = 0x01;
        public static final int COPY_DST = 0x02;
        public static final int TEXTURE_BINDING = 0x04;
        public static final int STORAGE_BINDING = 0x08;
        public static final int RENDER_ATTACHMENT = 0x10;
        public static final int TRANSIENT_ATTACHMENT = 0x20;

        private Usage() {
        }
    }

    final class Format {
        public static final String R8UNORM = "r8unorm";
        public static final String R8SNORM = "r8snorm";
        public static final String R8UINT = "r8uint";
        public static final String R8SINT = "r8sint";
        public static final String R16UINT = "r16uint";
        public static final String R16SINT = "r16sint";
        public static final String R16FLOAT = "r16float";
        public static final String RG8UNORM = "rg8unorm";
        public static final String RG8SNORM = "rg8snorm";
        public static final String RG8UINT = "rg8uint";
        public static final String RG8SINT = "rg8sint";
        public static final String R32UINT = "r32uint";
        public static final String R32SINT = "r32sint";
        public static final String R32FLOAT = "r32float";
        public static final String RG16UINT = "rg16uint";
        public static final String RG16SINT = "rg16sint";
        public static final String RG16FLOAT = "rg16float";
        public static final String RGBA8UNORM = "rgba8unorm";
        public static final String RGBA8UNORM_SRGB = "rgba8unorm-srgb";
        public static final String RGBA8SNORM = "rgba8snorm";
        public static final String RGBA8UINT = "rgba8uint";
        public static final String RGBA8SINT = "rgba8sint";
        public static final String BGRA8UNORM = "bgra8unorm";
        public static final String BGRA8UNORM_SRGB = "bgra8unorm-srgb";
        public static final String RGB9E5UFLOAT = "rgb9e5ufloat";
        public static final String RGB10A2UINT = "rgb10a2uint";
        public static final String RGB10A2UNORM = "rgb10a2unorm";
        public static final String RG11B10UFLOAT = "rg11b10ufloat";
        public static final String RG32UINT = "rg32uint";
        public static final String RG32SINT = "rg32sint";
        public static final String RG32FLOAT = "rg32float";
        public static final String RGBA16UINT = "rgba16uint";
        public static final String RGBA16SINT = "rgba16sint";
        public static final String RGBA16FLOAT = "rgba16float";
        public static final String RGBA32UINT = "rgba32uint";
        public static final String RGBA32SINT = "rgba32sint";
        public static final String RGBA32FLOAT = "rgba32float";
        public static final String STENCIL8 = "stencil8";
        public static final String DEPTH16UNORM = "depth16unorm";
        public static final String DEPTH24PLUS = "depth24plus";
        public static final String DEPTH24PLUS_STENCIL8 = "depth24plus-stencil8";
        public static final String DEPTH32FLOAT = "depth32float";
        public static final String DEPTH32FLOAT_STENCIL8 = "depth32float-stencil8";

        private Format() {
            // le ✨stub✨
        }
    }
}

