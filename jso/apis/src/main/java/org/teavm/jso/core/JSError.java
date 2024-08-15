/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.jso.core;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

@JSClass(name = "Error")
public class JSError implements JSObject {
    public JSError() {
    }

    public JSError(String message) {
    }

    @JSBody(params = { "tryClause", "catchClause" }, script = ""
            + "try {"
                + "return tryClause();"
            + "} catch (e) {"
                + "return catchClause(e);"
            + "}")
    public static native <T> T catchNative(TryClause<T> tryClause, CatchClause<T> catchClause);

    @Deprecated
    public static boolean isError(JSObject object) {
        return object instanceof JSError;
    }

    @JSProperty
    public native String getStack();

    @JSProperty
    public native String getMessage();

    @JSProperty
    public native String getName();

    @JSFunctor
    public interface TryClause<T> extends JSObject {
        T run();
    }

    @JSFunctor
    public interface CatchClause<T> extends JSObject {
        T accept(JSObject e);
    }
}
