package org.teavm.eclipse.debugger.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PlatformUI;

public class URLEditorInput extends PlatformObject implements IStorageEditorInput {
    private URL url;

    public URLEditorInput(URL url) {
        this.url = url;
    }

    @Override
    public boolean exists() {
        try (InputStream input = url.openStream()) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(url.getFile());
    }

    @Override
    public String getName() {
        return url.getFile();
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return url.toString();
    }

    @Override
    public IStorage getStorage() throws CoreException {
        return new URLStorage(url);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof URLEditorInput)) {
            return false;
        }
        URLEditorInput other = (URLEditorInput)obj;
        return Objects.equals(url, other.url);
    }
}
