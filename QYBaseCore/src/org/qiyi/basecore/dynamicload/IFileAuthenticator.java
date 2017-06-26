package org.qiyi.basecore.dynamicload;

import java.io.File;
import java.util.List;

public interface IFileAuthenticator {
    /**
     * Should override this to check the file which has been download There are several scenarios,
     * the file may be zip or some other type Authenticator need do the corresponding operation to
     * check the plug in file under this file. After all permission has pass the check, then
     * Authenticator should return the plug in file, this plug in file maybe just part of the
     * original file.
     * 
     * @param file The original file
     * @param url The original url, maybe null for local file
     * @return The plugin files or null when all authenticate failed
     */
    List<PluginFile> authenticate(File file, String url);
}
