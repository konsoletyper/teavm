/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.jso.webgl;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Uint32Array;

public interface WebGL2RenderingContext extends WebGLRenderingContext {
    int READ_BUFFER = 0x0C02;
    int UNPACK_ROW_LENGTH = 0x0CF2;
    int UNPACK_SKIP_ROWS = 0x0CF3;
    int UNPACK_SKIP_PIXELS = 0x0CF4;
    int PACK_ROW_LENGTH = 0x0D02;
    int PACK_SKIP_ROWS = 0x0D03;
    int PACK_SKIP_PIXELS = 0x0D04;
    int COLOR = 0x1800;
    int DEPTH = 0x1801;
    int STENCIL = 0x1802;
    int RED = 0x1903;
    int RGB8 = 0x8051;
    int RGB10_A2 = 0x8059;
    int TEXTURE_BINDING_3D = 0x806A;
    int UNPACK_SKIP_IMAGES = 0x806D;
    int UNPACK_IMAGE_HEIGHT = 0x806E;
    int TEXTURE_3D = 0x806F;
    int TEXTURE_WRAP_R = 0x8072;
    int MAX_3D_TEXTURE_SIZE = 0x8073;
    int UNSIGNED_INT_2_10_10_10_REV = 0x8368;
    int MAX_ELEMENTS_VERTICES = 0x80E8;
    int MAX_ELEMENTS_INDICES = 0x80E9;
    int TEXTURE_MIN_LOD = 0x813A;
    int TEXTURE_MAX_LOD = 0x813B;
    int TEXTURE_BASE_LEVEL = 0x813C;
    int TEXTURE_MAX_LEVEL = 0x813D;
    int MIN = 0x8007;
    int MAX = 0x8008;
    int DEPTH_COMPONENT24 = 0x81A6;
    int MAX_TEXTURE_LOD_BIAS = 0x84FD;
    int TEXTURE_COMPARE_MODE = 0x884C;
    int TEXTURE_COMPARE_FUNC = 0x884D;
    int CURRENT_QUERY = 0x8865;
    int QUERY_RESULT = 0x8866;
    int QUERY_RESULT_AVAILABLE = 0x8867;
    int STREAM_READ = 0x88E1;
    int STREAM_COPY = 0x88E2;
    int STATIC_READ = 0x88E5;
    int STATIC_COPY = 0x88E6;
    int DYNAMIC_READ = 0x88E9;
    int DYNAMIC_COPY = 0x88EA;
    int MAX_DRAW_BUFFERS = 0x8824;
    int DRAW_BUFFER0 = 0x8825;
    int DRAW_BUFFER1 = 0x8826;
    int DRAW_BUFFER2 = 0x8827;
    int DRAW_BUFFER3 = 0x8828;
    int DRAW_BUFFER4 = 0x8829;
    int DRAW_BUFFER5 = 0x882A;
    int DRAW_BUFFER6 = 0x882B;
    int DRAW_BUFFER7 = 0x882C;
    int DRAW_BUFFER8 = 0x882D;
    int DRAW_BUFFER9 = 0x882E;
    int DRAW_BUFFER10 = 0x882F;
    int DRAW_BUFFER11 = 0x8830;
    int DRAW_BUFFER12 = 0x8831;
    int DRAW_BUFFER13 = 0x8832;
    int DRAW_BUFFER14 = 0x8833;
    int DRAW_BUFFER15 = 0x8834;
    int MAX_FRAGMENT_UNIFORM_COMPONENTS = 0x8B49;
    int MAX_VERTEX_UNIFORM_COMPONENTS = 0x8B4A;
    int SAMPLER_3D = 0x8B5F;
    int SAMPLER_2D_SHADOW = 0x8B62;
    int FRAGMENT_SHADER_DERIVATIVE_HINT = 0x8B8B;
    int PIXEL_PACK_BUFFER = 0x88EB;
    int PIXEL_UNPACK_BUFFER = 0x88EC;
    int PIXEL_PACK_BUFFER_BINDING = 0x88ED;
    int PIXEL_UNPACK_BUFFER_BINDING = 0x88EF;
    int FLOAT_MAT2x3 = 0x8B65;
    int FLOAT_MAT2x4 = 0x8B66;
    int FLOAT_MAT3x2 = 0x8B67;
    int FLOAT_MAT3x4 = 0x8B68;
    int FLOAT_MAT4x2 = 0x8B69;
    int FLOAT_MAT4x3 = 0x8B6A;
    int SRGB = 0x8C40;
    int SRGB8 = 0x8C41;
    int SRGB8_ALPHA8 = 0x8C43;
    int COMPARE_REF_TO_TEXTURE = 0x884E;
    int RGBA32F = 0x8814;
    int RGB32F = 0x8815;
    int RGBA16F = 0x881A;
    int RGB16F = 0x881B;
    int VERTEX_ATTRIB_ARRAY_INTEGER = 0x88FD;
    int MAX_ARRAY_TEXTURE_LAYERS = 0x88FF;
    int MIN_PROGRAM_TEXEL_OFFSET = 0x8904;
    int MAX_PROGRAM_TEXEL_OFFSET = 0x8905;
    int MAX_VARYING_COMPONENTS = 0x8B4B;
    int TEXTURE_2D_ARRAY = 0x8C1A;
    int TEXTURE_BINDING_2D_ARRAY = 0x8C1D;
    int R11F_G11F_B10F = 0x8C3A;
    int UNSIGNED_INT_10F_11F_11F_REV = 0x8C3B;
    int RGB9_E5 = 0x8C3D;
    int UNSIGNED_INT_5_9_9_9_REV = 0x8C3E;
    int TRANSFORM_FEEDBACK_BUFFER_MODE = 0x8C7F;
    int MAX_TRANSFORM_FEEDBACK_SEPARATE_COMPONENTS = 0x8C80;
    int TRANSFORM_FEEDBACK_VARYINGS = 0x8C83;
    int TRANSFORM_FEEDBACK_BUFFER_START = 0x8C84;
    int TRANSFORM_FEEDBACK_BUFFER_SIZE = 0x8C85;
    int TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN = 0x8C88;
    int RASTERIZER_DISCARD = 0x8C89;
    int MAX_TRANSFORM_FEEDBACK_INTERLEAVED_COMPONENTS = 0x8C8A;
    int MAX_TRANSFORM_FEEDBACK_SEPARATE_ATTRIBS = 0x8C8B;
    int INTERLEAVED_ATTRIBS = 0x8C8C;
    int SEPARATE_ATTRIBS = 0x8C8D;
    int TRANSFORM_FEEDBACK_BUFFER = 0x8C8E;
    int TRANSFORM_FEEDBACK_BUFFER_BINDING = 0x8C8F;
    int RGBA32UI = 0x8D70;
    int RGB32UI = 0x8D71;
    int RGBA16UI = 0x8D76;
    int RGB16UI = 0x8D77;
    int RGBA8UI = 0x8D7C;
    int RGB8UI = 0x8D7D;
    int RGBA32I = 0x8D82;
    int RGB32I = 0x8D83;
    int RGBA16I = 0x8D88;
    int RGB16I = 0x8D89;
    int RGBA8I = 0x8D8E;
    int RGB8I = 0x8D8F;
    int RED_INTEGER = 0x8D94;
    int RGB_INTEGER = 0x8D98;
    int RGBA_INTEGER = 0x8D99;
    int SAMPLER_2D_ARRAY = 0x8DC1;
    int SAMPLER_2D_ARRAY_SHADOW = 0x8DC4;
    int SAMPLER_CUBE_SHADOW = 0x8DC5;
    int UNSIGNED_INT_VEC2 = 0x8DC6;
    int UNSIGNED_INT_VEC3 = 0x8DC7;
    int UNSIGNED_INT_VEC4 = 0x8DC8;
    int INT_SAMPLER_2D = 0x8DCA;
    int INT_SAMPLER_3D = 0x8DCB;
    int INT_SAMPLER_CUBE = 0x8DCC;
    int INT_SAMPLER_2D_ARRAY = 0x8DCF;
    int UNSIGNED_INT_SAMPLER_2D = 0x8DD2;
    int UNSIGNED_INT_SAMPLER_3D = 0x8DD3;
    int UNSIGNED_INT_SAMPLER_CUBE = 0x8DD4;
    int UNSIGNED_INT_SAMPLER_2D_ARRAY = 0x8DD7;
    int DEPTH_COMPONENT32F = 0x8CAC;
    int DEPTH32F_STENCIL8 = 0x8CAD;
    int FLOAT_32_UNSIGNED_INT_24_8_REV = 0x8DAD;
    int FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING = 0x8210;
    int FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE = 0x8211;
    int FRAMEBUFFER_ATTACHMENT_RED_SIZE = 0x8212;
    int FRAMEBUFFER_ATTACHMENT_GREEN_SIZE = 0x8213;
    int FRAMEBUFFER_ATTACHMENT_BLUE_SIZE = 0x8214;
    int FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE = 0x8215;
    int FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE = 0x8216;
    int FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE = 0x8217;
    int FRAMEBUFFER_DEFAULT = 0x8218;
    int UNSIGNED_INT_24_8 = 0x84FA;
    int DEPTH24_STENCIL8 = 0x88F0;
    int UNSIGNED_NORMALIZED = 0x8C17;
    int DRAW_FRAMEBUFFER_BINDING = 0x8CA6; /* Same as FRAMEBUFFER_BINDING */
    int READ_FRAMEBUFFER = 0x8CA8;
    int DRAW_FRAMEBUFFER = 0x8CA9;
    int READ_FRAMEBUFFER_BINDING = 0x8CAA;
    int RENDERBUFFER_SAMPLES = 0x8CAB;
    int FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER = 0x8CD4;
    int MAX_COLOR_ATTACHMENTS = 0x8CDF;
    int COLOR_ATTACHMENT1 = 0x8CE1;
    int COLOR_ATTACHMENT2 = 0x8CE2;
    int COLOR_ATTACHMENT3 = 0x8CE3;
    int COLOR_ATTACHMENT4 = 0x8CE4;
    int COLOR_ATTACHMENT5 = 0x8CE5;
    int COLOR_ATTACHMENT6 = 0x8CE6;
    int COLOR_ATTACHMENT7 = 0x8CE7;
    int COLOR_ATTACHMENT8 = 0x8CE8;
    int COLOR_ATTACHMENT9 = 0x8CE9;
    int COLOR_ATTACHMENT10 = 0x8CEA;
    int COLOR_ATTACHMENT11 = 0x8CEB;
    int COLOR_ATTACHMENT12 = 0x8CEC;
    int COLOR_ATTACHMENT13 = 0x8CED;
    int COLOR_ATTACHMENT14 = 0x8CEE;
    int COLOR_ATTACHMENT15 = 0x8CEF;
    int FRAMEBUFFER_INCOMPLETE_MULTISAMPLE = 0x8D56;
    int MAX_SAMPLES = 0x8D57;
    int HALF_FLOAT = 0x140B;
    int RG = 0x8227;
    int RG_INTEGER = 0x8228;
    int R8 = 0x8229;
    int RG8 = 0x822B;
    int R16F = 0x822D;
    int R32F = 0x822E;
    int RG16F = 0x822F;
    int RG32F = 0x8230;
    int R8I = 0x8231;
    int R8UI = 0x8232;
    int R16I = 0x8233;
    int R16UI = 0x8234;
    int R32I = 0x8235;
    int R32UI = 0x8236;
    int RG8I = 0x8237;
    int RG8UI = 0x8238;
    int RG16I = 0x8239;
    int RG16UI = 0x823A;
    int RG32I = 0x823B;
    int RG32UI = 0x823C;
    int VERTEX_ARRAY_BINDING = 0x85B5;
    int R8_SNORM = 0x8F94;
    int RG8_SNORM = 0x8F95;
    int RGB8_SNORM = 0x8F96;
    int RGBA8_SNORM = 0x8F97;
    int SIGNED_NORMALIZED = 0x8F9C;
    int COPY_READ_BUFFER = 0x8F36;
    int COPY_WRITE_BUFFER = 0x8F37;
    int COPY_READ_BUFFER_BINDING = 0x8F36; /* Same as COPY_READ_BUFFER */
    int COPY_WRITE_BUFFER_BINDING = 0x8F37; /* Same as COPY_WRITE_BUFFER */
    int UNIFORM_BUFFER = 0x8A11;
    int UNIFORM_BUFFER_BINDING = 0x8A28;
    int UNIFORM_BUFFER_START = 0x8A29;
    int UNIFORM_BUFFER_SIZE = 0x8A2A;
    int MAX_VERTEX_UNIFORM_BLOCKS = 0x8A2B;
    int MAX_FRAGMENT_UNIFORM_BLOCKS = 0x8A2D;
    int MAX_COMBINED_UNIFORM_BLOCKS = 0x8A2E;
    int MAX_UNIFORM_BUFFER_BINDINGS = 0x8A2F;
    int MAX_UNIFORM_BLOCK_SIZE = 0x8A30;
    int MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS = 0x8A31;
    int MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS = 0x8A33;
    int UNIFORM_BUFFER_OFFSET_ALIGNMENT = 0x8A34;
    int ACTIVE_UNIFORM_BLOCKS = 0x8A36;
    int UNIFORM_TYPE = 0x8A37;
    int UNIFORM_SIZE = 0x8A38;
    int UNIFORM_BLOCK_INDEX = 0x8A3A;
    int UNIFORM_OFFSET = 0x8A3B;
    int UNIFORM_ARRAY_STRIDE = 0x8A3C;
    int UNIFORM_MATRIX_STRIDE = 0x8A3D;
    int UNIFORM_IS_ROW_MAJOR = 0x8A3E;
    int UNIFORM_BLOCK_BINDING = 0x8A3F;
    int UNIFORM_BLOCK_DATA_SIZE = 0x8A40;
    int UNIFORM_BLOCK_ACTIVE_UNIFORMS = 0x8A42;
    int UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES = 0x8A43;
    int UNIFORM_BLOCK_REFERENCED_BY_VERTEX_SHADER = 0x8A44;
    int UNIFORM_BLOCK_REFERENCED_BY_FRAGMENT_SHADER = 0x8A46;
    int INVALID_INDEX = 0xFFFFFFFF;
    int MAX_VERTEX_OUTPUT_COMPONENTS = 0x9122;
    int MAX_FRAGMENT_INPUT_COMPONENTS = 0x9125;
    int MAX_SERVER_WAIT_TIMEOUT = 0x9111;
    int OBJECT_TYPE = 0x9112;
    int SYNC_CONDITION = 0x9113;
    int SYNC_STATUS = 0x9114;
    int SYNC_FLAGS = 0x9115;
    int SYNC_FENCE = 0x9116;
    int SYNC_GPU_COMMANDS_COMPLETE = 0x9117;
    int UNSIGNALED = 0x9118;
    int SIGNALED = 0x9119;
    int ALREADY_SIGNALED = 0x911A;
    int TIMEOUT_EXPIRED = 0x911B;
    int CONDITION_SATISFIED = 0x911C;
    int WAIT_FAILED = 0x911D;
    int SYNC_FLUSH_COMMANDS_BIT = 0x00000001;
    int VERTEX_ATTRIB_ARRAY_DIVISOR = 0x88FE;
    int ANY_SAMPLES_PASSED = 0x8C2F;
    int ANY_SAMPLES_PASSED_CONSERVATIVE = 0x8D6A;
    int SAMPLER_BINDING = 0x8919;
    int RGB10_A2UI = 0x906F;
    int INT_2_10_10_10_REV = 0x8D9F;
    int TRANSFORM_FEEDBACK = 0x8E22;
    int TRANSFORM_FEEDBACK_PAUSED = 0x8E23;
    int TRANSFORM_FEEDBACK_ACTIVE = 0x8E24;
    int TRANSFORM_FEEDBACK_BINDING = 0x8E25;
    int TEXTURE_IMMUTABLE_FORMAT = 0x912F;
    int MAX_ELEMENT_INDEX = 0x8D6B;
    int TEXTURE_IMMUTABLE_LEVELS = 0x82DF;

