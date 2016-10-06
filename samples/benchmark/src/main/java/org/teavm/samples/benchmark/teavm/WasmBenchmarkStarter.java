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

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.teavm.interop.Export;
import org.teavm.interop.Import;
import org.teavm.samples.benchmark.shared.Scene;

public final class WasmBenchmarkStarter {
    private static Scene scene = new Scene();
    private static int currentSecond;
    private static long startMillisecond;
    private static double timeSpentCalculating;

    private WasmBenchmarkStarter() {
    }

    public static void main(String[] args) {
        startMillisecond = System.currentTimeMillis();
        tick();
    }

    @Export(name = "tick")
    public static void tick() {
        double start = performanceTime();
        scene.calculate();
        double end = performanceTime();
        int second = (int) ((System.currentTimeMillis() - startMillisecond) / 1000);
        if (second > currentSecond) {
            reportPerformance(second, (int) timeSpentCalculating);
            timeSpentCalculating = 0;
            currentSecond = second;
        }
        timeSpentCalculating += end - start;
        render();
        repeatAfter(scene.timeUntilNextStep());
    }

    private static void render() {
        WasmCanvas.save();
        setupCanvas();
        for (Body body = scene.getWorld().getBodyList(); body != null; body = body.getNext()) {
            Vec2 center = body.getPosition();
            WasmCanvas.save();
            WasmCanvas.translate(center.x, center.y);
            WasmCanvas.rotate(body.getAngle());
            for (Fixture fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext()) {
                Shape shape = fixture.getShape();
                if (shape.getType() == ShapeType.CIRCLE) {
                    CircleShape circle = (CircleShape) shape;
                    WasmCanvas.beginPath();
                    WasmCanvas.arc(circle.m_p.x, circle.m_p.y, circle.getRadius(), 0, Math.PI * 2, true);
                    WasmCanvas.closePath();
                    WasmCanvas.stroke();
                } else if (shape.getType() == ShapeType.POLYGON) {
                    PolygonShape poly = (PolygonShape) shape;
                    Vec2[] vertices = poly.getVertices();
                    WasmCanvas.beginPath();
                    WasmCanvas.moveTo(vertices[0].x, vertices[0].y);
                    for (int i = 1; i < poly.getVertexCount(); ++i) {
                        WasmCanvas.lineTo(vertices[i].x, vertices[i].y);
                    }
                    WasmCanvas.closePath();
                    WasmCanvas.stroke();
                }
            }
            WasmCanvas.restore();
        }
        WasmCanvas.restore();
    }

    @Import(module = "benchmark", name = "setupCanvas")
    private static native void setupCanvas();

    @Import(module = "benchmark", name = "performanceTime")
    private static native double performanceTime();

    @Import(module = "benchmark", name = "reportPerformance")
    private static native void reportPerformance(int second, int timeSpentCalculating);

    @Import(module = "benchmark", name = "repeatAfter")
    private static native void repeatAfter(int seconds);
}
