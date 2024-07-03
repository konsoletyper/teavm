package org.teavm.jso.dom.events;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface TouchList extends JSObject {
    /**
     * The number of Touch objects in the TouchList.
     */
    @JSProperty
    int getLength();

    /**
     * Returns the Touch object at the specified index in the list.
     *
     * @param index
     */
    Touch item(int index);
}