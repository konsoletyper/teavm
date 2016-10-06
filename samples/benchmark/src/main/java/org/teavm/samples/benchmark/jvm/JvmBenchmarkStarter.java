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
package org.teavm.samples.benchmark.jvm;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.teavm.samples.benchmark.shared.Scene;

public class JvmBenchmarkStarter {
    private static Scene scene = new Scene();
    private static int currentSecond;
    private static long startMillisecond;
    private static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private static JRenderer renderer = new JRenderer();
    private static double timeSpentCalculating;

    private JvmBenchmarkStarter() {
    }

    public static void main(String[] args) {
        startMillisecond = System.currentTimeMillis();
        JFrame window = new JFrame();
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.add(renderer);
        window.setVisible(true);

        EventQueue.invokeLater(() -> {
            window.pack();
            makeStep();
        });
    }

    private static void makeStep() {
        long start = System.nanoTime();
        scene.calculate();
        long end = System.nanoTime();

        int second = (int) ((System.currentTimeMillis() - startMillisecond) / 1000);
        if (second > currentSecond) {
            System.out.println("Computing second " + second + " took " + timeSpentCalculating / 1000000 + " ms");
            currentSecond = second;
            timeSpentCalculating = 0;
        }
        timeSpentCalculating += end - start;
        renderer.repaint();

        executor.schedule(() -> EventQueue.invokeLater(JvmBenchmarkStarter::makeStep),
                scene.timeUntilNextStep(), TimeUnit.MILLISECONDS);
    }

    private static class JRenderer extends JComponent {
        @Override
        public void paint(Graphics g) {
            Graphics2D gfx = (Graphics2D) g;

            gfx.setBackground(Color.white);
            gfx.setPaint(Color.black);
            gfx.clearRect(0, 0, 600, 600);

            AffineTransform originalTransformation = gfx.getTransform();

            gfx.translate(0, 600);
            gfx.scale(1, -1);
            gfx.scale(100, 100);
            gfx.setStroke(new BasicStroke(0.01f));
            for (Body body = scene.getWorld().getBodyList(); body != null; body = body.getNext()) {
                Vec2 center = body.getPosition();

                AffineTransform bodyTransform = gfx.getTransform();
                gfx.translate(center.x, center.y);
                gfx.rotate(body.getAngle());
                for (Fixture fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext()) {
                    Shape shape = fixture.getShape();
                    if (shape.getType() == ShapeType.CIRCLE) {
                        CircleShape circle = (CircleShape) shape;
                        Arc2D arc = new Arc2D.Float(circle.m_p.x - circle.getRadius(),
                                circle.m_p.y - circle.getRadius(), circle.getRadius() * 2, circle.getRadius() * 2,
                                0, 360, Arc2D.CHORD);
                        gfx.draw(arc);
                    } else if (shape.getType() == ShapeType.POLYGON) {
                        PolygonShape poly = (PolygonShape) shape;
                        Vec2[] vertices = poly.getVertices();

                        Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);

                        path.moveTo(vertices[0].x, vertices[0].y);
                        for (int i = 1; i < poly.getVertexCount(); ++i) {
                            path.lineTo(vertices[i].x, vertices[i].y);
                        }
                        path.closePath();
                        gfx.draw(path);
                    }
                }
                gfx.setTransform(bodyTransform);
            }

            gfx.setTransform(originalTransformation);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(600, 600);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(600, 600);
        }
    }
}
