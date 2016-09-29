/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.samples.benchmark.teavm;

import org.teavm.interop.Import;
import org.teavm.interop.StaticInit;

@StaticInit
public final class WasmCanvas {
    private WasmCanvas() {
    }

    @Import(module = "canvas", name = "save")
    public static native void save();

    @Import(module = "canvas", name = "restore")
    public static native void restore();

    @Import(module = "canvas", name = "translate")
    public static native void translate(double x, double y);

    @Import(module = "canvas", name = "rotate")
    public static native void rotate(double angle);

    @Import(module = "canvas", name = "beginPath")
    public static native void beginPath();

    @Import(module = "canvas", name = "closePath")
    public static native void closePath();

    @Import(module = "canvas", name = "stroke")
    public static native void stroke();

    @Import(module = "canvas", name = "arc")
    public static native void arc(double cx, double cy, double radius, double startAngle, double endAngle,
            boolean counterClockwise);

    @Import(module = "canvas", name = "moveTo")
    public static native void moveTo(double x, double y);

    @Import(module = "canvas", name = "lineTo")
    public static native void lineTo(double x, double y);
}
