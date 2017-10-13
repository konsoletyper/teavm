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
package org.teavm.jso.dom.events;

import org.teavm.jso.dom.html.use.UseHTMLValue;

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/Document/createEvent#Notes
 */
public enum EventClass implements UseHTMLValue<String> {
  CUSTOM("CustomEvent"),
  KEYBOARD("KeyboardEvent"),
  MESSAGE("MessageEvent"),
  MOUSE("MouseEvent"),
  MUTATION("MutationEvent"),
  OVERFLOW("OverflowEvent"),
  PAGE_TRANSITION("PageTransitionEvent"),
  PROGRESS("ProgressEvent"),
  STORAGE("StorageEvent"),
  TEXT("TextEvent"),
  UI("UIEvent"),
  ANIMATION("AnimationEvent"),
  TRANSITION("TransitionEvent"),
  WHEEL("WheelEvent"),
  SVG("SVGEvents"),
  SVG_ZOOM("SVGZoomEvents"),
  TOUCH("TouchEvent");

  private final String value;

  EventClass(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

}
