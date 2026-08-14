package org.teavm.jso.webgpu;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.canvas.CanvasImageSource;
import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSNumber;

public abstract class GPUDescriptor implements JSObject {
    protected GPUDescriptor() {
        // le ✨stub✨
    }

    @JSBody(script = "return {};")
    public static native <T extends GPUDescriptor> T create();

    public abstract static class ObjectBase extends GPUDescriptor {
        @JSProperty
        public abstract String getLabel();

        @JSProperty
        public abstract void setLabel(String label);
    }

    public abstract static class RequestAdapterOptions extends GPUDescriptor {
        @JSProperty
        public abstract void setFeatureLevel(String featureLevel);

        @JSProperty
        public abstract void setPowerPreference(String powerPreference);

        @JSProperty
        public abstract void setForceFallbackAdapter(boolean forceFallbackAdapter);

        @JSProperty
        public abstract void setXrCompatible(boolean xrCompatible);
    }

    public abstract static class Device extends ObjectBase {
        @JSProperty
        public abstract void setRequiredFeatures(String[] requiredFeatures);

        @JSProperty
        public abstract void setRequiredLimits(JSMapLike<JSNumber> requiredLimits);

        @JSProperty
        public abstract void setDefaultQueue(Queue defaultQueue);
    }

    public abstract static class Queue extends ObjectBase {
        // le ✨stub✨
    }

    public abstract static class Buffer extends ObjectBase {
        @JSProperty
        public abstract void setSize(double size);

        @JSProperty
        public abstract void setUsage(int usage);

        @JSProperty
        public abstract void setMappedAtCreation(boolean mappedAtCreation);
    }

    public abstract static class Extent3D extends GPUDescriptor {
        @JSProperty
        public abstract void setWidth(int width);

        @JSProperty
        public abstract void setHeight(int height);

        @JSProperty
        public abstract void setDepthOrArrayLayers(int depthOrArrayLayers);
    }

    public abstract static class Origin2D extends GPUDescriptor {
        @JSProperty
        public abstract void setX(int x);

        @JSProperty
        public abstract void setY(int y);
    }

    public abstract static class Origin3D extends GPUDescriptor {
        @JSProperty
        public abstract void setX(int x);

        @JSProperty
        public abstract void setY(int y);

        @JSProperty
        public abstract void setZ(int z);
    }

    public abstract static class Color extends GPUDescriptor {
        @JSProperty
        public abstract void setR(double r);

        @JSProperty
        public abstract void setG(double g);

        @JSProperty
        public abstract void setB(double b);

        @JSProperty
        public abstract void setA(double a);
    }

    public abstract static class Texture extends ObjectBase {
        @JSProperty
        public abstract void setSize(Extent3D size);

        @JSProperty
        public abstract void setMipLevelCount(int mipLevelCount);

        @JSProperty
        public abstract void setSampleCount(int sampleCount);

        @JSProperty
        public abstract void setDimension(String dimension);

        @JSProperty
        public abstract void setFormat(String format);

        @JSProperty
        public abstract void setUsage(int usage);

        @JSProperty
        public abstract void setViewFormats(String[] viewFormats);
    }

    public abstract static class TextureView extends ObjectBase {
        @JSProperty
        public abstract void setFormat(String format);

        @JSProperty
        public abstract void setDimension(String dimension);

        @JSProperty
        public abstract void setUsage(int usage);

        @JSProperty
        public abstract void setAspect(String aspect);

        @JSProperty
        public abstract void setBaseMipLevel(int baseMipLevel);

        @JSProperty
        public abstract void setMipLevelCount(int mipLevelCount);

        @JSProperty
        public abstract void setBaseArrayLayer(int baseArrayLayer);

        @JSProperty
        public abstract void setArrayLayerCount(int arrayLayerCount);
    }

    public abstract static class Sampler extends ObjectBase {
        @JSProperty
        public abstract void setAddressModeU(String value);

