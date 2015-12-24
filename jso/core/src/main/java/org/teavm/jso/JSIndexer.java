/*
 *  Copyright 2014 Alexey Andreev.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marks abstract member method either as an getter indexer or setter indexer.</p>
 *
 * <p>Getter indexer is a method that returns value and takes exactly one parameter. In
 * this case annotation is equivalent to this:</p>
 *
 * <pre>
 * {@literal @}JSBody(params = "index", script = "return this[index];")
 * </pre>
 *
 * <p>Setter indexer is a method that takes two parameter and does not return any value.
 * Ins this case annotation is equivalent to the following:</p>
 *
 * <pre>
 * {@literal @}JSBody(params = { "index", "value" }, script = "this[index] = value;")
 * </pre>
 *
 * @author Alexey Andreev
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JSIndexer {
}
