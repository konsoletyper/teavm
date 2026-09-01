package org.teavm.jso.webgpu;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;

public interface GPUAdapter extends JSObject {
    @JSProperty
    GPU.StringSet getFeatures();

    @JSProperty
    SupportedLimits getLimits();

    @JSProperty
    Info getInfo();

    JSPromise<GPUDevice> requestDevice();

    JSPromise<GPUDevice> requestDevice(GPUDescriptor.Device descriptor);

    interface Info extends JSObject {
        @JSProperty
        String getVendor();

        @JSProperty
        String getArchitecture();

        @JSProperty
        String getDevice();

        @JSProperty
        String getDescription();

        @JSProperty
        int getSubgroupMinSize();

        @JSProperty
        int getSubgroupMaxSize();

        @JSProperty
        boolean isFallbackAdapter();
    }

    interface SupportedLimits extends JSObject {
        @JSProperty
        int getMaxTextureDimension1D();

        @JSProperty
        int getMaxTextureDimension2D();

        @JSProperty
        int getMaxTextureDimension3D();

        @JSProperty
        int getMaxTextureArrayLayers();

        @JSProperty
        int getMaxBindGroups();

        @JSProperty
        int getMaxBindingsPerBindGroup();

        @JSProperty
        int getMaxSampledTexturesPerShaderStage();

        @JSProperty
        int getMaxSamplersPerShaderStage();

        @JSProperty
        int getMaxStorageBuffersPerShaderStage();

        @JSProperty
        int getMaxStorageTexturesPerShaderStage();

        @JSProperty
        int getMaxUniformBuffersPerShaderStage();

        @JSProperty
        double getMaxUniformBufferBindingSize();

        @JSProperty
        double getMaxStorageBufferBindingSize();

        @JSProperty
        int getMinUniformBufferOffsetAlignment();

        @JSProperty
        int getMinStorageBufferOffsetAlignment();

        @JSProperty
        int getMaxVertexBuffers();

        @JSProperty
        double getMaxBufferSize();

        @JSProperty
        int getMaxVertexAttributes();

        @JSProperty
        int getMaxVertexBufferArrayStride();

        @JSProperty
        int getMaxInterStageShaderVariables();

        @JSProperty
        int getMaxColorAttachments();

        @JSProperty
        int getMaxColorAttachmentBytesPerSample();

        @JSProperty
        int getMaxComputeWorkgroupStorageSize();

        @JSProperty
        int getMaxComputeInvocationsPerWorkgroup();

        @JSProperty
        int getMaxComputeWorkgroupSizeX();

        @JSProperty
        int getMaxComputeWorkgroupSizeY();

        @JSProperty
        int getMaxComputeWorkgroupSizeZ();

        @JSProperty
        int getMaxComputeWorkgroupsPerDimension();
    }
}

