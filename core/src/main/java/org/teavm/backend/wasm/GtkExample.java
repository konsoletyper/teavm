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
package org.teavm.backend.wasm;

import org.teavm.interop.Address;
import org.teavm.interop.Function;
import org.teavm.interop.Import;
import org.teavm.interop.Structure;
import org.teavm.interop.c.Include;

public final class GtkExample {
    private GtkExample() {
    }

    public static void main(String[] args) {
        Gtk.init(null, null);
        Gtk.Window window = Gtk.windowNew(Gtk.WINDOW_TOPLEVEL);
        GLib.signalConnect(window, "delete-event",
                Function.get(GLib.Callback.class, GtkExample.class, "windowDeleted"), null);
        GLib.signalConnect(window, "destroy",
                Function.get(GLib.Callback.class, GtkExample.class, "destroy"), null);
        Gtk.setBorderWidth(window, 10);

        Gtk.Button button = Gtk.buttonNewWithLabel("Hello, world");
        GLib.signalConnect(button, "clicked", Function.get(GLib.Callback.class, GtkExample.class, "hello"), null);
        Gtk.add(window, button);
        Gtk.show(button);

        Gtk.show(window);
        Gtk.main();
    }

    private static void hello(Gtk.Widget widget, Address data) {
        System.out.println("Hello, world!");
    }

    private static boolean windowDeleted(Gtk.Widget widget, Address event, Address data) {
        System.out.println("System event occurred");
        return false;
    }

    private static void destroy(Gtk.Widget widget, Address data) {
        Gtk.mainQuit();
    }

    @Include("gtk/gtk.h")
    static class Gtk {
        static class Widget extends GLib.GObject {
        }

        static class Window extends Container {
        }

        static class Container extends Widget {
        }

        static class Button extends Widget {
        }

        @Import(name = "gtk_init")
        static native void init(Address argc, Address argv);

        @Import(name = "gtk_window_new")
        static native Window windowNew(int type);

        @Import(name = "gtk_widget_show")
        static native void show(Widget widget);

        @Import(name = "gtk_main")
        static native void main();

        @Import(name = "gtk_main_quit")
        static native void mainQuit();

        @Import(name = "gtk_container_set_border_width")
        static native void setBorderWidth(Container container, int width);

        @Import(name = "gtk_button_new_with_label")
        static native Button buttonNewWithLabel(String label);

        @Import(name = "gtk_container_add")
        static native void add(Container container, Widget widget);

        static final int WINDOW_TOPLEVEL = 0;
        static final int WINDOW_POPUP = 1;
    }

    static class GLib {
        static class GObject extends Structure {
        }

        @Import(name = "g_signal_connect")
        static native long signalConnect(GObject instance, String signalName, Callback callback, Address data);

        static abstract class Callback extends Function {
            abstract void call();
        }
    }
}
