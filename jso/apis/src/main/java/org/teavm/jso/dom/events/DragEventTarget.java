/*
 *  Copyright 2015 Alexey Andreev.
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

/**
*
* @author Jan-Felix Wittmann
*/
public interface DragEventTarget extends EventTarget {

    default void listenDrag(EventListener<DragEvent> listener) {
        addEventListener("drag", listener);
    }

    default void neglectDrag(EventListener<DragEvent> listener) {
        removeEventListener("drag", listener);
    }

    default void listenDragStart(EventListener<DragEvent> listener) {
        addEventListener("dragstart", listener);
    }

    default void neglectDragStart(EventListener<DragEvent> listener) {
        removeEventListener("dragstart", listener);
    }

    default void listenDragEnd(EventListener<DragEvent> listener) {
        addEventListener("dragend", listener);
    }

    default void neglectDragEnd(EventListener<DragEvent> listener) {
        removeEventListener("dragend", listener);
    }

    default void listenDragOver(EventListener<DragEvent> listener) {
        addEventListener("dragover", listener);
    }

    default void neglectDragOver(EventListener<DragEvent> listener) {
        removeEventListener("dragover", listener);
    }

    default void listenDragEnter(EventListener<DragEvent> listener) {
        addEventListener("dragenter", listener);
    }

    default void neglectDragEnter(EventListener<DragEvent> listener) {
        removeEventListener("dragenter", listener);
    }

    default void listenDragLeave(EventListener<DragEvent> listener) {
        addEventListener("dragleave", listener);
    }

    default void neglectDragLeave(EventListener<DragEvent> listener) {
        removeEventListener("dragleave", listener);
    }

    default void listenDrop(EventListener<DragEvent> listener) {
        addEventListener("drop", listener);
    }

    default void neglectDrop(EventListener<DragEvent> listener) {
        removeEventListener("drop", listener);
    }
}
