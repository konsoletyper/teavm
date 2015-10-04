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
 * <p>Marks abstract member method as a JavaScript method. This is equivalent to the following:</p>
 *
 * <pre>
 * {@literal @}JSBody(params = ..., script = "return new this.methodName(...);")
 * </pre>
 *
 * <p>where <code>methodName</code> is method's name by default or a name, directly specified by
 * this annotation.</p>
 *
 * <p>JSMethod can be avoided. This means that if you define abstract method on overlay class or interface,
 * and don't specify any annotations, this method is treated as marked by JSMethod.</p>
 *
 * @author Alexey Andreev
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JSMethod {
    String value() default "";
}
