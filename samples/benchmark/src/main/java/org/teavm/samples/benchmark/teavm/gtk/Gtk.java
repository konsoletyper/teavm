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

import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.interop.c.Include;

@Include("gtk/gtk.h")
public final class Gtk {
    private Gtk() {
    }

    public static class Widget extends GLib.GObject {
    }

    public static class Window extends Container {
    }

    public static class Container extends Widget {
    }

    public static class Button extends Widget {
    }

    @Import(name = "gtk_init")
    public static native void init(Address argc, Address argv);

    @Import(name = "gtk_window_new")
    public static native Window windowNew(int type);

    @Import(name = "gtk_widget_show")
    public static native void show(Widget widget);

    @Import(name = "gtk_main")
    public static native void main();

    @Import(name = "gtk_main_quit")
    public static native void mainQuit();

    @Import(name = "gtk_container_set_border_width")
    public static native void setBorderWidth(Container container, int width);

    @Import(name = "gtk_button_new_with_label")
    public static native Button buttonNewWithLabel(String label);

    @Import(name = "gtk_drawing_area_new")
    public static native Widget drawingAreaNew();

    @Import(name = "gtk_container_add")
    public static native void add(Container container, Widget widget);

    @Import(name = "gtk_widget_set_size_request")
    public static native void setSizeRequest(Widget widget, int width, int height);

    @Import(name = "gtk_widget_get_allocated_width")
    public static native int getAllocatedWidth(Widget widget);

    @Import(name = "gtk_widget_get_allocated_height")
    public static native int getAllocatedHeight(Widget widget);

    @Import(name = "gtk_widget_queue_draw")
    public static native void queueDraw(Widget widget);

    public static final int WINDOW_TOPLEVEL = 0;
    public static final int WINDOW_POPUP = 1;
}
