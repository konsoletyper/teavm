package org.teavm.eclipse.debugger.ui;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

/**
 *
 * @author Alexey Andreev
 */
public class StorageEditorInput extends PlatformObject implements IStorageEditorInput {
    private IStorage storage;

    public StorageEditorInput(IStorage storage) {
        this.storage = storage;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        return getStorage().getName();
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return getStorage().getFullPath().toOSString();
    }

    @Override
    public IStorage getStorage() {
        return storage;
    }
}
