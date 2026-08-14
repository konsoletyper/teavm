package org.teavm.jso.webgpu;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventTarget;

public interface GPUDevice extends EventTarget, GPU.ObjectBase {
    @JSProperty
    GPU.StringSet getFeatures();

    @JSProperty
    GPUAdapter.SupportedLimits getLimits();

    @JSProperty
    GPUAdapter.Info getAdapterInfo();

    @JSProperty
    GPUQueue getQueue();

    @JSProperty
    JSPromise<DeviceLostInfo> getLost();

    void destroy();

    GPUBuffer createBuffer(GPUDescriptor.Buffer descriptor);

    GPUTexture createTexture(GPUDescriptor.Texture descriptor);

    GPUSampler createSampler();

    GPUSampler createSampler(GPUDescriptor.Sampler descriptor);

    ExternalTexture importExternalTexture(GPUDescriptor.ExternalTexture descriptor);

    GPUBindGroup.Layout createBindGroupLayout(GPUDescriptor.BindGroupLayout descriptor);

    GPUPipeline.Layout createPipelineLayout(GPUDescriptor.PipelineLayout descriptor);

    GPUBindGroup createBindGroup(GPUDescriptor.BindGroup descriptor);

    GPUShaderModule createShaderModule(GPUDescriptor.ShaderModule descriptor);

    GPUPipeline.Compute createComputePipeline(GPUDescriptor.ComputePipeline descriptor);

    GPUPipeline.Render createRenderPipeline(GPUDescriptor.RenderPipeline descriptor);

    JSPromise<GPUPipeline.Compute> createComputePipelineAsync(GPUDescriptor.ComputePipeline descriptor);

    JSPromise<GPUPipeline.Render> createRenderPipelineAsync(GPUDescriptor.RenderPipeline descriptor);

    GPUCommandEncoder createCommandEncoder();

    GPUCommandEncoder createCommandEncoder(GPUDescriptor.CommandEncoder descriptor);

    GPURenderPassEncoder.BundleEncoder createRenderBundleEncoder(GPUDescriptor.RenderBundleEncoder descriptor);

    QuerySet createQuerySet(GPUDescriptor.QuerySet descriptor);

    void pushErrorScope(String filter);

    JSPromise<Error> popErrorScope();

    interface ExternalTexture extends GPU.ObjectBase {
    }

    interface QuerySet extends GPU.ObjectBase {
        void destroy();

        @JSProperty
        String getType();

        @JSProperty
        int getCount();
    }

    interface DeviceLostInfo extends JSObject {
        @JSProperty
        String getReason();

        @JSProperty
        String getMessage();
    }

    interface Error extends JSObject {
        @JSProperty
        String getMessage();
    }

    interface UncapturedErrorEvent extends Event {
        @JSProperty
        Error getError();
    }
}

