package org.teavm.chromerpd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.debugging.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ChromeRDPDebugger implements JavaScriptDebugger {
    private ChromeRDPDebuggerEndpoint endpoint;
    private List<JavaScriptDebuggerListener> listeners = new ArrayList<>();
    private Set<RDPBreakpoint> breakpoints = new HashSet<>();

    void setEndpoint(ChromeRDPDebuggerEndpoint endpoint) {
        if (this.endpoint == endpoint) {
            return;
        }
        this.endpoint = endpoint;
        if (endpoint != null) {
            for (RDPBreakpoint breakpoint : breakpoints) {
                endpoint.updateBreakpoint(breakpoint);
            }
            for (JavaScriptDebuggerListener listener : listeners) {
                listener.attached();
            }
        } else {
            for (JavaScriptDebuggerListener listener : listeners) {
                listener.detached();
            }
        }
    }

    @Override
    public void addListener(JavaScriptDebuggerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(JavaScriptDebuggerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void suspend() {
        if (endpoint != null) {
            endpoint.suspend();
        }
    }

    @Override
    public void resume() {
        if (endpoint != null) {
            endpoint.resume();
        }
    }

    @Override
    public void stepInto() {
        if (endpoint != null) {
            endpoint.stepInto();
        }
    }

    @Override
    public void stepOut() {
        if (endpoint != null) {
            endpoint.stepOut();
        }
    }

    @Override
    public void stepOver() {
        if (endpoint != null) {
            endpoint.stepOver();
        }
    }

    @Override
    public void continueToLocation(JavaScriptLocation location) {
        if (endpoint != null) {
            endpoint.continueToLocation(location);
        }
    }

    @Override
    public boolean isSuspended() {
        return endpoint != null && endpoint.isSuspended();
    }

    @Override
    public boolean isAttached() {
        return endpoint != null;
    }

    @Override
    public JavaScriptCallFrame[] getCallStack() {
        return endpoint != null ? endpoint.getCallStack() : null;
    }

    @Override
    public JavaScriptBreakpoint getCurrentBreakpoint() {
        return endpoint != null ? endpoint.getCurrentBreakpoint() : null;
    }

    @Override
    public JavaScriptBreakpoint createBreakpoint(JavaScriptLocation location) {
        RDPBreakpoint breakpoint = new RDPBreakpoint(this, location);
        breakpoints.add(breakpoint);
        if (endpoint != null) {
            endpoint.updateBreakpoint(breakpoint);
        }
        return breakpoint;
    }

    void destroyBreakpoint(RDPBreakpoint breakpoint) {
        breakpoints.remove(breakpoint);
        if (endpoint != null) {
            endpoint.destroyBreakpoint(breakpoint);
        }
    }

    void fireResumed() {
        for (JavaScriptDebuggerListener listener : listeners) {
            listener.resumed();
        }
    }

    void firePaused() {
        for (JavaScriptDebuggerListener listener : listeners) {
            listener.paused();
        }
    }

    void fireScriptAdded(String script) {
        for (JavaScriptDebuggerListener listener : listeners) {
            listener.scriptAdded(script);
        }
    }
}
