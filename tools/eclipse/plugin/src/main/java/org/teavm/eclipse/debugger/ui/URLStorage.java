package org.teavm.eclipse.debugger.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.teavm.eclipse.TeaVMEclipsePlugin;

/**
 *
 * @author Alexey Andreev
 */
public class URLStorage extends PlatformObject implements IStorage {
    private URL url;

    public URLStorage(URL url) {
        this.url = url;
    }

    @Override
    public InputStream getContents() throws CoreException {
        try {
            return url.openStream();
        } catch (IOException e) {
            throw new CoreException(TeaVMEclipsePlugin.makeError(e));
        }
    }

    @Override
    public IPath getFullPath() {
        return null;
    }

    @Override
    public String getName() {
        return url.getFile();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
