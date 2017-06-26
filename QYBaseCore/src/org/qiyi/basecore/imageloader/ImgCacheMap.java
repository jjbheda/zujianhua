package org.qiyi.basecore.imageloader;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.imageloader.gif.GifDrawable;

public class ImgCacheMap<K, V> {
    private static final String TAG = "ImageCacheMap";

    private final KiloByteBitmapCache<String, Resource<?>> mLruMemCache;

    /**
     * Creates a KiloByteBitmapCache instance initialized to hold (total
     * available memory) / memoryFraction kilobytes worth of Bitmaps.
     *
     * @param memoryFraction
     */
    public ImgCacheMap(final int memoryFraction) {
        mLruMemCache = KiloByteBitmapCache.create(memoryFraction, false);
    }

    public ImgCacheMap(final int memoryFraction, boolean enableFraction) {
        mLruMemCache = KiloByteBitmapCache.create(memoryFraction, enableFraction);
    }

    public Bitmap put(String key, Bitmap value) {
        if (TextUtils.isEmpty(key) || value == null) {
            return value;
        }
        DebugLog.d(TAG, "Current LruMemCache size is : " + mLruMemCache.size()
                + " , Max size: " + mLruMemCache.maxSize());
        Resource<Bitmap> resource = new Resource<Bitmap>();
        resource.setResource(value);
        mLruMemCache.put(key, resource);
        return value;
    }

    public Bitmap get(String key) {
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        DebugLog.d(TAG, "miss count: " + mLruMemCache.missCount() + " hit count: "
                + mLruMemCache.hitCount() + " put count: " + mLruMemCache.putCount());
        Resource resource = mLruMemCache.get(key);
        Bitmap ret = null;
        if (resource != null) {
            Object ob = resource.getResource();
            if (ob != null && ob instanceof Bitmap)
                ret = (Bitmap) ob;
        }
        return ret;
    }


    public Resource<?> putResource(String key, Resource<?> value) {
        if (TextUtils.isEmpty(key) || value == null) {
            return value;
        }
        DebugLog.d(TAG, "Current LruMemCache size is : " + mLruMemCache.size()
                + " , Max size: " + mLruMemCache.maxSize());
        return mLruMemCache.put(key, value);
    }

    public Resource<?> getResource(String key) {
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        DebugLog.d(TAG, "miss count: " + mLruMemCache.missCount() + " hit count: "
                + mLruMemCache.hitCount() + " put count: " + mLruMemCache.putCount());
        return mLruMemCache.get(key);
    }


    public void clear() {
        mLruMemCache.evictAll();
    }

    /**
     * A Bitmap cache that measures the size in kilo-bytes and provides a
     * factory method to adjust to available memory.
     */
    static class KiloByteBitmapCache<K, V> extends LruCache<K, V> {

        private static final int KILOBYTE = 1024;

        private static final int DEFAULT_MEM_SIZE = 3 * KILOBYTE;

        private KiloByteBitmapCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(K key, V resource) {
            // The cache size will be measured in kilobytes rather than
            // number of items. Add 1KB for overhead.
            if (resource instanceof Bitmap) {
                return (getBitmapSize((Bitmap) resource) / KILOBYTE) + 1;
            } else if (resource instanceof Resource) {
                return (getResourceSize((Resource) resource) / KILOBYTE) + 1;
            } else {
                return 1;
            }
        }

        /**
         * Creates a KiloByteBitmapCache instance initialized to hold (total
         * available memory) / memoryFraction kilobytes worth of Bitmaps.
         *
         * @param memoryFraction The max size fraction of memory to use. 1 for 100% of the
         *                       available memory, 5 for 20% of the available memory etc.
         * @return A new instance of KiloByteBitmapCache with a pre-calculated
         * max size.
         * @throws IllegalArgumentException if memoryFraction is less or equal to 0.
         */
        public static <K, V extends Object> KiloByteBitmapCache<K, V> create(
                int memoryFraction, boolean enableFraction) throws IllegalArgumentException {
            int maxMemory = (int) (Runtime.getRuntime().maxMemory() / KILOBYTE);
            if (maxMemory > DEFAULT_MEM_SIZE) {
                maxMemory = DEFAULT_MEM_SIZE;
            }
            if (enableFraction) {
                if (memoryFraction <= 0) {
                    throw new IllegalArgumentException(
                            "Negative memory fractions are not allowed.");
                }
                if (memoryFraction < 2) {
                    memoryFraction = 2;
                }
                int availableSize = (int) ((Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory()) / KILOBYTE);
                DebugLog.d("lincai", "Max available memory size:"
                        + availableSize);
                maxMemory = availableSize / memoryFraction;
                if (maxMemory < 1 * KILOBYTE) {
                    maxMemory = 1 * KILOBYTE;
                }
                if (maxMemory > 12 * KILOBYTE) {
                    maxMemory = 12 * KILOBYTE;
                }
            }
            DebugLog.d("lincai", "maxMemory:" + maxMemory);
            // Get max available VM memory in KB
            // Use 1/CACHE_MEMORY_FRACTION of the available memory for this
            // memory cache.
            return new KiloByteBitmapCache<K, V>(maxMemory);
        }

        private static int getBitmapSize(Bitmap value) {

            int ret = 0;

            // VERSION_CODES.KITKAT 19
            if (Build.VERSION.SDK_INT >= 19) {
                try {
                    ret = value.getAllocationByteCount();
                } catch (Exception e) {
                    DebugLog.e("KiloByteBitmapCache", "exception in getBitmapSize: " + e.getMessage());
                    ret = value.getByteCount();
                }
            } else if (Build.VERSION.SDK_INT >= 12) {
                // VERSION_CODES.HONEYCOMB_MR1 12
                ret = value.getByteCount();
            } else {
                ret = value.getRowBytes() * value.getHeight();
            }

            return ret;
        }

        public static int getResourceSize(Resource resource) {
            int ret = 0;
            if (resource == null) return ret;
            Object ob = resource.getResource();
            if (ob != null) {
                if (ob instanceof Bitmap) {
                    ret = getBitmapSize((Bitmap) ob);
                } else if (ob instanceof GifDrawable) {
                    /**
                     * 此处计算GifDrawable中最占内存的两处数据，Bitmap对象 与byte数组
                     */
                    try {
                        ret += getBitmapSize(((GifDrawable) ob).getFirstFrame()) * 2;
                        ret += ((GifDrawable) ob).getData().length;
                    } catch (Exception e) {
                        DebugLog.d(TAG, "resouce资源计算错误！");
                    }
                }
            }
            DebugLog.d(TAG, "resource 大小：" + ret / KILOBYTE);
            return ret;

        }
    }
}
