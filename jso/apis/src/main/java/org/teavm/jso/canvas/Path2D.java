/*
 *  Copyright 2022 ihromant.
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

import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;

@JSClass
public class Path2D implements JSObject {
    public Path2D() {
    }

    public Path2D(Path2D path) {
    }

    public Path2D(String svg) {
    }

    @JSBody(script = "return new Path2D();")
    @NoSideEffects
    @Deprecated
    public static native Path2D create();

    @JSBody(params = "path", script = "return new Path2D(path);")
    @NoSideEffects
    @Deprecated
    public static native Path2D create(Path2D path);

    @JSBody(params = "svg", script = "return new Path2D(svg);")
    @NoSideEffects
    @Deprecated
    public static native Path2D create(String svg);

    public native void addPath(Path2D path);

    public native void closePath();

    public native void moveTo(double x, double y);

    public native void lineTo(double x, double y);

    public native void bezierCurveTo(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y);

    public native void quadraticCurveTo(double cpx, double cpy, double x, double y);

    public native void arc(double x, double y, double radius, double startAngle, double endAngle);

    public native void arc(double x, double y, double radius, double startAngle, double endAngle,
            boolean counterclockwise);

    public native void arcTo(double x1, double y1, double x2, double y2, double radius);

    public native void ellipse(double x, double y, double radiusX, double radiusY, double rotation,
            double startAngle, double endAngle);

    public native void ellipse(double x, double y, double radiusX, double radiusY, double rotation,
            double startAngle, double endAngle, boolean counterclockwise);

    public native void rect(double x, double y, double width, double height);

    public native void roundRect(double x, double y, double width, double height, JSObject radii);
}
