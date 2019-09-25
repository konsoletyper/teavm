/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.backend.c.util;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public final class GCVisualizer {
    private static final int WIDTH = 1024;
    private static final int LINE_HEIGHT = 4;
    private static final int GAP_SIZE = 1;
    private static final int GC_GAP_SIZE = 3;

    private GCVisualizer() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Two arguments (input, output) expected");
            System.exit(1);
        }

        List<Line> lines = readData(args[0]);
        BufferedImage bitmap = createBitmap(lines);
        writeBitmap(bitmap, args[1]);
    }

    private static List<Line> readData(String fileName) throws IOException {
        List<Line> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName),
                StandardCharsets.UTF_8))) {
            while (true) {
                String fileLine = reader.readLine();
                if (fileLine == null) {
                    break;
                }

                int charIndex = fileLine.indexOf(':');
                if (charIndex < 0) {
                    continue;
                }

                String kind = fileLine.substring(0, charIndex);
                if (kind.equals("start") || lines.isEmpty()) {
                    lines.add(new Line());
                }
                Line line = lines.get(lines.size() - 1);

                int[] array;
                switch (kind) {
                    case "start":
                        array = line.start;
                        break;
                    case "sweep":
                        array = line.sweep;
                        break;
                    case "defrag":
                        array = line.defrag;
                        break;
                    default:
                        continue;
                }

                charIndex = fileLine.indexOf(' ', charIndex);
                if (charIndex < 0) {
                    continue;
                }
                charIndex++;

                for (int i = 0; i < array.length; ++i) {
                    int next = fileLine.indexOf(' ', charIndex);
                    if (next < 0) {
                        next = fileLine.length();
                    }

                    try {
                        array[i] = Integer.parseInt(fileLine.substring(charIndex, next));
                    } catch (NumberFormatException e) {
                        // do nothing
                    }

                    if (next == fileLine.length()) {
                        break;
                    }
                    charIndex = next + 1;
                }
            }
        }

        return lines;
    }

    private static BufferedImage createBitmap(List<Line> lines) {
        int height = lines.size() * (3 * (GAP_SIZE + LINE_HEIGHT) + GC_GAP_SIZE);
        int[] data = new int[WIDTH * height];

        int offset = 0;
        for (Line line : lines) {
            offset = renderComponent(data, line.start, offset, 0, 0, 255);
            offset = renderComponent(data, line.sweep, offset, 0, 255, 0);
            offset = renderComponent(data, line.defrag, offset, 255, 0, 0);
            renderLine(data, offset, GC_GAP_SIZE, 0, 0, 0);
            offset += WIDTH * GC_GAP_SIZE;
        }

        BufferedImage image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, WIDTH, height, data, 0, WIDTH);
        return image;
    }

    private static int renderComponent(int[] data, int[] array, int offset, int r, int g, int b) {
        renderArray(data, array, offset, r, g, b);
        offset += LINE_HEIGHT * WIDTH;
        renderLine(data, offset, GAP_SIZE, 0, 0, 0);
        offset += GAP_SIZE * WIDTH;
        return offset;
    }

    private static void renderArray(int[] data, int[] array, int offset, int r, int g, int b) {
        for (int i = 0; i < WIDTH; ++i) {
            int start = array.length * i / WIDTH;
            int end = array.length * (i + 1) / WIDTH;
            int total = 0;
            for (int j = start; j < end; ++j) {
                total += array[j];
            }
            double rate = total / (4096.0 * (end - start));
            int pixelR = makeColorComponent(r, rate);
            int pixelG = makeColorComponent(g, rate);
            int pixelB = makeColorComponent(b, rate);

            int pixelOffset = offset;
            int pixel = (255 << 24) | ((pixelR & 255) << 16) | ((pixelG & 255) << 8) | (pixelB & 255);
            for (int j = 0; j < LINE_HEIGHT; ++j) {
                data[pixelOffset] = pixel;
                pixelOffset += WIDTH;
            }
            offset++;
        }
    }

    private static void renderLine(int[] data, int offset, int height, int r, int g, int b) {
        int count = height * WIDTH;
        int pixel = (255 << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
        for (int i = 0; i < count; ++i) {
            data[offset++] = pixel;
        }
    }

    private static int makeColorComponent(int c, double rate) {
        int r = (int) (c * rate + 255 * (1 - rate));
        return Math.min(Math.max(r, 0), 255);
    }

    private static void writeBitmap(BufferedImage image, String fileName) throws IOException {
        ImageIO.write(image, "png", new File(fileName));
    }

    static class Line {
        int[] start = new int[4096];
        int[] sweep = new int[4096];
        int[] defrag = new int[4096];
    }
}
