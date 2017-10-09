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
package org.teavm.jso.canvas;

import org.teavm.jso.JSObject;

public interface CanvasGradient extends JSObject {
    /**
     * <p>The CanvasGradient.addColorStop() method adds a new stop, defined by an
     * offset and a color, to the gradient. If the offset is not between 0 and 1,
     * an INDEX_SIZE_ERR is raised, if the color can't be parsed as a CSS color,
     * a SYNTAX_ERR is raised.</p>
     *
     * @param offset Offset between 0 and 1
     * @param color A CSS parseable color.
     */
    void addColorStop(double offset, String color);
}
