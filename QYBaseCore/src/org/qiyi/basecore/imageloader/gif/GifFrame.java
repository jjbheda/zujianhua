package org.qiyi.basecore.imageloader.gif;

/**
 * Created by niejunjiang on 2015/10/24.
 */
public class GifFrame {

    public int ix;
    public int iy;
    public int iw;
    public int ih;
    /**
     * Control Flag.
     */
    public boolean interlace;
    /**
     * Control Flag.
     */
    public boolean transparency;
    /**
     * Disposal Method.
     */
    public int dispose;
    /**
     * Transparency Index.
     */
    public int transIndex;
    /**
     * Delay, in ms, to next frame.
     */
    public int delay;
    /**
     * Index in the raw buffer where we need to start reading to decode.
     */
    public int bufferFrameStart;
    /**
     * Local Color Table.
     */
    public int[] lct;
}
