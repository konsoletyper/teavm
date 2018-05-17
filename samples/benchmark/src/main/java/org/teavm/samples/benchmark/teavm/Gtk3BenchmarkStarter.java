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
package org.teavm.samples.benchmark.teavm;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.teavm.interop.Address;
import org.teavm.interop.Function;
import org.teavm.interop.Import;
import org.teavm.interop.c.Include;
import org.teavm.samples.benchmark.shared.Scene;
import org.teavm.samples.benchmark.teavm.gtk.Cairo;
import org.teavm.samples.benchmark.teavm.gtk.GLib;
import org.teavm.samples.benchmark.teavm.gtk.Gtk;

public final class Gtk3BenchmarkStarter {
    private static Gtk.Widget canvas;
    private static Scene scene = new Scene();
    private static int currentSecond;
    private static long startMillisecond;
    private static long timeSpentCalculating;

    private Gtk3BenchmarkStarter() {
    }

    public static void main(String[] args) {
        Gtk.init(null, null);
        Gtk.Window window = Gtk.windowNew(Gtk.WINDOW_TOPLEVEL);
        GLib.signalConnect(window, "delete-event",
                Function.get(GLib.Callback.class, Gtk3BenchmarkStarter.class, "windowDeleted"), null);
        GLib.signalConnect(window, "destroy",
                Function.get(GLib.Callback.class, Gtk3BenchmarkStarter.class, "destroy"), null);
        Gtk.setBorderWidth(window, 10);

        canvas = Gtk.drawingAreaNew();
        Gtk.setSizeRequest(canvas, 300, 300);
        GLib.signalConnect(canvas, "draw",
                Function.get(GLib.Callback.class, Gtk3BenchmarkStarter.class, "draw"), null);
        Gtk.add(window, canvas);
        Gtk.show(canvas);

        Gtk.show(window);
        GLib.delay(0, Function.get(GLib.TimerFunction.class, Gtk3BenchmarkStarter.class, "tick"), null);

        startMillisecond = System.currentTimeMillis();
        Gtk.main();
    }

    private static boolean windowDeleted(Gtk.Widget widget, Address event, Address data) {
        System.out.println("Closing window");
        return false;
    }

    private static void destroy(Gtk.Widget widget, Address data) {
        Gtk.mainQuit();
    }

    private static void draw(Gtk.Widget widget, Cairo.Context context) {
        int width = Gtk.getAllocatedWidth(widget);
        int height = Gtk.getAllocatedHeight(widget);
        int sz = Math.min(width, height);

        Cairo.setSourceRgb(context, 1, 1, 1);
        Cairo.rectangle(context, 0, 0, width, height);
        Cairo.fill(context);

        Cairo.save(context);
        Cairo.setSourceRgb(context, 0, 0, 0);
        Cairo.translate(context, 0, height);
        Cairo.translate(context, (width - sz) / 2.0, -(height - sz) / 2.0);
        Cairo.scale(context, 1, -1);
        Cairo.scale(context, sz / 6.0, sz / 6.0);
        Cairo.setLineWidth(context, 0.01);
        for (Body body = scene.getWorld().getBodyList(); body != null; body = body.getNext()) {
            Vec2 center = body.getPosition();
            Cairo.save(context);
            Cairo.translate(context, center.x, center.y);
            Cairo.rotate(context, body.getAngle());
            for (Fixture fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext()) {
                Shape shape = fixture.getShape();
                if (shape.getType() == ShapeType.CIRCLE) {
                    CircleShape circle = (CircleShape) shape;
                    Cairo.arc(context, circle.m_p.x, circle.m_p.y, circle.getRadius(), 0, Math.PI * 2);
                    Cairo.stroke(context);
                } else if (shape.getType() == ShapeType.POLYGON) {
                    PolygonShape poly = (PolygonShape) shape;
                    Vec2[] vertices = poly.getVertices();
                    Cairo.moveTo(context, vertices[0].x, vertices[0].y);
                    for (int i = 1; i < poly.getVertexCount(); ++i) {
                        Cairo.lineTo(context, vertices[i].x, vertices[i].y);
                    }
                    Cairo.stroke(context);
                }
            }
            Cairo.restore(context);
        }
        Cairo.restore(context);
    }

    private static int tick() {
        long start = currentTimeNano();
        scene.calculate();
        long end = currentTimeNano();
        int second = (int) ((System.currentTimeMillis() - startMillisecond) / 1000);

        if (second > currentSecond) {
            System.out.println("Second " + second + ": " + (timeSpentCalculating / 1_000_000.0) + " ms");
            timeSpentCalculating = 0;
            currentSecond = second;
        }
        long delta = end - start;
        if (delta < 0) {
            delta += 1_000_000_000;
        }
        timeSpentCalculating += delta;

        Gtk.queueDraw(canvas);
        GLib.delay(scene.timeUntilNextStep(),
                Function.get(GLib.TimerFunction.class, Gtk3BenchmarkStarter.class, "tick"), null);

        return 0;
    }

    @Import(name = "currentTimeNano")
    @Include(value = "support.c", isSystem = false)
    private static native long currentTimeNano();
}
