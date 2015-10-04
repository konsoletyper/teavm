/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.jso.browser;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Junji Takakura
 */
public abstract class Storage implements JSObject {
    @JSProperty
    public abstract int getLength();

    public abstract String key(int index);

    public abstract String getItem(String key);

    public abstract void setItem(String key, String value);

    public abstract void removeItem(String key);

    public abstract void clear();

    public static Storage getSessionStorage() {
        return Window.current().getSessionStorage();
    }

    public static Storage getLocalStorage() {
        return Window.current().getLocalStorage();
    }
}
