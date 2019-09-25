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
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public abstract class JSError implements JSObject {
    @JSBody(params = { "tryClause", "catchClause" }, script = ""
            + "try {"
                + "return tryClause();"
            + "} catch (e) {"
                + "return catchClause(e);"
            + "}")
    public static native <T extends JSObject> T catchNative(TryClause<T> tryClause, CatchClause<T> catchClause);

    @JSBody(params = "object", script = "return object instanceof Error;")
    public static native boolean isError(JSObject object);

    @JSProperty
    public abstract String getStack();

    @JSProperty
    public abstract String getMessage();

    @JSProperty
    public abstract String getName();

    @JSFunctor
    public interface TryClause<T extends JSObject> extends JSObject {
        T run();
    }

    @JSFunctor
    public interface CatchClause<T extends JSObject> extends JSObject {
        T accept(JSObject e);
    }
}
