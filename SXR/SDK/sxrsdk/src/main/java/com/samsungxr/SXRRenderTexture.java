/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsungxr;

/** Frame Buffer object. */
public class SXRRenderTexture extends SXRTexture {
    /**
     * Constructs a SXRRenderTexture for a frame buffer of the specified size.
     *
     * @param gvrContext
     *            Current gvrContext
     * @param width
     *            Width of the frame buffer.
     * @param height
     *            Height of the frame buffer.
     */
    public SXRRenderTexture(SXRContext gvrContext, int width, int height)
    {
        super(gvrContext, NativeRenderTexture.ctor(width, height));

        mWidth = width;
        mHeight = height;
    }

    /**
     * Constructs a SXRRenderTexture for a frame buffer of the specified size,
     * with MSAA enabled at the specified sample count.
     *
     * @param gvrContext
     *            Current gvrContext
     * @param width
     *            Width of the frame buffer.
     * @param height
     *            Height of the frame buffer.
     * @param sampleCount
     *            MSAA sample count.
     */
    public SXRRenderTexture(SXRContext gvrContext, int width, int height, int sampleCount)
    {
        this(gvrContext,width,height,sampleCount,1);
    }

    public SXRRenderTexture(SXRContext gvrContext, int width, int height, int sampleCount, int number_views)
    {
        super(gvrContext, NativeRenderTexture.ctorMSAA(width, height,
                sampleCount, number_views));
        mWidth = width;
        mHeight = height;
    }

    public SXRRenderTexture(SXRContext gvrContext, int width, int height, long ptr){
        super(gvrContext,ptr);
        mWidth = width;
        mHeight= height;
    }
    /**
     * Constructs a SXRRenderTexture for a frame buffer of the specified size,
     * with MSAA enabled at the specified sample count, and with specified color
     * format, depth format, resolution depth and texture parameters.
     *
     * @param gvrContext
     *            Current gvrContext
     * @param width
     *            Width of the frame buffer.
     * @param height
     *            Height of the frame buffer.
     * @param sampleCount
     *            MSAA sample count.
     * @param colorFormat
     *            SXR color format.
     *            See {@linkplain com.samsungxr.utility.VrAppSettings.EyeBufferParams.ColorFormat ColorFormat}.
     * @param depthFormat
     *            Depth format.
     *            See {@linkplain com.samsungxr.utility.VrAppSettings.EyeBufferParams.DepthFormat DepthFormat}.
     * @param resolveDepth
     *            If true, resolves the depth buffer into a texture.
     * @param parameters
     *            Texture parameters. See {@link SXRTextureParameters}.
     *
     */
    public SXRRenderTexture(SXRContext gvrContext, int width, int height,
                            int sampleCount, int colorFormat, int depthFormat,
                            boolean resolveDepth, SXRTextureParameters parameters)
    {
        this(gvrContext, width, height, sampleCount, colorFormat, depthFormat, resolveDepth, parameters, 1);
    }
    /**
     * Constructs a SXRRenderTexture for a frame buffer of the specified size,
     * with MSAA enabled at the specified sample count, and with specified color
     * format, depth format, resolution depth and texture parameters.
     *
     * @param gvrContext
     *            Current gvrContext
     * @param width
     *            Width of the frame buffer.
     * @param height
     *            Height of the frame buffer.
     * @param sampleCount
     *            MSAA sample count.
     * @param colorFormat
     *            SXR color format.
     *            See {@linkplain com.samsungxr.utility.VrAppSettings.EyeBufferParams.ColorFormat ColorFormat}.
     * @param depthFormat
     *            Depth format.
     *            See {@linkplain com.samsungxr.utility.VrAppSettings.EyeBufferParams.DepthFormat DepthFormat}.
     * @param resolveDepth
     *            If true, resolves the depth buffer into a texture.
     * @param parameters
     *            Texture parameters. See {@link SXRTextureParameters}.
     * @param numberViews
     *            If multiview, it should be 2 or else 1.
     */
    public SXRRenderTexture(SXRContext gvrContext, int width, int height,
                            int sampleCount, int colorFormat, int depthFormat,
                            boolean resolveDepth, SXRTextureParameters parameters, int numberViews)
    {
        super(gvrContext, NativeRenderTexture.ctorWithParameters(width, height,
                sampleCount, colorFormat,
                depthFormat, resolveDepth,
                parameters.getCurrentValuesArray(), numberViews));
        mWidth = width;
        mHeight = height;
    }
    SXRRenderTexture(SXRContext gvrContext, long ptr) {
        super(gvrContext, ptr);
    }

    /**
     * Return the width of SXRRenderTexture (FBO)
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Return the height of SXRRenderTexture (FBO)
     */
    public int getHeight() {
        return mHeight;
    }

    void beginRendering() {
        NativeRenderTexture.beginRendering(getNative());
    }

    void endRendering() {
        NativeRenderTexture.endRendering(getNative());
    }
    /**
     * Return the render texture.
     *
     * @param readbackBuffer
     *        A preallocated IntBuffer to receive the data from the texture. Its capacity should
     *        be width * height. The output pixel format is GPU format GL_RGBA packed as an 32-bit
     *        integer.
     *
     * @return true if successful.
     */
    boolean readRenderResult(int[] readbackBuffer)
    {
        return NativeRenderTexture.readRenderResult(getNative(), readbackBuffer);
    }

    /**
     * Bind the framebuffer for this SXRRenderTexture.
     *
     *      Before calling this, remember to retrieve the currently bound framebuffer (with glGetIntegerv(GL_FRAMEBUFFER_BINDING, int[])) so you can restore it after issuing calls to this SXRRenderTexture.
     */
    public void bind() {
        NativeRenderTexture.bind(getNative());
    }

    private int mWidth, mHeight;
}

class NativeRenderTexture {
    static native long ctor(int width, int height);

    static native long ctorMSAA(int width, int height, int sampleCount, int number_views);

    static native long ctorWithParameters(int width, int height,
                                          int sampleCount, int colorFormat, int depthFormat,
                                          boolean resolveDepth, int[] parameters, int number_views);

    static native long ctorArray(int width, int height, int samples, int layers);


    static native void beginRendering(long ptr);

    static native void endRendering(long ptr);
    static native boolean readRenderResult(long ptr, int[] readbackBuffer);

    static native void bind(long ptr);
}
