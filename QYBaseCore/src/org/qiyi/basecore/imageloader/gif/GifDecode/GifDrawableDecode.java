package org.qiyi.basecore.imageloader.gif.GifDecode;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.qiyi.basecore.imageloader.gif.GifDrawable;
import org.qiyi.basecore.imageloader.gif.GifHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;

/**
 * Created by niejunjiang on 2015/10/25.
 * 这个解析器的作用是吧获取到的gif inputStream解析成GifDrawable 资源
 * 真正做的事只是调用GIfDecoder解析了gif的第一帧和gif的头信息,并没有吧所以的帧都解析出来节省内存
 * 每一帧是需要的时候才会去解析
 */
public class GifDrawableDecode {
    public static final String TAG = GifDrawableDecode.class.toString();

    private static final GifHeaderParserPool PARSER_POOL = new GifHeaderParserPool();
    private static final GifDecoderPool DECODER_POOL = new GifDecoderPool();
    private final GifHeaderParserPool parserPool;
    private final GifDecoderPool decoderPool;
    private final Context context;

    public GifDrawableDecode(Context context) {
        this.context = context;
        parserPool = PARSER_POOL;
        decoderPool = DECODER_POOL;
    }

    public GifDrawable decode(InputStream source, int width, int height) {
        byte[] data = inputStreamToBytes(source);
        final GifHeaderParser parser = parserPool.obtain(data);
        final GifDecoder decoder = decoderPool.obtain();
        try {
            return decode(data, width, height, parser, decoder);
        } finally {
            parserPool.release(parser);
            decoderPool.release(decoder);
        }
    }

    private static byte[] inputStreamToBytes(InputStream is) {
        final int bufferSize = 16384;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferSize);
        try {
            int nRead;
            byte[] data = new byte[bufferSize];
            while ((nRead = is.read(data)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        } catch (IOException e) {
            Log.w(GifDrawable.DEBUGKEY + TAG, "Error reading data from stream", e);
        }
        //TODO the returned byte[] may be partial if an IOException was thrown from read
        return buffer.toByteArray();
    }

    private GifDrawable decode(byte[] data, int width, int height, GifHeaderParser parser, GifDecoder decoder) {
        final GifHeader header = parser.parseHeader();
        if (header.getNumFrames() <= 0 || header.getStatus() != GifDecoder.STATUS_OK) {
            return null;
        }

        Bitmap firstFrame = decodeFirstFrame(decoder, header, data);
        if (firstFrame == null) {
            return null;
        }

        GifDrawable gifDrawable = new GifDrawable(context, width, height, header, data, firstFrame, true);
        return gifDrawable;
    }

    private Bitmap decodeFirstFrame(GifDecoder decoder, GifHeader header, byte[] data) {
        decoder.setData(header, data);
        decoder.advance();
        return decoder.getNextFrame();
    }

    /**
     * 解析器pool,保留一个GifDecoder
     */
    static class GifDecoderPool {
        private final Queue<GifDecoder> pool = Util.createQueue(0);

        public synchronized GifDecoder obtain() {
            GifDecoder result = pool.poll();
            if (result == null) {
                result = new GifDecoder();
            }
            return result;
        }

        public synchronized void release(GifDecoder decoder) {
            decoder.clear();
            pool.offer(decoder);
        }
    }

    /**
     * GIfHeader的解析器池，保留一个GifheaderParser
     */
    static class GifHeaderParserPool {
        private final Queue<GifHeaderParser> pool = Util.createQueue(0);

        public synchronized GifHeaderParser obtain(byte[] data) {
            GifHeaderParser result = pool.poll();
            if (result == null) {
                result = new GifHeaderParser();
            }
            result.clear();
            return result.setData(data);
        }

        public synchronized void release(GifHeaderParser parser) {
            parser.clear();
            pool.offer(parser);
        }
    }


}
