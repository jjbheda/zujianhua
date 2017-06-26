package org.qiyi.basecore.imageloader.gif;

/**
 * Created by niejunjiang on 2015/10/25.
 */

import org.qiyi.basecore.imageloader.gif.GifDecode.GifDecoder;

import java.util.ArrayList;
import java.util.List;

public class GifHeader {
    public int[] gct = null;
    public int status = GifDecoder.STATUS_OK;
    public int frameCount = 0;

    public GifFrame currentFrame;
    public List<GifFrame> frames = new ArrayList<GifFrame>();
    // Logical screen size.
    // Full image width.
    public int width;
    // Full image height.
    public int height;

    // 1 : global color table flag.
    public boolean gctFlag;
    // 2-4 : color resolution.
    // 5 : gct sort flag.
    // 6-8 : gct size.
    public int gctSize;
    // Background color index.
    public int bgIndex;
    // Pixel aspect ratio.
    public int pixelAspect;
    //TODO: this is set both during reading the header and while decoding frames...
    public int bgColor;
    public int loopCount;

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getNumFrames() {
        return frameCount;
    }

    /**
     * Global status code of GIF data parsing.
     */
    public int getStatus() {
        return status;
    }
}
