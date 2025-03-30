package org.teavm.classlib.java.util;

public abstract class TEventListenerProxy<T extends TEventListener> implements TEventListener {

    private final T eventListener;

    public TEventListenerProxy(final T eventListener) {
        this.eventListener = eventListener;
    }

    public T getListener() {
        return eventListener;
    }

}
