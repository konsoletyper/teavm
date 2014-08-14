package org.teavm.eclipse.debugger;

/**
 *
 * @author Alexey Andreev
 */
public interface TeaVMDebugConstants {
    public static final String JAVA_BREAKPOINT_INSTALL_COUNT = "org.eclipse.jdt.debug.core.installCount";

    public static final String DEBUG_TARGET_ID = "org.teavm.eclipse.debugger";

    public static final String THREAD_ID = DEBUG_TARGET_ID + ".thread";

    public static final String STACK_FRAME_ID = DEBUG_TARGET_ID + ".frame";

    public static final String VALUE_ID = DEBUG_TARGET_ID + ".value";

    public static final String VARIABLE_ID = DEBUG_TARGET_ID + ".variable";
}