        @JSProperty
        public abstract void setAddressModeV(String value);

        @JSProperty
        public abstract void setAddressModeW(String value);

        @JSProperty
        public abstract void setMagFilter(String value);

        @JSProperty
        public abstract void setMinFilter(String value);

        @JSProperty
        public abstract void setMipmapFilter(String value);

        @JSProperty
        public abstract void setLodMinClamp(double value);

        @JSProperty
        public abstract void setLodMaxClamp(double value);

        @JSProperty
        public abstract void setCompare(String value);

        @JSProperty
        public abstract void setMaxAnisotropy(int value);
    }

    public abstract static class ExternalTexture extends ObjectBase {
        @JSProperty
        public abstract void setSource(CanvasImageSource source);

        @JSProperty
        public abstract void setColorSpace(String colorSpace);
    }

    public abstract static class CanvasToneMapping extends GPUDescriptor {
        @JSProperty
        public abstract String getMode();

        @JSProperty
        public abstract void setMode(String mode);
    }

    public abstract static class CanvasConfiguration extends GPUDescriptor {
        @JSProperty
        public abstract GPUDevice getDevice();

        @JSProperty
        public abstract void setDevice(GPUDevice device);

        @JSProperty
        public abstract String getFormat();

        @JSProperty
        public abstract void setFormat(String format);

        @JSProperty
        public abstract int getUsage();

        @JSProperty
        public abstract void setUsage(int usage);

        @JSProperty
        public abstract String[] getViewFormats();

        @JSProperty
        public abstract void setViewFormats(String[] viewFormats);

        @JSProperty
        public abstract String getColorSpace();

        @JSProperty
        public abstract void setColorSpace(String colorSpace);

        @JSProperty
        public abstract CanvasToneMapping getToneMapping();

        @JSProperty
        public abstract void setToneMapping(CanvasToneMapping toneMapping);

        @JSProperty
        public abstract String getAlphaMode();

        @JSProperty
        public abstract void setAlphaMode(String alphaMode);
    }

    public abstract static class BufferBinding extends GPUDescriptor {
        @JSProperty
        public abstract void setBuffer(GPUBuffer buffer);

        @JSProperty
        public abstract void setOffset(double offset);

        @JSProperty
        public abstract void setSize(double size);
    }

    public abstract static class BufferBindingLayout extends GPUDescriptor {
        @JSProperty
        public abstract void setType(String type);

        @JSProperty
        public abstract void setHasDynamicOffset(boolean hasDynamicOffset);

        @JSProperty
        public abstract void setMinBindingSize(double minBindingSize);
    }

    public abstract static class SamplerBindingLayout extends GPUDescriptor {
        @JSProperty
        public abstract void setType(String type);
    }

    public abstract static class TextureBindingLayout extends GPUDescriptor {
        @JSProperty
        public abstract void setSampleType(String sampleType);

        @JSProperty
        public abstract void setViewDimension(String viewDimension);

        @JSProperty
        public abstract void setMultisampled(boolean multisampled);
    }

    public abstract static class StorageTextureBindingLayout extends GPUDescriptor {
        @JSProperty
        public abstract void setAccess(String access);

        @JSProperty
        public abstract void setFormat(String format);

        @JSProperty
        public abstract void setViewDimension(String viewDimension);
    }

    public abstract static class ExternalTextureBindingLayout extends GPUDescriptor {
        // le ✨stub✨
    }

    public abstract static class BindGroupLayoutEntry extends GPUDescriptor {
        @JSProperty
        public abstract void setBinding(int binding);

        @JSProperty
        public abstract void setVisibility(int visibility);

        @JSProperty
        public abstract void setBuffer(BufferBindingLayout buffer);

        @JSProperty
        public abstract void setSampler(SamplerBindingLayout sampler);

        @JSProperty
        public abstract void setTexture(TextureBindingLayout texture);

        @JSProperty
        public abstract void setStorageTexture(StorageTextureBindingLayout storageTexture);

