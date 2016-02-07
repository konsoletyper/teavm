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
package org.teavm.samples.benchmark.gwt;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.teavm.samples.benchmark.shared.Scene;

public class BenchmarkStarter implements EntryPoint {
    private Document document;
    private CanvasElement canvas;
    private Element resultTableBody;
    private Scene scene = new Scene();
    private static int currentSecond;
    private static long startMillisecond;
    private static double timeSpentCalculating;

    @Override
    public void onModuleLoad() {
        document = RootPanel.getBodyElement().getOwnerDocument();
        canvas = (CanvasElement) document.getElementById("benchmark-canvas");
        resultTableBody = document.getElementById("result-table-body");
        startMillisecond = System.currentTimeMillis();
        makeStep();
    }

    private void makeStep() {
        double start = Performance.now();
        scene.calculate();
        double end = Performance.now();
        int second = (int) ((System.currentTimeMillis() - startMillisecond) / 1000);
        if (second > currentSecond) {
            Element row = document.createElement("tr");
            resultTableBody.appendChild(row);
            Element secondCell = document.createElement("td");
            row.appendChild(secondCell);
            secondCell.appendChild(document.createTextNode(String.valueOf(second)));
            Element timeCell = document.createElement("td");
            row.appendChild(timeCell);
            timeCell.appendChild(document.createTextNode(String.valueOf(timeSpentCalculating)));

            timeSpentCalculating = 0;
            currentSecond = second;
        }
        timeSpentCalculating += end - start;
        render();
        new Timer() {
            @Override public void run() {
                makeStep();
            }
        }.schedule(scene.timeUntilNextStep());
    }

    private void render() {
        Context2d context = canvas.getContext2d();
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
