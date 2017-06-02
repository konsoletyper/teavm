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
 * https://developer.mozilla.org/en-US/docs/Web/Events
 */
public enum EventType implements UseHTMLValue<String> {

  ANIMATION_END("animationend"),
  ANIMATION_ITERATION("animationiteration"),
  ANIMATION_START("animationstart"),
  BEFORE_COPY("beforecopy"),
  BEFORE_CUT("beforecut"),
  BEFORE_PASTE("beforepaste"),
  BLUR("blur"),
  CHANGE("change"),
  CLICK("click"),
  CONTEXT_MENU("contextmenu"),
  COPY("copy"),
  CUT("cut"),
  DOUBLE_CLICK("dblclick"),
  DRAG("drag"),
  DRAG_END("dragend"),
  DRAG_ENTER("dragenter"),
  DRAG_LEAVE("dragleave"),
  DRAG_OVER("dragover"),
  DRAG_START("dragstart"),
  DROP("drop"),
  ERROR("error"),
  FOCUS("focus"),
  FOCUS_IN("focusin"),
  FOCUS_OUT("focusout"),
  INPUT("input"),
  INVALID("invalid"),
  KEY_DOWN("keydown"),
  KEY_PRESS("keypress"),
  KEY_UP("keyup"),
  MOUSE_DOWN("mousedown"),
  MOUSE_MOVE("mousemove"),
  MOUSE_OUT("mouseout"),
  MOUSE_OVER("mouseover"),
  MOUSE_ENTER("mouseenter"),
  MOUSE_LEAVE("mouseleave"),
  MOUSE_UP("mouseup"),
  MOUSE_WHEEL("mousewheel"),
  PASTE("paste"),
  POINTER_CANCEL("pointercancel"),
  POINTER_DOWN("pointerdown"),
  POINTER_ENTER("pointerenter"),
  POINTER_LEAVE("pointerleave"),
  POINTER_MOVE("pointermove"),
  POINTER_OUT("pointerout"),
  POINTER_OVER("pointerover"),
  POINTER_UP("pointerup"),
  RESET("reset"),
  RESIZE("resize"),
  SCROLL("scroll"),
  SELECT_START("selectstart"),
  SUBMIT("submit"),
  TOUCH_CANCEL("touchcancel"),
  TOUCH_END("touchend"),
  TOUCH_MOVE("touchmove"),
  TOUCH_START("touchstart"),
  TRANSITION_END("transitionend");

  private final String value;

  EventType(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

}