        @JSProperty
        public abstract void setExternalTexture(ExternalTextureBindingLayout externalTexture);
    }

    public abstract static class BindGroupLayout extends ObjectBase {
        @JSProperty
        public abstract void setEntries(BindGroupLayoutEntry[] entries);
    }

    public abstract static class BindGroupEntry extends GPUDescriptor {
        @JSProperty
        public abstract void setBinding(int binding);

        @JSProperty
        public abstract void setResource(JSObject resource);
    }

    public abstract static class BindGroup extends ObjectBase {
        @JSProperty
        public abstract void setLayout(GPUBindGroup.Layout layout);

        @JSProperty
        public abstract void setEntries(BindGroupEntry[] entries);
    }

    public abstract static class PipelineLayout extends ObjectBase {
        @JSProperty
        public abstract void setBindGroupLayouts(GPUBindGroup.Layout[] bindGroupLayouts);

        @JSProperty
        public abstract void setImmediateSize(int immediateSize);
    }

    public abstract static class ShaderModuleCompilationHint extends GPUDescriptor {
        @JSProperty
        public abstract void setEntryPoint(String entryPoint);

        @JSProperty
        public abstract void setLayout(GPUPipeline.Layout layout);

        @JSProperty
        public abstract void setLayout(String autoLayout);
    }

    public abstract static class ShaderModule extends ObjectBase {
        @JSProperty
        public abstract void setCode(String code);

        @JSProperty
        public abstract void setCompilationHints(ShaderModuleCompilationHint[] compilationHints);
    }

    public abstract static class ProgrammableStage extends GPUDescriptor {
        @JSProperty
        public abstract void setModule(GPUShaderModule module);

        @JSProperty
        public abstract void setEntryPoint(String entryPoint);

        @JSProperty
        public abstract void setConstants(JSMapLike<JSNumber> constants);
    }

    public abstract static class Pipeline extends ObjectBase {
        @JSProperty
        public abstract void setLayout(GPUPipeline.Layout layout);

        @JSProperty
        public abstract void setLayout(String autoLayout);
    }

    public abstract static class ComputePipeline extends Pipeline {
        @JSProperty
        public abstract void setCompute(ProgrammableStage compute);
    }

    public abstract static class VertexAttribute extends GPUDescriptor {
        @JSProperty
        public abstract void setFormat(String format);

        @JSProperty
        public abstract void setOffset(double offset);

        @JSProperty
        public abstract void setShaderLocation(int shaderLocation);
    }

    public abstract static class VertexBufferLayout extends GPUDescriptor {
        @JSProperty
        public abstract void setArrayStride(double arrayStride);

        @JSProperty
        public abstract void setStepMode(String stepMode);

        @JSProperty
        public abstract void setAttributes(VertexAttribute[] attributes);
    }

    public abstract static class VertexState extends ProgrammableStage {
        @JSProperty
        public abstract void setBuffers(VertexBufferLayout[] buffers);
    }

    public abstract static class BlendComponent extends GPUDescriptor {
        @JSProperty
        public abstract void setOperation(String operation);

        @JSProperty
        public abstract void setSrcFactor(String srcFactor);

        @JSProperty
        public abstract void setDstFactor(String dstFactor);
    }

    public abstract static class BlendState extends GPUDescriptor {
        @JSProperty
        public abstract void setColor(BlendComponent color);

        @JSProperty
        public abstract void setAlpha(BlendComponent alpha);
    }

    public abstract static class ColorTargetState extends GPUDescriptor {
        @JSProperty
        public abstract void setFormat(String format);

        @JSProperty
        public abstract void setBlend(BlendState blend);

        @JSProperty
        public abstract void setWriteMask(int writeMask);
    }

    public abstract static class FragmentState extends ProgrammableStage {
        @JSProperty
        public abstract void setTargets(ColorTargetState[] targets);
    }

    public abstract static class PrimitiveState extends GPUDescriptor {
        @JSProperty
        public abstract void setTopology(String topology);

