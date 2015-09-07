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
package org.teavm.jso;

/**
 * <p>Indicates that method is to have native JavaScript implementation, which can be written as a
 * single JavaScript expression.
 * Method only can take and return primitive values and {@link JSObject}s.
 * JSExpression script can't call Java methods, but you can pass callbacks wrapped into {@link JSFunctor}.
 * Note that unless method is static, it must belong to class that implements {@link JSObject}.
 * If applied to non-native method, original Java body will be overwritten by JavaScript.</p>
 *
 * <p>Use this annotation instead of JSBody when possible, since this can increase performance and
 * reduce generated JavaScript size.</p>
 *
 * @author Alexey Andreev
 */
public @interface JSExpression {
    /**
     * <p>How method parameters are named inside JavaScript implementation.</p>
     */
    String[] params();

    /**
     * <p>JavaScript expression.</p>
     */
    String expr();
}
