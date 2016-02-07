/*
 *  Copyright 2015 Jaroslav Tulach.
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
package org.teavm.samples.benchmark.htmljava;

import com.dukescript.api.canvas.GraphicsContext2D;
import com.dukescript.api.canvas.Style;
import com.dukescript.canvas.html.HTML5Graphics;
import java.util.Timer;
import java.util.TimerTask;
import net.java.html.BrwsrCtx;
import net.java.html.js.JavaScriptBody;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.teavm.samples.benchmark.shared.Scene;

public final class BenchmarkStarter {
    private BenchmarkStarter() {
    }

    private static final Timer TIMER = new Timer("Make Step");
    private static final Scene scene = new Scene();
    private static double startMillisecond;
    private static int currentSecond;
    private static double timeSpentCalculating;
    private static BrwsrCtx ctx;

    public static void main(String[] args) {
        startMillisecond = System.currentTimeMillis();
        ctx = BrwsrCtx.findDefault(BenchmarkStarter.class);
        makeStep();
    }

    static void makeStep() {
        ctx.execute(() -> makeStep0());
    }

    private static void makeStep0() {
        double start = System.currentTimeMillis();
        scene.calculate();
        double end = System.currentTimeMillis();
        int second = (int) ((System.currentTimeMillis() - startMillisecond) / 1000);
        if (second > currentSecond) {
            publishResults(second, timeSpentCalculating);
            timeSpentCalculating = 0;
            currentSecond = second;
        }
        timeSpentCalculating += end - start;
        render();
        TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                makeStep();
            }
        }, scene.timeUntilNextStep());
    }

    private static void render() {
        GraphicsContext2D context = HTML5Graphics.getOrCreate("benchmark-canvas");
        context.setFillStyle(new Style.Color("white"));
        context.setStrokeStyle(new Style.Color("grey"));
        context.fillRect(0, 0, 600, 600);
        context.save();
        context.translate(0, 600);
        context.scale(1, -1);
        context.scale(100, 100);
        context.setLineWidth(0.01);
        for (Body body = scene.getWorld().getBodyList(); body != null; body = body.getNext()) {
            Vec2 center = body.getPosition();
            context.save();
            context.translate(center.x, center.y);
            context.rotate(body.getAngle());
            for (Fixture fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext()) {
                Shape shape = fixture.getShape();
                if (shape.getType() == ShapeType.CIRCLE) {
                    CircleShape circle = (CircleShape) shape;
                    context.beginPath();
                    context.arc(circle.m_p.x, circle.m_p.y, circle.getRadius(), 0, Math.PI * 2, true);
                    context.closePath();
                    context.stroke();
                } else if (shape.getType() == ShapeType.POLYGON) {
                    PolygonShape poly = (PolygonShape) shape;
                    Vec2[] vertices = poly.getVertices();
                    context.beginPath();
                    context.moveTo(vertices[0].x, vertices[0].y);
                    for (int i = 1; i < poly.getVertexCount(); ++i) {
                        context.lineTo(vertices[i].x, vertices[i].y);
                    }
                    context.closePath();
                    context.stroke();
                }
            }
            context.restore();
        }
        context.restore();
    }

    @JavaScriptBody(args = { "second", "timeSpentCalculating" }, body = ""
        + "var resultTableBody = document.getElementById('result-table-body');\n"
        + "var row = document.createElement(\"tr\");\n"
        + "resultTableBody.appendChild(row);\n"
        + "var secondCell = document.createElement(\"td\");\n"
        + "row.appendChild(secondCell);\n"
        + "secondCell.appendChild(document.createTextNode(second));\n"
        + "var timeCell = document.createElement(\"td\");\n"
        + "row.appendChild(timeCell);\n"
        + "timeCell.appendChild(document.createTextNode(timeSpentCalculating));\n"
    )
    private static native void publishResults(int second, double timeSpentCalculating);
}
