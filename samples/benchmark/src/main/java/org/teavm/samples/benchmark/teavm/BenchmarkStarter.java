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
package org.teavm.samples.benchmark.teavm;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.teavm.jso.browser.Performance;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.samples.benchmark.shared.Scene;

public final class BenchmarkStarter {
    private static HTMLDocument document = Window.current().getDocument();
    private static HTMLCanvasElement canvas = (HTMLCanvasElement) document.getElementById("benchmark-canvas");
    private static HTMLElement resultTableBody = document.getElementById("result-table-body");
    private static HTMLInputElement displayAnimationCheckbox =
            document.getElementById("display-animation-checkbox").cast();
    private static HTMLElement averageTime = document.getElementById("average-time");
    private static Scene scene = new Scene();
    private static int currentSecond;
    private static long startMillisecond;
    private static double timeSpentCalculating;
    private static double totalTime;

    private BenchmarkStarter() {
    }

    public static void main(String[] args) {
        startMillisecond = System.currentTimeMillis();
        makeStep();
    }

    private static void makeStep() {
        double start = Performance.now();
        scene.calculate();
        double end = Performance.now();
        int second = (int) ((System.currentTimeMillis() - startMillisecond) / 1000);
        if (second > currentSecond) {
            HTMLElement row = document.createElement("tr");
            resultTableBody.appendChild(row);
            HTMLElement secondCell = document.createElement("td");
            row.appendChild(secondCell);
            secondCell.appendChild(document.createTextNode(String.valueOf(second)));
            HTMLElement timeCell = document.createElement("td");
            row.appendChild(timeCell);
            timeCell.appendChild(document.createTextNode(String.valueOf(timeSpentCalculating)));

            totalTime += timeSpentCalculating;
            timeSpentCalculating = 0;
            currentSecond = second;

            averageTime.withText(String.valueOf(Math.round(totalTime / second)));
        }
        timeSpentCalculating += end - start;

        if (displayAnimationCheckbox.isChecked()) {
            render();
        }

        Window.setTimeout(() -> makeStep(), scene.timeUntilNextStep());
    }

    private static void render() {
        CanvasRenderingContext2D context = (CanvasRenderingContext2D) canvas.getContext("2d");
        context.setFillStyle("white");
        context.setStrokeStyle("grey");
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
}
