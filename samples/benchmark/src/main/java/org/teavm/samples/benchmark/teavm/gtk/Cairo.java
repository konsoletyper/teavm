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
package org.teavm.samples.benchmark.teavm.gtk;

import org.teavm.interop.Import;

public final class Cairo {
    private Cairo() {
    }

    @Import(name = "cairo_save")
    public static native void save(Context context);

    @Import(name = "cairo_restore")
    public static native void restore(Context context);

    @Import(name = "cairo_rectangle")
    public static native void rectangle(Context context, double x, double y, double width, double height);

    @Import(name = "cairo_move_to")
    public static native void moveTo(Context context, double x, double y);

    @Import(name = "cairo_line_to")
    public static native void lineTo(Context context, double x, double y);

    @Import(name = "cairo_arc")
    public static native void arc(Context context, double xc, double yc, double radius, double angle1, double angle2);

    @Import(name = "cairo_scale")
    public static native void scale(Context context, double sx, double sy);

    @Import(name = "cairo_translate")
    public static native void translate(Context context, double x, double y);

    @Import(name = "cairo_rotate")
    public static native void rotate(Context context, double angle);

    @Import(name = "cairo_set_line_width")
    public static native void setLineWidth(Context context, double width);

    @Import(name = "cairo_set_source_rgb")
    public static native void setSourceRgb(Context context, double red, double green, double blue);

    @Import(name = "cairo_fill")
    public static native void fill(Context context);

    @Import(name = "cairo_stroke")
    public static native void stroke(Context context);

    public static class Context {
    }
}