        @JSProperty
        public abstract void setStripIndexFormat(String stripIndexFormat);

        @JSProperty
        public abstract void setFrontFace(String frontFace);

        @JSProperty
        public abstract void setCullMode(String cullMode);

        @JSProperty
        public abstract void setUnclippedDepth(boolean unclippedDepth);
    }

    public abstract static class StencilFaceState extends GPUDescriptor {
        @JSProperty
        public abstract void setCompare(String compare);

        @JSProperty
        public abstract void setFailOp(String failOp);

        @JSProperty
        public abstract void setDepthFailOp(String depthFailOp);

        @JSProperty
        public abstract void setPassOp(String passOp);
    }

    public abstract static class DepthStencilState extends GPUDescriptor {
        @JSProperty
        public abstract void setFormat(String format);

        @JSProperty
        public abstract void setDepthWriteEnabled(boolean depthWriteEnabled);

        @JSProperty
        public abstract void setDepthCompare(String depthCompare);

        @JSProperty
        public abstract void setStencilFront(StencilFaceState stencilFront);

        @JSProperty
        public abstract void setStencilBack(StencilFaceState stencilBack);

        @JSProperty
        public abstract void setStencilReadMask(int stencilReadMask);

        @JSProperty
        public abstract void setStencilWriteMask(int stencilWriteMask);

        @JSProperty
        public abstract void setDepthBias(int depthBias);

        @JSProperty
        public abstract void setDepthBiasSlopeScale(double depthBiasSlopeScale);

        @JSProperty
        public abstract void setDepthBiasClamp(double depthBiasClamp);
    }

    public abstract static class MultisampleState extends GPUDescriptor {
        @JSProperty
        public abstract void setCount(int count);

        @JSProperty
        public abstract void setMask(int mask);

        @JSProperty
        public abstract void setAlphaToCoverageEnabled(boolean alphaToCoverageEnabled);
    }

    public abstract static class RenderPipeline extends Pipeline {
        @JSProperty
        public abstract void setVertex(VertexState vertex);

        @JSProperty
        public abstract void setPrimitive(PrimitiveState primitive);

        @JSProperty
        public abstract void setDepthStencil(DepthStencilState depthStencil);

        @JSProperty
        public abstract void setMultisample(MultisampleState multisample);

        @JSProperty
        public abstract void setFragment(FragmentState fragment);
    }

    public abstract static class QuerySet extends ObjectBase {
        @JSProperty
        public abstract void setType(String type);

        @JSProperty
        public abstract void setCount(int count);
    }

    public abstract static class ComputePassTimestampWrites extends GPUDescriptor {
        @JSProperty
        public abstract void setQuerySet(GPUDevice.QuerySet querySet);

        @JSProperty
        public abstract void setBeginningOfPassWriteIndex(int index);

        @JSProperty
        public abstract void setEndOfPassWriteIndex(int index);
    }

    public abstract static class ComputePass extends ObjectBase {
        @JSProperty
        public abstract void setTimestampWrites(ComputePassTimestampWrites timestampWrites);
    }

    public abstract static class RenderPassTimestampWrites extends GPUDescriptor {
        @JSProperty
        public abstract void setQuerySet(GPUDevice.QuerySet querySet);

        @JSProperty
        public abstract void setBeginningOfPassWriteIndex(int index);

        @JSProperty
        public abstract void setEndOfPassWriteIndex(int index);
    }

    public abstract static class RenderPassColorAttachment extends GPUDescriptor {
        @JSProperty
        public abstract void setView(GPUTexture.View view);

        @JSProperty
        public abstract void setView(GPUTexture texture);

        @JSProperty
        public abstract void setDepthSlice(int depthSlice);

        @JSProperty
        public abstract void setResolveTarget(GPUTexture.View resolveTarget);

        @JSProperty
        public abstract void setResolveTarget(GPUTexture resolveTarget);

        @JSProperty
        public abstract void setClearValue(Color clearValue);

