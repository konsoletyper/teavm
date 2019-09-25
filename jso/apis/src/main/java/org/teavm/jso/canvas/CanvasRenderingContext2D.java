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
package org.teavm.jso.canvas;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.xml.Element;

public interface CanvasRenderingContext2D extends JSObject {
    // Path

    void beginPath();

    void closePath();

    void arc(double x, double y, double radius, double startAngle, double endAngle, boolean anticlockwise);

    void arc(double x, double y, double radius, double startAngle, double endAngle);

    void arcTo(double x1, double y1, double x2, double y2, double radius);

    void bezierCurveTo(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y);

    void clearRect(double x, double y, double width, double height);

    void moveTo(double x, double y);

    void lineTo(double x, double y);

    boolean isPointInPath(double x, double y);

    boolean isPointInStroke(double x, double y);

    void quadraticCurveTo(double cpx, double cpy, double x, double y);

    void rect(double x, double y, double width, double height);

    void scrollPathIntoView();

    // Clip

    void clip();

    // Creating images, gradients and patterns

    ImageData createImageData(double width, double height);

    CanvasGradient createLinearGradient(double x0, double y0, double x1, double y1);

    CanvasPattern createPattern(CanvasImageSource image, String repetition);

    CanvasGradient createRadialGradient(double x0, double y0, double r0, double x1, double y1, double r1);

    // Drawing images

    void drawImage(CanvasImageSource image, double dx, double dy);

    void drawImage(CanvasImageSource image, double dx, double dy, double dw, double dh);

    void drawImage(CanvasImageSource image, double sx, double sy, double sw, double sh, double dx, double dy,
            double dw, double dh);

    // Focus ring

    boolean drawCustomFocusRing(Element element);

    void drawSystemFocusRing(Element element);

    // Line dash

    JSArrayReader<JSObject> getLineDash();

    void setLineDash(JSArray<JSObject> lineDash);

    // Image data

    void putImageData(ImageData imagedata, double dx, double dy, double dirtyX, double dirtyY,
            double dirtyWidth, double dirtyHeight);

    void putImageData(ImageData imagedata, double dx, double dy);

    ImageData getImageData(double x, double y, double width, double height);

    // Text

    TextMetrics measureText(String text);

    // Fill

    void fill();

    void fillRect(double x, double y, double width, double height);

    void fillText(String text, double x, double y, double maxWidth);

    void fillText(String text, double x, double y);

    // Sroke

    void stroke();

    void strokeRect(double x, double y, double w, double h);

    void strokeText(String text, double x, double y, double maxWidth);

    void strokeText(String text, double x, double y);

    // Transformation

    void setTransform(double m11, double m12, double m21, double m22, double dx, double dy);

    void transform(double m11, double m12, double m21, double m22, double dx, double dy);

    void translate(double x, double y);

    void rotate(double angle);

    void scale(double x, double y);

    // Save and restore

    void save();

    void restore();

    // Fill properties

    @JSProperty
    JSObject getFillStyle();

    @JSProperty
    void setFillStyle(String fillStyle);

    @JSProperty
    void setFillStyle(CanvasGradient gradient);

    @JSProperty
    void setFillStyle(CanvasPattern pattern);

    // Line properties

    @JSProperty
    String getLineCap();

    @JSProperty
    void setLineCap(String lineCap);

    @JSProperty
    double getLineDashOffset();

    @JSProperty
    void setLineDashOffset(double lineDashOffset);

    @JSProperty
    String getLineJoin();

    @JSProperty
    void setLineJoin(String lineJoin);

    @JSProperty
    double getLineWidth();

    @JSProperty
    void setLineWidth(double lineWidth);

    @JSProperty
    double getMiterLimit();

    @JSProperty
    void setMiterLimit(double miterLimit);

    @JSProperty
    JSObject getStrokeStyle();

    @JSProperty
    void setStrokeStyle(String fillStyle);

    @JSProperty
    void setStrokeStyle(CanvasGradient gradient);

    @JSProperty
    void setStrokeStyle(CanvasPattern pattern);

    // Alpha composite options

    @JSProperty
    double getGlobalAlpha();

    @JSProperty
    void setGlobalAlpha(double globalAlpha);

    @JSProperty
    String getGlobalCompositeOperation();

    @JSProperty
    void setGlobalCompositeOperation(String operation);

    // Shadow properties

    @JSProperty
    double getShadowBlur();

    @JSProperty
    void setShadowBlur(double shadowBlur);

    @JSProperty
    String getShadowColor();

    @JSProperty
    void setShadowColor(String shadowColor);

    @JSProperty
    double getShadowOffsetX();

    @JSProperty
    void setShadowOffsetX(double offsetX);

    @JSProperty
    double getShadowOffsetY();

    @JSProperty
    void setShadowOffsetY(double offsetY);

    // Text properties

    @JSProperty
    String getFont();

    @JSProperty
    void setFont(String font);

    @JSProperty
    String getTextAlign();

    @JSProperty
    void setTextAlign(String textAlign);

    @JSProperty
    String getTextBaseline();

    @JSProperty
    void setTextBaseline(String textBaseline);

    // Misc.

    @JSProperty
    HTMLCanvasElement getCanvas();
}