    int TIMEOUT_IGNORED = -1;

    int MAX_CLIENT_WAIT_TIMEOUT_WEBGL = 0x9247;

    void copyBufferSubData(int readTarget, int writeTarget, int readOffset, int writeOffset, int size);

    void getBufferSubData(int target, int srcByteOffset, ArrayBufferView dstBuffer, int dstOffset, int length);

    void getBufferSubData(int target, int srcByteOffset, ArrayBufferView dstBuffer);

    void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1,
            int mask, int filter);

    void framebufferTextureLayer(int target, int attachment, WebGLTexture texture, int level, int layer);

    void invalidateFramebuffer(int target, JSArrayReader<JSNumber> attachments);

    void invalidateFramebuffer(int target, Int32Array attachments);

    void invalidateFramebuffer(int target, @JSByRef(optional = true) int[] attachments);

    void invalidateSubFramebuffer(int target, JSArrayReader<JSNumber> attachments, int x, int y, int width, int height);

    void invalidateSubFramebuffer(int target, Int32Array attachments, int x, int y, int width, int height);

    void invalidateSubFramebuffer(int target, @JSByRef(optional = true) int[] attachments, int x, int y,
            int width, int height);

    void readBuffer(int src);

    JSObject getInternalformatParameter(int target, int internalformat, int pname);

    void renderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height);

    void texStorage2D(int target, int levels, int internalformat, int width, int height);
    void texStorage3D(int target, int levels, int internalformat, int width, int height, int depth);

    void texImage3D(int target, int level, int internalformat, int width, int height,
            int depth, int border, int format, int type, int pboOffset);

    void texImage3D(int target, int level, int internalformat, int width, int height,
            int depth, int border, int format, int type, ArrayBufferView source);

    void texImage3D(int target, int level, int internalformat, int width, int height,
            int depth, int border, int format, int type, ArrayBufferView source, int srcOffset);

    void texImage3D(int target, int level, int internalformat, int width, int height,
            int depth, int border, int format, int type, Buffer source);

    void texImage3D(int target, int level, int internalformat, int width, int height,
            int depth, int border, int format, int type, Buffer source, int srcOffset);

    void texImage3D(int target, int level, int internalformat, int width, int height,
            int depth, int border, int format, int type, ImageData source);

    void texImage3D(int target, int level, int internalformat, int width, int height,
            int depth, int border, int format, int type, HTMLImageElement source);

    void texImage3D(int target, int level, int internalformat, int width, int height,
            int depth, int border, int format, int type, HTMLCanvasElement source);

    void texSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
            int width, int height, int depth, int format, int type, int pboOffset);

    int texSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
            int width, int height, int depth, int format, int type, ImageData source);

    int texSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
            int width, int height, int depth, int format, int type, HTMLImageElement source);

    int texSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
            int width, int height, int depth, int format, int type, HTMLCanvasElement source);
    
    void texSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
            int width, int height, int depth, int format, int type, ArrayBufferView srcData, int srcOffset);

    void texSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
            int width, int height, int depth, int format, int type, ArrayBufferView srcData);

    void texSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
            int width, int height, int depth, int format, int type, Buffer srcData, int srcOffset);

    void texSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
            int width, int height, int depth, int format, int type, Buffer srcData);

    void copyTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
            int x, int y, int width, int height);

    void compressedTexImage3D(int target, int level, int internalformat, int width,
            int height, int depth, int border, int imageSize, int offset);
    
    void compressedTexImage3D(int target, int level, int internalformat, int width,
            int height, int depth, int border, ArrayBufferView srcData, int srcOffset, int srcLengthOverride);

    void compressedTexImage3D(int target, int level, int internalformat, int width,
            int height, int depth, int border, ArrayBufferView srcData);

    void compressedTexImage3D(int target, int level, int internalformat, int width,
            int height, int depth, int border, Buffer srcData, int srcOffset, int srcLengthOverride);

    void compressedTexImage3D(int target, int level, int internalformat, int width,
            int height, int depth, int border, Buffer srcData);

    void compressedTexSubImage3D(int target, int level, int xoffset, int yoffset,
            int zoffset, int width, int height, int depth,
            int format, int imageSize, int offset);
    
    void compressedTexSubImage3D(int target, int level, int xoffset, int yoffset,
            int zoffset, int width, int height, int depth, int format, ArrayBufferView srcData,
            int srcOffset, int srcLengthOverride);

    void compressedTexSubImage3D(int target, int level, int xoffset, int yoffset,
            int zoffset, int width, int height, int depth, int format, ArrayBufferView srcData);

    void compressedTexSubImage3D(int target, int level, int xoffset, int yoffset,
            int zoffset, int width, int height, int depth, int format, Buffer srcData,
            int srcOffset, int srcLengthOverride);

    void compressedTexSubImage3D(int target, int level, int xoffset, int yoffset,
            int zoffset, int width, int height, int depth, int format, Buffer srcData);

    int getFragDataLocation(WebGLProgram program, String name);

    void uniform1ui(WebGLUniformLocation location, int v0);
    void uniform2ui(WebGLUniformLocation location, int v0, int v1);
    void uniform3ui(WebGLUniformLocation location, int v0, int v1, int v2);
    void uniform4ui(WebGLUniformLocation location, int v0, int v1, int v2, int v3);

    void uniform1uiv(WebGLUniformLocation location, Uint32Array data, int srcOffset, int srcLength);

    void uniform1uiv(WebGLUniformLocation location, Uint32Array data);

    void uniform2uiv(WebGLUniformLocation location, Uint32Array data, int srcOffset, int srcLength);

    void uniform2uiv(WebGLUniformLocation location, Uint32Array data);

    void uniform3uiv(WebGLUniformLocation location, Uint32Array data, int srcOffset, int srcLength);

    void uniform3uiv(WebGLUniformLocation location, Uint32Array data);

    void uniform4uiv(WebGLUniformLocation location, Uint32Array data, int srcOffset, int srcLength);

    void uniform4uiv(WebGLUniformLocation location, Uint32Array data);

    void uniformMatrix3x2fv(WebGLUniformLocation location, boolean transpose, Float32Array data,
            int srcOffset, int srcLength);

    void uniformMatrix3x2fv(WebGLUniformLocation location, boolean transpose, Float32Array data);

    void uniformMatrix3x2fv(WebGLUniformLocation location, boolean transpose, @JSByRef(optional = true) float[] data,
            int srcOffset, int srcLength);
    void uniformMatrix3x2fv(WebGLUniformLocation location, boolean transpose, @JSByRef(optional = true) float[] data);

    void uniformMatrix3x2fv(WebGLUniformLocation location, boolean transpose, FloatBuffer data,
            int srcOffset, int srcLength);

    void uniformMatrix3x2fv(WebGLUniformLocation location, boolean transpose, FloatBuffer data);

    void uniformMatrix4x2fv(WebGLUniformLocation location, boolean transpose, Float32Array data,
            int srcOffset, int srcLength);

    void uniformMatrix4x2fv(WebGLUniformLocation location, boolean transpose, Float32Array data);

    void uniformMatrix4x2fv(WebGLUniformLocation location, boolean transpose, @JSByRef(optional = true) float[] data,
            int srcOffset, int srcLength);

    void uniformMatrix4x2fv(WebGLUniformLocation location, boolean transpose, @JSByRef(optional = true) float[] data);

    void uniformMatrix4x2fv(WebGLUniformLocation location, boolean transpose, FloatBuffer data,
            int srcOffset, int srcLength);

    void uniformMatrix4x2fv(WebGLUniformLocation location, boolean transpose, FloatBuffer data);

    void uniformMatrix2x3fv(WebGLUniformLocation location, boolean transpose, Float32Array data,
            int srcOffset, int srcLength);

    void uniformMatrix2x3fv(WebGLUniformLocation location, boolean transpose, Float32Array data);

    void uniformMatrix2x3fv(WebGLUniformLocation location, boolean transpose, @JSByRef(optional = true) float[] data,
            int srcOffset, int srcLength);

    void uniformMatrix2x3fv(WebGLUniformLocation location, boolean transpose, @JSByRef(optional = true) float[] data);

    void uniformMatrix2x3fv(WebGLUniformLocation location, boolean transpose, FloatBuffer data,
            int srcOffset, int srcLength);

    void uniformMatrix2x3fv(WebGLUniformLocation location, boolean transpose, FloatBuffer data);

    void uniformMatrix4x3fv(WebGLUniformLocation location, boolean transpose, Float32Array data,
            int srcOffset, int srcLength);

    void uniformMatrix4x3fv(WebGLUniformLocation location, boolean transpose, Float32Array data);

    void uniformMatrix4x3fv(WebGLUniformLocation location, boolean transpose, @JSByRef(optional = true) float[] data,
            int srcOffset, int srcLength);

    void uniformMatrix4x3fv(WebGLUniformLocation location, boolean transpose, @JSByRef(optional = true) float[] data);

    void uniformMatrix4x3fv(WebGLUniformLocation location, boolean transpose, FloatBuffer data,
            int srcOffset, int srcLength);

    void uniformMatrix4x3fv(WebGLUniformLocation location, boolean transpose, FloatBuffer data);

    void uniformMatrix2x4fv(WebGLUniformLocation location, boolean transpose, Float32Array data,
            int srcOffset, int srcLength);

    void uniformMatrix2x4fv(WebGLUniformLocation location, boolean transpose, Float32Array data);

    void uniformMatrix2x4fv(WebGLUniformLocation location, boolean transpose, @JSByRef(optional = true) float[] data,
            int srcOffset, int srcLength);

    void uniformMatrix2x4fv(WebGLUniformLocation location, boolean transpose, @JSByRef(optional = true) float[] data);

    void uniformMatrix2x4fv(WebGLUniformLocation location, boolean transpose, FloatBuffer data,
            int srcOffset, int srcLength);

    void uniformMatrix2x4fv(WebGLUniformLocation location, boolean transpose, FloatBuffer data);

    void uniformMatrix3x4fv(WebGLUniformLocation location, boolean transpose, Float32Array data,
            int srcOffset, int srcLength);

    void uniformMatrix3x4fv(WebGLUniformLocation location, boolean transpose, Float32Array data);

    void uniformMatrix3x4fv(WebGLUniformLocation location, boolean transpose, @JSByRef(optional = true) float[] data,
            int srcOffset, int srcLength);

    void uniformMatrix3x4fv(WebGLUniformLocation location, boolean transpose, @JSByRef(optional = true) float[] data);

    void uniformMatrix3x4fv(WebGLUniformLocation location, boolean transpose, FloatBuffer data,
            int srcOffset, int srcLength);

    void uniformMatrix3x4fv(WebGLUniformLocation location, boolean transpose, FloatBuffer data);

    void vertexAttribI4i(int index, int x, int y, int z, int w);

    void vertexAttribI4iv(int index, Float32Array values);

    void vertexAttribI4iv(int index, @JSByRef(optional = true) float[] values);

    void vertexAttribI4iv(int index, FloatBuffer values);

    void vertexAttribI4ui(int index, int x, int y, int z, int w);

    void vertexAttribI4uiv(int index, Uint32Array values);

    void vertexAttribIPointer(int index, int size, int type, int stride, int offset);


    void vertexAttribDivisor(int index, int divisor);

    void drawArraysInstanced(int mode, int first, int count, int instanceCount);

    void drawElementsInstanced(int mode, int count, int type, int offset, int instanceCount);

    void drawRangeElements(int mode, int start, int end, int count, int type, int offset);

    void drawBuffers(Int32Array buffers);

    void drawBuffers(@JSByRef(optional = true) int[] buffers);

    void clearBufferfv(int buffer, int drawbuffer, Float32Array values, int srcOffset);

    void clearBufferfv(int buffer, int drawbuffer, Float32Array values);

    void clearBufferfv(int buffer, int drawbuffer, @JSByRef(optional = true) float[] values, int srcOffset);

    void clearBufferfv(int buffer, int drawbuffer, @JSByRef(optional = true) float[] values);

    void clearBufferfv(int buffer, int drawbuffer, FloatBuffer values, int srcOffset);

    void clearBufferfv(int buffer, int drawbuffer, FloatBuffer values);

    void clearBufferiv(int buffer, int drawbuffer, Int32Array values, int srcOffset);

    void clearBufferiv(int buffer, int drawbuffer, Int32Array values);

    void clearBufferiv(int buffer, int drawbuffer, @JSByRef(optional = true) int[] values, int srcOffset);

    void clearBufferiv(int buffer, int drawbuffer, @JSByRef(optional = true) int[] values);

    void clearBufferiv(int buffer, int drawbuffer, IntBuffer values, int srcOffset);

    void clearBufferiv(int buffer, int drawbuffer, IntBuffer values);

    void clearBufferuiv(int buffer, int drawbuffer, Uint32Array values, int srcOffset);

    void clearBufferuiv(int buffer, int drawbuffer, Uint32Array values);

    void clearBufferfi(int buffer, int drawbuffer, float depth, int stencil);

    WebGLQuery createQuery();

    void deleteQuery(WebGLQuery query);

    boolean isQuery(WebGLQuery query);

    void beginQuery(int target, WebGLQuery query);
    void endQuery(int target);

    WebGLQuery getQuery(int target, int pname);

    JSObject getQueryParameter(WebGLQuery query, int pname);

    WebGLSampler createSampler();

    void deleteSampler(WebGLSampler sampler);

    boolean isSampler(WebGLSampler sampler);

    void bindSampler(int unit, WebGLSampler sampler);

    void samplerParameteri(WebGLSampler sampler, int pname, int param);

    void samplerParameterf(WebGLSampler sampler, int pname, float param);

    JSObject getSamplerParameter(WebGLSampler sampler, int pname);

    WebGLSync fenceSync(int condition, int flags);

    boolean isSync(WebGLSync sync);

    void deleteSync(WebGLSync sync);

    int clientWaitSync(WebGLSync sync, int flags, int timeout);

    void waitSync(WebGLSync sync, int flags, int timeout);

    JSObject getSyncParameter(WebGLSync sync, int pname);

    WebGLTransformFeedback createTransformFeedback();

    void deleteTransformFeedback(WebGLTransformFeedback tf);

    boolean isTransformFeedback(WebGLTransformFeedback tf);

    void bindTransformFeedback(int target, WebGLTransformFeedback tf);

    void beginTransformFeedback(int primitiveMode);

    void endTransformFeedback();

    void transformFeedbackVaryings(WebGLProgram program, String[] varyings, int bufferMode);

    void transformFeedbackVaryings(WebGLProgram program, JSArray<JSString> varyings, int bufferMode);

    WebGLActiveInfo getTransformFeedbackVarying(WebGLProgram program, int index);

    void pauseTransformFeedback();

    void resumeTransformFeedback();

    /* Uniform Buffer Objects and Transform Feedback Buffers */
    void bindBufferBase(int target, int index, WebGLBuffer buffer);

    void bindBufferRange(int target, int index, WebGLBuffer buffer, int offset, int size);

    JSObject getIndexedParameter(int target, int index);

    int[] getUniformIndices(WebGLProgram program, String[] uniformNames);

    int[] getUniformIndices(WebGLProgram program, JSArrayReader<JSString> uniformNames);

    JSObject getActiveUniforms(WebGLProgram program, @JSByRef(optional = true) int[] uniformIndices, int pname);

    int getUniformBlockIndex(WebGLProgram program, String uniformBlockName);

    JSObject getActiveUniformBlockParameter(WebGLProgram program, int uniformBlockIndex, int pname);

    String getActiveUniformBlockName(WebGLProgram program, int uniformBlockIndex);

    void uniformBlockBinding(WebGLProgram program, int uniformBlockIndex, int uniformBlockBinding);

    WebGLVertexArrayObject createVertexArray();

    void deleteVertexArray(WebGLVertexArrayObject vertexArray);

    boolean isVertexArray(WebGLVertexArrayObject vertexArray);

    void bindVertexArray(WebGLVertexArrayObject array);
}
