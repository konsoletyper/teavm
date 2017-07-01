/*
 *  Copyright 2015 Alexey Andreev.
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

import org.teavm.jso.JSByRef;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int32Array;

public interface WebGLRenderingContext extends JSObject {
    int DEPTH_BUFFER_BIT               = 0x00000100;
    int STENCIL_BUFFER_BIT             = 0x00000400;
    int COLOR_BUFFER_BIT               = 0x00004000;
    int POINTS                         = 0x0000;
    int LINES                          = 0x0001;
    int LINE_LOOP                      = 0x0002;
    int LINE_STRIP                     = 0x0003;
    int TRIANGLES                      = 0x0004;
    int TRIANGLE_STRIP                 = 0x0005;
    int TRIANGLE_FAN                   = 0x0006;

    int ZERO                           = 0;
    int ONE                            = 1;
    int SRC_COLOR                      = 0x0300;
    int ONE_MINUS_SRC_COLOR            = 0x0301;
    int SRC_ALPHA                      = 0x0302;
    int ONE_MINUS_SRC_ALPHA            = 0x0303;
    int DST_ALPHA                      = 0x0304;
    int ONE_MINUS_DST_ALPHA            = 0x0305;

    int DST_COLOR                      = 0x0306;
    int ONE_MINUS_DST_COLOR            = 0x0307;
    int SRC_ALPHA_SATURATE             = 0x0308;

    int FUNC_ADD                       = 0x8006;
    int BLEND_EQUATION                 = 0x8009;
    int BLEND_EQUATION_RGB             = 0x8009;
    int BLEND_EQUATION_ALPHA           = 0x883D;

    int FUNC_SUBTRACT                  = 0x800A;
    int FUNC_REVERSE_SUBTRACT          = 0x800B;

    int BLEND_DST_RGB                  = 0x80C8;
    int BLEND_SRC_RGB                  = 0x80C9;
    int BLEND_DST_ALPHA                = 0x80CA;
    int BLEND_SRC_ALPHA                = 0x80CB;
    int CONSTANT_COLOR                 = 0x8001;
    int ONE_MINUS_CONSTANT_COLOR       = 0x8002;
    int CONSTANT_ALPHA                 = 0x8003;
    int ONE_MINUS_CONSTANT_ALPHA       = 0x8004;
    int BLEND_COLOR                    = 0x8005;

    int ARRAY_BUFFER                   = 0x8892;
    int ELEMENT_ARRAY_BUFFER           = 0x8893;
    int ARRAY_BUFFER_BINDING           = 0x8894;
    int ELEMENT_ARRAY_BUFFER_BINDING   = 0x8895;

    int STREAM_DRAW                    = 0x88E0;
    int STATIC_DRAW                    = 0x88E4;
    int DYNAMIC_DRAW                   = 0x88E8;

    int BUFFER_SIZE                    = 0x8764;
    int BUFFER_USAGE                   = 0x8765;

    int CURRENT_VERTEX_ATTRIB          = 0x8626;

    int FRONT                          = 0x0404;
    int BACK                           = 0x0405;
    int FRONT_AND_BACK                 = 0x0408;

    int CULL_FACE                      = 0x0B44;
    int BLEND                          = 0x0BE2;
    int DITHER                         = 0x0BD0;
    int STENCIL_TEST                   = 0x0B90;
    int DEPTH_TEST                     = 0x0B71;
    int SCISSOR_TEST                   = 0x0C11;
    int POLYGON_OFFSET_FILL            = 0x8037;
    int SAMPLE_ALPHA_TO_COVERAGE       = 0x809E;
    int SAMPLE_COVERAGE                = 0x80A0;

    int NO_ERROR                       = 0;
    int INVALID_ENUM                   = 0x0500;
    int INVALID_VALUE                  = 0x0501;
    int INVALID_OPERATION              = 0x0502;
    int OUT_OF_MEMORY                  = 0x0505;

    int CW                             = 0x0900;
    int CCW                            = 0x0901;

    int LINE_WIDTH                     = 0x0B21;
    int ALIASED_POINT_SIZE_RANGE       = 0x846D;
    int ALIASED_LINE_WIDTH_RANGE       = 0x846E;
    int CULL_FACE_MODE                 = 0x0B45;
    int FRONT_FACE                     = 0x0B46;
    int DEPTH_RANGE                    = 0x0B70;
    int DEPTH_WRITEMASK                = 0x0B72;
    int DEPTH_CLEAR_VALUE              = 0x0B73;
    int DEPTH_FUNC                     = 0x0B74;
    int STENCIL_CLEAR_VALUE            = 0x0B91;
    int STENCIL_FUNC                   = 0x0B92;
    int STENCIL_FAIL                   = 0x0B94;
    int STENCIL_PASS_DEPTH_FAIL        = 0x0B95;
    int STENCIL_PASS_DEPTH_PASS        = 0x0B96;
    int STENCIL_REF                    = 0x0B97;
    int STENCIL_VALUE_MASK             = 0x0B93;
    int STENCIL_WRITEMASK              = 0x0B98;
    int STENCIL_BACK_FUNC              = 0x8800;
    int STENCIL_BACK_FAIL              = 0x8801;
    int STENCIL_BACK_PASS_DEPTH_FAIL   = 0x8802;
    int STENCIL_BACK_PASS_DEPTH_PASS   = 0x8803;
    int STENCIL_BACK_REF               = 0x8CA3;
    int STENCIL_BACK_VALUE_MASK        = 0x8CA4;
    int STENCIL_BACK_WRITEMASK         = 0x8CA5;
    int VIEWPORT                       = 0x0BA2;
    int SCISSOR_BOX                    = 0x0C10;
    int COLOR_CLEAR_VALUE              = 0x0C22;
    int COLOR_WRITEMASK                = 0x0C23;
    int UNPACK_ALIGNMENT               = 0x0CF5;
    int PACK_ALIGNMENT                 = 0x0D05;
    int MAX_TEXTURE_SIZE               = 0x0D33;
    int MAX_VIEWPORT_DIMS              = 0x0D3A;
    int SUBPIXEL_BITS                  = 0x0D50;
    int RED_BITS                       = 0x0D52;
    int GREEN_BITS                     = 0x0D53;
    int BLUE_BITS                      = 0x0D54;
    int ALPHA_BITS                     = 0x0D55;
    int DEPTH_BITS                     = 0x0D56;
    int STENCIL_BITS                   = 0x0D57;
    int POLYGON_OFFSET_UNITS           = 0x2A00;
    int POLYGON_OFFSET_FACTOR          = 0x8038;
    int TEXTURE_BINDING_2D             = 0x8069;
    int SAMPLE_BUFFERS                 = 0x80A8;
    int SAMPLES                        = 0x80A9;
    int SAMPLE_COVERAGE_VALUE          = 0x80AA;
    int SAMPLE_COVERAGE_INVERT         = 0x80AB;

    int COMPRESSED_TEXTURE_FORMATS     = 0x86A3;

    int DONT_CARE                      = 0x1100;
    int FASTEST                        = 0x1101;
    int NICEST                         = 0x1102;

    int GENERATE_MIPMAP_HINT            = 0x8192;

    int BYTE                           = 0x1400;
    int UNSIGNED_BYTE                  = 0x1401;
    int SHORT                          = 0x1402;
    int UNSIGNED_SHORT                 = 0x1403;
    int INT                            = 0x1404;
    int UNSIGNED_INT                   = 0x1405;
    int FLOAT                          = 0x1406;

    int DEPTH_COMPONENT                = 0x1902;
    int ALPHA                          = 0x1906;
    int RGB                            = 0x1907;
    int RGBA                           = 0x1908;
    int LUMINANCE                      = 0x1909;
    int LUMINANCE_ALPHA                = 0x190A;

    int UNSIGNED_SHORT_4_4_4_4         = 0x8033;
    int UNSIGNED_SHORT_5_5_5_1         = 0x8034;
    int UNSIGNED_SHORT_5_6_5           = 0x8363;

    int FRAGMENT_SHADER                  = 0x8B30;
    int VERTEX_SHADER                    = 0x8B31;
    int MAX_VERTEX_ATTRIBS               = 0x8869;
    int MAX_VERTEX_UNIFORM_VECTORS       = 0x8DFB;
    int MAX_VARYING_VECTORS              = 0x8DFC;
    int MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8B4D;
    int MAX_VERTEX_TEXTURE_IMAGE_UNITS   = 0x8B4C;
    int MAX_TEXTURE_IMAGE_UNITS          = 0x8872;
    int MAX_FRAGMENT_UNIFORM_VECTORS     = 0x8DFD;
    int SHADER_TYPE                      = 0x8B4F;
    int DELETE_STATUS                    = 0x8B80;
    int LINK_STATUS                      = 0x8B82;
    int VALIDATE_STATUS                  = 0x8B83;
    int ATTACHED_SHADERS                 = 0x8B85;
    int ACTIVE_UNIFORMS                  = 0x8B86;
    int ACTIVE_ATTRIBUTES                = 0x8B89;
    int SHADING_LANGUAGE_VERSION         = 0x8B8C;
    int CURRENT_PROGRAM                  = 0x8B8D;

    int NEVER                          = 0x0200;
    int LESS                           = 0x0201;
    int EQUAL                          = 0x0202;
    int LEQUAL                         = 0x0203;
    int GREATER                        = 0x0204;
    int NOTEQUAL                       = 0x0205;
    int GEQUAL                         = 0x0206;
    int ALWAYS                         = 0x0207;

    int KEEP                           = 0x1E00;
    int REPLACE                        = 0x1E01;
    int INCR                           = 0x1E02;
    int DECR                           = 0x1E03;
    int INVERT                         = 0x150A;
    int INCR_WRAP                      = 0x8507;
    int DECR_WRAP                      = 0x8508;

    int VENDOR                         = 0x1F00;
    int RENDERER                       = 0x1F01;
    int VERSION                        = 0x1F02;

    int NEAREST                        = 0x2600;
    int LINEAR                         = 0x2601;

    int NEAREST_MIPMAP_NEAREST         = 0x2700;
    int LINEAR_MIPMAP_NEAREST          = 0x2701;
    int NEAREST_MIPMAP_LINEAR          = 0x2702;
    int LINEAR_MIPMAP_LINEAR           = 0x2703;

    int TEXTURE_MAG_FILTER             = 0x2800;
    int TEXTURE_MIN_FILTER             = 0x2801;
    int TEXTURE_WRAP_S                 = 0x2802;
    int TEXTURE_WRAP_T                 = 0x2803;

    int TEXTURE_2D                     = 0x0DE1;
    int TEXTURE                        = 0x1702;

    int TEXTURE_CUBE_MAP               = 0x8513;
    int TEXTURE_BINDING_CUBE_MAP       = 0x8514;
    int TEXTURE_CUBE_MAP_POSITIVE_X    = 0x8515;
    int TEXTURE_CUBE_MAP_NEGATIVE_X    = 0x8516;
    int TEXTURE_CUBE_MAP_POSITIVE_Y    = 0x8517;
    int TEXTURE_CUBE_MAP_NEGATIVE_Y    = 0x8518;
    int TEXTURE_CUBE_MAP_POSITIVE_Z    = 0x8519;
    int TEXTURE_CUBE_MAP_NEGATIVE_Z    = 0x851A;
    int MAX_CUBE_MAP_TEXTURE_SIZE      = 0x851C;

    int TEXTURE0                       = 0x84C0;
    int TEXTURE1                       = 0x84C1;
    int TEXTURE2                       = 0x84C2;
    int TEXTURE3                       = 0x84C3;
    int TEXTURE4                       = 0x84C4;
    int TEXTURE5                       = 0x84C5;
    int TEXTURE6                       = 0x84C6;
    int TEXTURE7                       = 0x84C7;
    int TEXTURE8                       = 0x84C8;
    int TEXTURE9                       = 0x84C9;
    int TEXTURE10                      = 0x84CA;
    int TEXTURE11                      = 0x84CB;
    int TEXTURE12                      = 0x84CC;
    int TEXTURE13                      = 0x84CD;
    int TEXTURE14                      = 0x84CE;
    int TEXTURE15                      = 0x84CF;
    int TEXTURE16                      = 0x84D0;
    int TEXTURE17                      = 0x84D1;
    int TEXTURE18                      = 0x84D2;
    int TEXTURE19                      = 0x84D3;
    int TEXTURE20                      = 0x84D4;
    int TEXTURE21                      = 0x84D5;
    int TEXTURE22                      = 0x84D6;
    int TEXTURE23                      = 0x84D7;
    int TEXTURE24                      = 0x84D8;
    int TEXTURE25                      = 0x84D9;
    int TEXTURE26                      = 0x84DA;
    int TEXTURE27                      = 0x84DB;
    int TEXTURE28                      = 0x84DC;
    int TEXTURE29                      = 0x84DD;
    int TEXTURE30                      = 0x84DE;
    int TEXTURE31                      = 0x84DF;
    int ACTIVE_TEXTURE                 = 0x84E0;

    int REPEAT                         = 0x2901;
    int CLAMP_TO_EDGE                  = 0x812F;
    int MIRRORED_REPEAT                = 0x8370;

    int FLOAT_VEC2                     = 0x8B50;
    int FLOAT_VEC3                     = 0x8B51;
    int FLOAT_VEC4                     = 0x8B52;
    int INT_VEC2                       = 0x8B53;
    int INT_VEC3                       = 0x8B54;
    int INT_VEC4                       = 0x8B55;
    int BOOL                           = 0x8B56;
    int BOOL_VEC2                      = 0x8B57;
    int BOOL_VEC3                      = 0x8B58;
    int BOOL_VEC4                      = 0x8B59;
    int FLOAT_MAT2                     = 0x8B5A;
    int FLOAT_MAT3                     = 0x8B5B;
    int FLOAT_MAT4                     = 0x8B5C;
    int SAMPLER_2D                     = 0x8B5E;
    int SAMPLER_CUBE                   = 0x8B60;

    /* Vertex Arrays */
    int VERTEX_ATTRIB_ARRAY_ENABLED        = 0x8622;
    int VERTEX_ATTRIB_ARRAY_SIZE           = 0x8623;
    int VERTEX_ATTRIB_ARRAY_STRIDE         = 0x8624;
    int VERTEX_ATTRIB_ARRAY_TYPE           = 0x8625;
    int VERTEX_ATTRIB_ARRAY_NORMALIZED     = 0x886A;
    int VERTEX_ATTRIB_ARRAY_POINTER        = 0x8645;
    int VERTEX_ATTRIB_ARRAY_BUFFER_BINDING = 0x889F;

    /* Shader Source */
    int COMPILE_STATUS                 = 0x8B81;

    /* Shader Precision-Specified Types */
    int LOW_FLOAT                      = 0x8DF0;
    int MEDIUM_FLOAT                   = 0x8DF1;
    int HIGH_FLOAT                     = 0x8DF2;
    int LOW_INT                        = 0x8DF3;
    int MEDIUM_INT                     = 0x8DF4;
    int HIGH_INT                       = 0x8DF5;

    /* Framebuffer Object. */
    int FRAMEBUFFER                    = 0x8D40;
    int RENDERBUFFER                   = 0x8D41;

    int RGBA4                          = 0x8056;
    int RGB5_A1                        = 0x8057;
    int RGB565                         = 0x8D62;
    int DEPTH_COMPONENT16              = 0x81A5;
    int STENCIL_INDEX                  = 0x1901;
    int STENCIL_INDEX8                 = 0x8D48;
    int DEPTH_STENCIL                  = 0x84F9;

    int RENDERBUFFER_WIDTH             = 0x8D42;
    int RENDERBUFFER_HEIGHT            = 0x8D43;
    int RENDERBUFFER_INTERNAL_FORMAT   = 0x8D44;
    int RENDERBUFFER_RED_SIZE          = 0x8D50;
    int RENDERBUFFER_GREEN_SIZE        = 0x8D51;
    int RENDERBUFFER_BLUE_SIZE         = 0x8D52;
    int RENDERBUFFER_ALPHA_SIZE        = 0x8D53;
    int RENDERBUFFER_DEPTH_SIZE        = 0x8D54;
    int RENDERBUFFER_STENCIL_SIZE      = 0x8D55;

    int FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE           = 0x8CD0;
    int FRAMEBUFFER_ATTACHMENT_OBJECT_NAME           = 0x8CD1;
    int FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL         = 0x8CD2;
    int FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE = 0x8CD3;

    int COLOR_ATTACHMENT0              = 0x8CE0;
    int DEPTH_ATTACHMENT               = 0x8D00;
    int STENCIL_ATTACHMENT             = 0x8D20;
    int DEPTH_STENCIL_ATTACHMENT       = 0x821A;

    int NONE                           = 0;

    int FRAMEBUFFER_COMPLETE                      = 0x8CD5;
    int FRAMEBUFFER_INCOMPLETE_ATTACHMENT         = 0x8CD6;
    int FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7;
    int FRAMEBUFFER_INCOMPLETE_DIMENSIONS         = 0x8CD9;
    int FRAMEBUFFER_UNSUPPORTED                   = 0x8CDD;

    int FRAMEBUFFER_BINDING            = 0x8CA6;
    int RENDERBUFFER_BINDING           = 0x8CA7;
    int MAX_RENDERBUFFER_SIZE          = 0x84E8;

    int INVALID_FRAMEBUFFER_OPERATION  = 0x0506;

    int UNPACK_FLIP_Y_WEBGL            = 0x9240;
    int UNPACK_PREMULTIPLY_ALPHA_WEBGL = 0x9241;
    int CONTEXT_LOST_WEBGL             = 0x9242;
    int UNPACK_COLORSPACE_CONVERSION_WEBGL = 0x9243;
    int BROWSER_DEFAULT_WEBGL          = 0x9244;

    @JSProperty
    HTMLCanvasElement getCanvas();

    @JSProperty
    int getDrawingBufferWidth();

    @JSProperty
    int getDrawingBufferHeight();

    WebGLContextAttributes getContextAttributes();

    boolean isContextLost();

    JSArrayReader<JSString> getSupportedExtensions();

    @JSMethod("getSupportedExtensions")
    String[] getSupportedExtensionArray();

    JSObject getExtension(String name);

    void activeTexture(int texture);

    void attachShader(WebGLProgram program, WebGLShader shader);

    void bindAttribLocation(WebGLProgram program, int index, String name);

    void bindBuffer(int target, WebGLBuffer buffer);

    void bindFramebuffer(int target, WebGLFramebuffer framebuffer);

    void bindRenderbuffer(int target, WebGLRenderbuffer renderbuffer);

    void bindTexture(int target, WebGLTexture texture);

    void blendColor(float red, float green, float blue, float alpha);

    void blendEquation(int mode);

    void blendEquationSeparate(int modeRGB, int modeAlpha);

    void blendFunc(int sfactor, int dfactor);

    void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);


    void bufferData(int target, int size, int usage);

    void bufferData(int target, ArrayBufferView data, int usage);

    void bufferData(int target, ArrayBuffer data, int usage);

    void bufferSubData(int target, int offset, ArrayBufferView data);

    void bufferSubData(int target, int offset, ArrayBuffer data);

    int checkFramebufferStatus(int target);

    void clear(int mask);

    void clearColor(float red, float green, float blue, float alpha);

    void clearDepth(float depth);

    void clearStencil(int s);

    void colorMask(boolean red, boolean green, boolean blue, boolean alpha);

    void compileShader(WebGLShader shader);

    void compressedTexImage2D(int target, int level, int internalformat, int width, int height, int border,
            ArrayBufferView data);

    void compressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format,
               ArrayBufferView data);

    void copyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border);

    void copyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height);


    WebGLBuffer createBuffer();

    WebGLFramebuffer createFramebuffer();

    WebGLProgram createProgram();

    WebGLRenderbuffer createRenderbuffer();

    WebGLShader createShader(int type);

    WebGLTexture createTexture();

    void cullFace(int mode);

    void deleteBuffer(WebGLBuffer buffer);

    void deleteFramebuffer(WebGLFramebuffer framebuffer);

    void deleteProgram(WebGLProgram program);

    void deleteRenderbuffer(WebGLRenderbuffer renderbuffer);

    void deleteShader(WebGLShader shader);

    void deleteTexture(WebGLTexture texture);

    void depthFunc(int func);

    void depthMask(boolean flag);

    void depthRange(float zNear, float zFar);

    void detachShader(WebGLProgram program, WebGLShader shader);

    void disable(int cap);

    void disableVertexAttribArray(int index);

    void drawArrays(int mode, int first, int count);

    void drawElements(int mode, int count, int type, int offset);

    void enable(int cap);

    void enableVertexAttribArray(int index);

    void finish();

    void flush();

    void framebufferRenderbuffer(int target, int attachment, int renderbuffertarget, WebGLRenderbuffer renderbuffer);

    void framebufferTexture2D(int target, int attachment, int textarget, WebGLTexture texture, int level);

    void frontFace(int mode);

    void generateMipmap(int target);

    WebGLActiveInfo getActiveAttrib(WebGLProgram program, int index);

    WebGLActiveInfo getActiveUniform(WebGLProgram program, int index);

    JSArrayReader<WebGLShader> getAttachedShaders(WebGLProgram program);

    @JSMethod("getAttachedShaders")
    WebGLShader[] getAttachedShadersArray(WebGLProgram program);

    int getAttribLocation(WebGLProgram program, String name);

    JSObject getBufferParameter(int target, int pname);

    JSObject getParameter(int pname);

    @JSMethod("getParameter")
    int getParameteri(int pname);

    @JSMethod("getParameter")
    String getParameterString(int pname);

    @JSMethod("getParameter")
    float getParameterf(int pname);

    int getError();

    JSObject getFramebufferAttachmentParameter(int target, int attachment, int pname);

    JSObject getProgramParameter(WebGLProgram program, int pname);

    @JSMethod("getProgramParameter")
    boolean getProgramParameterb(WebGLProgram program, int pname);

    @JSMethod("getProgramParameter")
    int getProgramParameteri(WebGLProgram program, int pname);

    String getProgramInfoLog(WebGLProgram program);

    JSObject getRenderbufferParameter(int target, int pname);

    JSObject getShaderParameter(WebGLShader shader, int pname);

    @JSMethod("getShaderParameter")
    boolean getShaderParameterb(WebGLShader shader, int pname);

    @JSMethod("getShaderParameter")
    int getShaderParameteri(WebGLShader shader, int pname);

    WebGLShaderPrecisionFormat getShaderPrecisionFormat(int shadertype, int precisiontype);

    String getShaderInfoLog(WebGLShader shader);

    String getShaderSource(WebGLShader shader);

    JSObject getTexParameter(int target, int pname);

    JSObject getUniform(WebGLProgram program, WebGLUniformLocation location);

    WebGLUniformLocation getUniformLocation(WebGLProgram program, String name);

    JSObject getVertexAttrib(int index, int pname);

    int getVertexAttribOffset(int index, int pname);

    void hint(int target, int mode);

    boolean isBuffer(WebGLBuffer buffer);

    boolean isEnabled(int cap);

    boolean isFramebuffer(WebGLFramebuffer framebuffer);

    boolean isProgram(WebGLProgram program);

    boolean isRenderbuffer(WebGLRenderbuffer renderbuffer);

    boolean isShader(WebGLShader shader);

    boolean isTexture(WebGLTexture texture);

    void lineWidth(float width);

    void linkProgram(WebGLProgram program);

    void pixelStorei(int pname, int param);

    void polygonOffset(float factor, float units);

    void readPixels(int x, int y, int width, int height, int format, int type, ArrayBufferView pixels);

    void renderbufferStorage(int target, int internalformat, int width, int height);

    void sampleCoverage(float value, boolean invert);

    void scissor(int x, int y, int width, int height);

    void shaderSource(WebGLShader shader, String source);

    void stencilFunc(int func, int ref, int mask);

    void stencilFuncSeparate(int face, int func, int ref, int mask);

    void stencilMask(int mask);

    void stencilMaskSeparate(int face, int mask);

    void stencilOp(int fail, int zfail, int zpass);

    void stencilOpSeparate(int face, int fail, int zfail, int zpass);

    void texImage2D(int target, int level, int internalformat, int width, int height, int border, int format,
            int type, ArrayBufferView pixels);

    void texImage2D(int target, int level, int internalformat, int format, int type, ImageData pixels);

    void texImage2D(int target, int level, int internalformat, int format, int type, HTMLImageElement image);

    void texImage2D(int target, int level, int internalformat, int format, int type, HTMLCanvasElement canvas);

    //void texImage2D(int target, int level, int internalformat, int format, int type, HTMLVideoElement video);

    void texParameterf(int target, int pname, float param);

    void texParameteri(int target, int pname, int param);

    void texSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height,  int format, int type,
            ArrayBufferView pixels);

    void texSubImage2D(int target, int level, int xoffset, int yoffset, int format, int type, ImageData pixels);

    void texSubImage2D(int target, int level, int xoffset, int yoffset, int format, int type, HTMLImageElement image);

    void texSubImage2D(int target, int level, int xoffset, int yoffset, int format, int type, HTMLCanvasElement canvas);

    //void texSubImage2D(int target, int level, int xoffset, int yoffset, int format, int type, HTMLVideoElement video);

    void uniform1f(WebGLUniformLocation location, float x);

    void uniform1fv(WebGLUniformLocation location, Float32Array v);

    void uniform1fv(WebGLUniformLocation location, JSArrayReader<JSNumber> v);

    void uniform1fv(WebGLUniformLocation location, @JSByRef float[] v);

    void uniform1i(WebGLUniformLocation location, int x);

    void uniform1iv(WebGLUniformLocation location, Int32Array v);

    void uniform1iv(WebGLUniformLocation location, JSArrayReader<JSNumber> v);

    void uniform1iv(WebGLUniformLocation location, @JSByRef int[] v);

    void uniform2f(WebGLUniformLocation location, float x, float y);

    void uniform2fv(WebGLUniformLocation location, Float32Array v);

    void uniform2fv(WebGLUniformLocation location, JSArrayReader<JSNumber> v);

    void uniform2fv(WebGLUniformLocation location, @JSByRef float[] v);

    void uniform2i(WebGLUniformLocation location, int x, int y);

    void uniform2iv(WebGLUniformLocation location, Int32Array v);

    void uniform2iv(WebGLUniformLocation location, JSArrayReader<JSNumber> v);

    void uniform2iv(WebGLUniformLocation location, @JSByRef int[] v);

    void uniform3f(WebGLUniformLocation location, float x, float y, float z);

    void uniform3fv(WebGLUniformLocation location, Float32Array v);

    void uniform3fv(WebGLUniformLocation location, JSArrayReader<JSNumber> v);

    void uniform3fv(WebGLUniformLocation location, @JSByRef float[] v);

    void uniform3i(WebGLUniformLocation location, int x, int y, int z);

    void uniform3iv(WebGLUniformLocation location, Int32Array v);

    void uniform3iv(WebGLUniformLocation location, JSArrayReader<JSNumber> v);

    void uniform3iv(WebGLUniformLocation location, @JSByRef int[] v);

    void uniform4f(WebGLUniformLocation location, float x, float y, float z, float w);

    void uniform4fv(WebGLUniformLocation location, Float32Array v);

    void uniform4fv(WebGLUniformLocation location, JSArrayReader<JSNumber> v);

    void uniform4fv(WebGLUniformLocation location, @JSByRef float[] v);

    void uniform4i(WebGLUniformLocation location, int x, int y, int z, int w);

    void uniform4iv(WebGLUniformLocation location, Int32Array v);

    void uniform4iv(WebGLUniformLocation location, JSArrayReader<JSNumber> v);

    void uniform4iv(WebGLUniformLocation location, @JSByRef int[] v);

    void uniformMatrix2fv(WebGLUniformLocation location, boolean transpose, Float32Array value);

    void uniformMatrix2fv(WebGLUniformLocation location, boolean transpose, JSArrayReader<JSNumber> value);

    void uniformMatrix2fv(WebGLUniformLocation location, boolean transpose, @JSByRef float[] value);

    void uniformMatrix3fv(WebGLUniformLocation location, boolean transpose, Float32Array value);

    void uniformMatrix3fv(WebGLUniformLocation location, boolean transpose, JSArrayReader<JSNumber> value);

    void uniformMatrix3fv(WebGLUniformLocation location, boolean transpose, @JSByRef float[] value);

    void uniformMatrix4fv(WebGLUniformLocation location, boolean transpose, Float32Array value);

    void uniformMatrix4fv(WebGLUniformLocation location, boolean transpose, JSArrayReader<JSNumber> value);

    void uniformMatrix4fv(WebGLUniformLocation location, boolean transpose, @JSByRef float[] value);

    void useProgram(WebGLProgram program);

    void validateProgram(WebGLProgram program);

    void vertexAttrib1f(int indx, float x);

    void vertexAttrib1fv(int indx, Float32Array values);

    void vertexAttrib1fv(int indx, JSArrayReader<JSNumber> values);

    void vertexAttrib1fv(int indx, @JSByRef float[] values);

    void vertexAttrib2f(int indx, float x, float y);

    void vertexAttrib2fv(int indx, Float32Array values);

    void vertexAttrib2fv(int indx, JSArrayReader<JSNumber> values);

    void vertexAttrib2fv(int indx, @JSByRef float[] values);

    void vertexAttrib3f(int indx, float x, float y, float z);

    void vertexAttrib3fv(int indx, Float32Array values);

    void vertexAttrib3fv(int indx, JSArrayReader<JSNumber> values);

    void vertexAttrib3fv(int indx, @JSByRef float[] values);

    void vertexAttrib4f(int indx, float x, float y, float z, float w);

    void vertexAttrib4fv(int indx, Float32Array values);

    void vertexAttrib4fv(int indx, JSArrayReader<JSNumber> values);

    void vertexAttrib4fv(int indx, @JSByRef float[] values);

    void vertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int offset);

    void viewport(int x, int y, int width, int height);
}
