/*
 *  Copyright 2019 devnewton.
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
package org.teavm.samples.gamepad;

import org.teavm.jso.browser.Navigator;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.gamepad.Gamepad;
import org.teavm.jso.gamepad.GamepadButton;

/**
 *
 * @author devnewton
 */
public final class Application {

    private static final HTMLDocument document = Window.current().getDocument();

    private Application() {
    }

    public static void main(String[] args) {
        refresh(0);
    }

    public static void refresh(double timestamp) {
        StringBuilder sb = new StringBuilder();
        for (Gamepad pad : Navigator.getGamepads()) {
            if (null != pad) {
                sb.append("<p>");
                sb.append("Pad: ").append(pad.getId()).append("<br>");

                sb.append("Axes: ");
                double[] axes = pad.getAxes();
                for (int a = 0; a < axes.length; ++a) {
                    sb.append(axes[a]).append(" ");
                }
                sb.append("<br>");

                sb.append("Buttons pressed: ");
                int buttonNum = 1;
                for (GamepadButton button : pad.getButtons()) {
                    if (button.isPressed()) {
                        sb.append(buttonNum).append(" ");
                    }
                    ++buttonNum;
                }

                sb.append("</p>");
            }
        }
        HTMLElement status = document.getElementById("gamepad-status");
        status.setInnerHTML(sb.toString());
        Window.requestAnimationFrame(Application::refresh);
    }
}