        @JSProperty
        public abstract void setLoadOp(String loadOp);

        @JSProperty
        public abstract void setStoreOp(String storeOp);
    }

    public abstract static class RenderPassDepthStencilAttachment extends GPUDescriptor {
        @JSProperty
        public abstract void setView(GPUTexture.View view);

        @JSProperty
        public abstract void setView(GPUTexture texture);

        @JSProperty
        public abstract void setDepthClearValue(double depthClearValue);

        @JSProperty
        public abstract void setDepthLoadOp(String depthLoadOp);

        @JSProperty
        public abstract void setDepthStoreOp(String depthStoreOp);

        @JSProperty
        public abstract void setDepthReadOnly(boolean depthReadOnly);

        @JSProperty
        public abstract void setStencilClearValue(int stencilClearValue);

        @JSProperty
        public abstract void setStencilLoadOp(String stencilLoadOp);

        @JSProperty
        public abstract void setStencilStoreOp(String stencilStoreOp);

        @JSProperty
        public abstract void setStencilReadOnly(boolean stencilReadOnly);
    }

    public abstract static class RenderPass extends ObjectBase {
        @JSProperty
        public abstract void setColorAttachments(RenderPassColorAttachment[] colorAttachments);

        @JSProperty
        public abstract void setDepthStencilAttachment(RenderPassDepthStencilAttachment attachment);

        @JSProperty
        public abstract void setOcclusionQuerySet(GPUDevice.QuerySet occlusionQuerySet);

        @JSProperty
        public abstract void setTimestampWrites(RenderPassTimestampWrites timestampWrites);

        @JSProperty
        public abstract void setMaxDrawCount(double maxDrawCount);
    }

    public abstract static class RenderPassLayout extends ObjectBase {
        @JSProperty
        public abstract void setColorFormats(String[] colorFormats);

        @JSProperty
        public abstract void setDepthStencilFormat(String depthStencilFormat);

        @JSProperty
        public abstract void setSampleCount(int sampleCount);
    }

    public abstract static class RenderBundleEncoder extends RenderPassLayout {
        @JSProperty
        public abstract void setDepthReadOnly(boolean depthReadOnly);

        @JSProperty
        public abstract void setStencilReadOnly(boolean stencilReadOnly);
    }

    public abstract static class CommandEncoder extends ObjectBase {
    }

    public abstract static class CommandBuffer extends ObjectBase {
    }

    public abstract static class RenderBundle extends ObjectBase {
    }

    public abstract static class TexelCopyBufferLayout extends GPUDescriptor {
        @JSProperty
        public abstract void setOffset(double offset);

        @JSProperty
        public abstract void setBytesPerRow(int bytesPerRow);

        @JSProperty
        public abstract void setRowsPerImage(int rowsPerImage);
    }

    public abstract static class TexelCopyBufferInfo extends TexelCopyBufferLayout {
        @JSProperty
        public abstract void setBuffer(GPUBuffer buffer);
    }

    public abstract static class TexelCopyTextureInfo extends GPUDescriptor {
        @JSProperty
        public abstract void setTexture(GPUTexture texture);

        @JSProperty
        public abstract void setMipLevel(int mipLevel);

        @JSProperty
        public abstract void setOrigin(Origin3D origin);

        @JSProperty
        public abstract void setAspect(String aspect);
    }

    public abstract static class CopyExternalImageSourceInfo extends GPUDescriptor {
        @JSProperty
        public abstract void setSource(CanvasImageSource source);

        @JSProperty
        public abstract void setOrigin(Origin2D origin);

        @JSProperty
        public abstract void setFlipY(boolean flipY);
    }

    public abstract static class CopyExternalImageDestInfo extends TexelCopyTextureInfo {
        @JSProperty
        public abstract void setColorSpace(String colorSpace);

        @JSProperty
        public abstract void setPremultipliedAlpha(boolean premultipliedAlpha);
    }
}
