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
 * https://developer.mozilla.org/en-US/docs/Web/API/Event/eventPhase#Event_phase_constants
 */
public enum EventPhase implements UseHTMLValue<Integer> {
  NONE(0),
  CAPTURING_PHASE(1),
  AT_TARGET(2),
  BUBBLING_PHASE(3);

  private final int value;

  EventPhase(int value) {
    this.value = value;
  }

  @Override
  public Integer getValue() {
    return value;
  }

}
