package org.qiyi.basecore.dynamicload;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.qiyi.basecore.dynamicload.DynamicLoader.LoadFinishCallback;
import org.qiyi.basecore.utils.FileUtils;

import dalvik.system.DexClassLoader;

import android.content.Context;
import android.os.Handler;

public class InjectTask extends Thread {
	private static final String PLUGIN_LIB_PATH = "pluginlib";

    private Context mContext;
    private File mOriginalFile;
    private String mUrl;
    private IFileAuthenticator mAuth;
    private Handler mHandler;
    private LoadFinishCallback mLoadFinishCb;

    /**
     * Inject file for so, Java(dex, apk, zip) or zip file which include so, java
     * 
     * @param context
     * @param originalFile local file which will be injected
     * @param url corresponding with the originalFile, maybe null for local file
     * @param auth auth interface to privilege the original file
     * @param handler handler to post callback result;
     * @param callback call back after finish task
     */
	public InjectTask(Context context, File originalFile, String url,
			IFileAuthenticator auth, Handler handler, LoadFinishCallback callback) {
		super("InjectTask");
		mContext = context;
		mOriginalFile = originalFile;
		mUrl = url;
		mAuth = auth;
		mHandler = handler;
		mLoadFinishCb = callback;
	}

	@Override
    public void run() {
        if (mContext == null || mOriginalFile == null || mAuth == null || mHandler == null
                || mLoadFinishCb == null) {
            List<InjectResult>  result = new ArrayList<InjectResult>();
            result.add(new InjectResult(null, InjectResult.INVALIDATE_PARAM,
                    PluginFile.FILE_TYPE_UNKNOWN));
            postResult(mHandler, mLoadFinishCb, result);
            return;
        }
        List<PluginFile> files = mAuth.authenticate(mOriginalFile, mUrl);
        if (files == null || files.size() < 1) {
            return;
        }
        List<InjectResult> result = new ArrayList<InjectResult>(files.size());
        PluginFile file;
        for (int i = 0; i < files.size(); i++) {
            file = files.get(i);
            if (file.mFileType == PluginFile.FILE_TYPE_DEX && file.mFile != null) {
                result.add(injectDex(mContext, file.mFile));
            } else if (file.mFileType == PluginFile.FILE_TYPE_SO && file.mFile != null) {
                result.add(injectSo(mContext, file.mFile));
            } else {
                result.add(new InjectResult(null, InjectResult.INVALIDATE_PARAM,
                        PluginFile.FILE_TYPE_UNKNOWN));
            }
        }
        postResult(mHandler, mLoadFinishCb, result);
    }

    private void postResult(Handler handler, final LoadFinishCallback loadFinishCb,
            final List<InjectResult> result) {
        if (null != handler && null != loadFinishCb) {
            handler.post(new Runnable() {

                @Override
                public void run() {
                    loadFinishCb.onLoadFinish(result);
                }
            });
        }
    }

	/**
	 * Inject SO file
	 * 
	 * @param context
	 * @param soFile
	 * @return return the inject result or error {@link InjectResult}
	 */
	private static InjectResult injectSo(Context context, File soFile) {
		File tempFile = new File(getPluginLibDir(context),
				System.currentTimeMillis() + ".so");
		if (!FileUtils.copyToFile(soFile, tempFile) || tempFile == null
				|| !tempFile.canRead() || tempFile.length() <= 0) {
			if (tempFile != null) {
				tempFile.delete();
			}
			return new InjectResult(null, InjectResult.COPY_FAILED,
					PluginFile.FILE_TYPE_SO);
		} else {
			System.load(tempFile.getAbsolutePath());
			return new InjectResult(null, InjectResult.INJECT_SUCCESS,
					PluginFile.FILE_TYPE_SO);
		}
	}

	/**
	 * Inject java file
	 * 
	 * @param context
	 * @param dexFile
	 * @return
	 */
	private static InjectResult injectDex(Context context, File dexFile) {
		File libDir = getPluginLibDir(context);
		if (libDir == null) {
			return new InjectResult(null, InjectResult.COPY_FAILED,
					PluginFile.FILE_TYPE_DEX);
		}
		ClassLoader clsLoader = new DexClassLoader(dexFile.getAbsolutePath(),
				libDir.getAbsolutePath(), null, context.getClassLoader());
		return new InjectResult(clsLoader, InjectResult.INJECT_SUCCESS,
				PluginFile.FILE_TYPE_DEX);
	}

	private static File getPluginLibDir(Context context) {
		if (context == null) {
			return null;
		}
		File rootDir = context.getDir(PLUGIN_LIB_PATH, 0);
		if (!rootDir.exists()) {
			rootDir.mkdir();
		}
		return rootDir;
	}

	public static class InjectResult {
		public static final int INVALIDATE_PARAM = 0;
		public static final int DOWNLOAD_FAILED = INVALIDATE_PARAM + 1;
		public static final int AUTHENTICATION_FAILED = DOWNLOAD_FAILED + 1;
		public static final int COPY_FAILED = AUTHENTICATION_FAILED + 1;
		public static final int INJECT_FAILED = COPY_FAILED + 1;
		public static final int INJECT_SUCCESS = INJECT_FAILED + 1;
		/**
		 * Class loader for inject java
		 */
		public ClassLoader mClsLoader;
		/**
		 * Inject result
		 */
		public int mInjectResult;
		/**
		 * Inject file type{@link PluginFile}
		 */
		public int mFileType;

		/**
		 * Single file's inject result
		 * 
		 * @param clsLoader
		 *            class loader for java file, maybe null for so type file
		 *            and inject failed
		 * @param injectResult
		 *            inject result {@link #DOWNLOAD_FAILED} etc.
		 * @param fileType
		 *            file type {@link PluginFile}
		 */
		public InjectResult(ClassLoader clsLoader, int injectResult,
				int fileType) {
			mClsLoader = clsLoader;
			mInjectResult = injectResult;
			mFileType = fileType;
		}
	}
}
