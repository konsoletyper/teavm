package org.teavm.eclipse;

import org.eclipse.core.runtime.Plugin;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMEclipsePlugin extends Plugin {
    private static TeaVMEclipsePlugin defaultInstance;

    static {
        System.out.println();
    }

    public TeaVMEclipsePlugin() {
        defaultInstance = this;
    }

    public static TeaVMEclipsePlugin getDefault() {
        return defaultInstance;
    }
}
