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
 * <p>Marks abstract member method as either a getter or a setter.</p>
 *
 * <p>Getter's name must conform the Java Beans specification, i.e. start with <code>get</code> prefix
 * (or <code>is</code> in case of boolean getter). It must not take any parameters and must return a value.
 * For getter annotation is equivalent to the following:</p>
 *
 * <pre>
 * {@literal @}JSBody(params = {}, script = "return this.propertyName;")
 * </pre>
 *
 * <p>Setter's name must conform the Java Beans specification, i.e. start with <code>set</code> prefix
 * It must take exactly one parameter and must not return a value.
 * For setter annotation is equivalent to the following:</p>
 *
 * <pre>
 * {@literal @}JSBody(params = "value", script = "this.propertyName = value;")
 * </pre>
 *
 * <p>By default <code>propertyName</code> is calculated from method's name according to Java Beans specification,
 * otherwise the name specified by annotation is taken.</p>
 *
 * @author Alexey Andreev
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JSProperty {
    String value() default "";
}
