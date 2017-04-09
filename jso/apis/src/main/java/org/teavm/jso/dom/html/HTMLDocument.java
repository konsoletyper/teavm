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
package org.teavm.jso.dom.html;

import java.util.function.Consumer;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.css.StyleSheetList;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventClass;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.html.use.UseHTMLHRef;
import org.teavm.jso.dom.html.use.UseHTMLValue;
import org.teavm.jso.dom.range.Range;
import org.teavm.jso.dom.xml.DOMImplementation;
import org.teavm.jso.dom.xml.Document;
import org.teavm.jso.dom.xml.DocumentDesignMode;
import org.teavm.jso.dom.xml.DocumentDirection;
import org.teavm.jso.dom.xml.DocumentReadyState;
import org.teavm.jso.dom.xml.DocumentType;
import org.teavm.jso.dom.xml.Node;
import org.teavm.jso.dom.xml.NodeFilter;
import org.teavm.jso.dom.xml.NodeIterator;
import org.teavm.jso.dom.xml.NodeList;
import org.teavm.jso.dom.xml.Selection;

/**
 * @author Alexey Andreev
 *         <p>
 *         https://developer.mozilla.org/en-US/docs/Web/API/Document
 */
public interface HTMLDocument extends Document, EventTarget {
    @JSProperty
    @Override
    HTMLHtmlElement getDocumentElement();

    @Override
    HTMLElement createElement(String tagName);

    default HTMLElement createElement(String tagName, Consumer<HTMLElement> consumer) {
        HTMLElement result = createElement(tagName);
        consumer.accept(result);
        return result;
    }

    @Override
    HTMLElement getElementById(String elementId);

    @JSMethod
    <E extends HTMLElement> HTMLCollection<E> getElementsByClassName(String className);

    default <E extends HTMLElement> HTMLCollection<E> getElementsByTagName(TagName tagName) {
        return innerGetElementsByTagName(UseHTMLValue.getHtmlValue(tagName));
    }

    @JSMethod("getElementsByTagName")
    <E extends HTMLElement> HTMLCollection<E> innerGetElementsByTagName(String tagName);

    default <E extends HTMLElement> HTMLCollection<E> getElementsByTagNameNS(String namespaceURI, TagName tagName) {
        return innerGetElementsByTagNameNS(namespaceURI, UseHTMLValue.getHtmlValue(tagName));
    }

    @JSMethod("getElementsByTagNameNS")
    <E extends HTMLElement> HTMLCollection<E> innerGetElementsByTagNameNS(String namespaceURI, String tagName);

    @JSProperty
    HTMLBodyElement getBody();

    @JSProperty
    void setBody(HTMLBodyElement body);

    @JSProperty
    HTMLElement getHead();

    @JSProperty
    int getScrollLeft();

    @JSProperty
    int getScrollTop();

    static HTMLDocument current() {
        return Window.current().getDocument();
    }

    @JSProperty
    String getURL();

    @JSProperty
    <E extends HTMLElement> E getActiveElement();

    @JSProperty
    String getBgColor();

    @JSProperty
    void setBgColor(String bgColor);

    @JSProperty
    String getCharacterSet();

    @JSProperty
    String getCompatMode();

    @JSProperty
    String getCookie();

    @JSProperty
    void setCookie(String cookie);

    @JSProperty
    Window getDefaultView();

    default DocumentDesignMode getDesignMode() {
        return UseHTMLValue.toEnumValue(DocumentDesignMode.class, innerGetDesignMode());
    }

    @JSBody(script = "return this.designMode;")
    String innerGetDesignMode();

    default void setDesignMode(DocumentDesignMode designMode) {
        innerSetDesignMode(UseHTMLValue.getHtmlValue(designMode));
    }

    @JSBody(params = "value", script = "this.designMode = value;")
    void innerSetDesignMode(String designMode);

    default DocumentDirection getDir() {
        return UseHTMLValue.toEnumValue(DocumentDirection.class, innerGetDir());
    }

    @JSBody(script = "return this.dir;")
    String innerGetDir();

    default void setDir(DocumentDirection dir) {
        innerSetDir(UseHTMLValue.getHtmlValue(dir));
    }

    @JSBody(params = "value", script = "this.dir = value;")
    void innerSetDir(String dir);

    @JSBody(script = "return this.docType;")
    DocumentType getDocType();

    @JSProperty
    String getDocumentURI();

    @JSProperty
    void setDocumentURI(String documentURI);

    @JSProperty
    String getDomain();

    @JSProperty
    void setDomain(String domain);

    @JSProperty
    HTMLCollection<HTMLEmbedElement> getEmbeds();

    @JSProperty
    String getFgColor();

    @JSProperty
    void setFgColor(String fgColor);

    @JSProperty
    HTMLCollection<HTMLFormElement> getForms();

    @JSProperty
    int getHeight();

    @JSProperty
    HTMLCollection<HTMLImageElement> getImages();

    @Override
    @JSProperty
    DOMImplementation getImplementation();

    @JSProperty
    String getInputEncoding();

    @JSProperty
    String getLastModified();

    @JSProperty
    String getLinkColor();

    @JSProperty
    void setLinkColor(String linkColor);

    @JSProperty
    <E extends HTMLElement & UseHTMLHRef> HTMLCollection<E> getLinks();

    @JSProperty
    Location getLocation();

    @JSProperty
    void setLocation(Location location);

    @JSProperty
    HTMLCollection<HTMLEmbedElement> getPlugins();

    @JSProperty
    String getPreferredStylesheetSet();

    default DocumentReadyState getReadyState() {
        return UseHTMLValue.toEnumValue(DocumentReadyState.class, innerGetReadyState());
    }

    @JSBody(script = "return this.readyState;")
    String innerGetReadyState();

    @JSProperty
    String getReferrer();

    @JSProperty
    HTMLCollection<HTMLScriptElement> getScripts();

    @JSProperty
    String getSelectedStylesheetSet();

    @JSProperty
    void setSelectedStylesheetSet(String selectedStylesheetSet);

    @JSProperty
    StyleSheetList getStyleSheets();

    @JSProperty
    String getTitle();

    @JSProperty
    void setTitle(String title);

    @JSProperty("vlinkColor")
    String getVLinkColor();

    @JSProperty("vlinkColor")
    void setVLinkColor(String vLinkColor);

    @JSProperty
    boolean isHidden();

    @JSProperty
    String getVisibilityState();

    @JSProperty
    int getWidth();

    @JSProperty("onafterscriptexecute")
    EventListener<Event> getOnAfterScriptExecute();

    @JSProperty("onbeforescriptexecute")
    EventListener<Event> getOnBeforeScriptExecute();

    @JSProperty("oncopy")
    EventListener<Event> getOnCopy();

    @JSProperty("oncut")
    EventListener<Event> getOnCut();

    @JSProperty("onpaste")
    EventListener<Event> getOnPaste();

    @JSProperty("onpointerlockchange")
    EventListener<Event> getOnPointerLockChange();

    @JSProperty("onpointerlockerror")
    EventListener<Event> getOnPointerLockError();

    @JSProperty("onreadystatechange")
    EventListener<Event> getOnReadyStateChange();

    @JSProperty("onselectionchange")
    EventListener<Event> getOnSelectionChange();

    @JSProperty("onwheel")
    EventListener<Event> getOnWheel();

    @JSProperty("onafterscriptexecute")
    EventListener<Event> setOnAfterScriptExecute(EventListener<Event> listener);

    @JSProperty("onbeforescriptexecute")
    EventListener<Event> setOnBeforeScriptExecute(EventListener<Event> listener);

    @JSProperty("oncopy")
    EventListener<Event> setOnCopy(EventListener<Event> listener);

    @JSProperty("oncut")
    EventListener<Event> setOnCut(EventListener<Event> listener);

    @JSProperty("onpaste")
    EventListener<Event> setOnPaste(EventListener<Event> listener);

    @JSProperty("onpointerlockchange")
    EventListener<Event> setOnPointerLockChange(EventListener<Event> listener);

    @JSProperty("onpointerlockerror")
    EventListener<Event> setOnPointerLockError(EventListener<Event> listener);

    @JSProperty("onreadystatechange")
    EventListener<Event> setOnReadyStateChange(EventListener<Event> listener);

    @JSProperty("onselectionchange")
    EventListener<Event> setOnSelectionChange(EventListener<Event> listener);

    @JSProperty("onwheel")
    EventListener<Event> setOnWheel(EventListener<Event> listener);


    @JSMethod
    Node adoptNode(Node source);

    @JSMethod
    Range caretRangeFromPoint(double x, double y);

    default <E extends HTMLElement> E createElement(TagName tagName) {
        return innerCreateElement(UseHTMLValue.getHtmlValue(tagName));
    }

    @JSMethod("createElement")
    <E extends HTMLElement> E innerCreateElement(String tagName);

    default <E extends HTMLElement> E createElementNS(String namespaceURI, TagName tagName) {
        return innerCreateElementNS(namespaceURI, UseHTMLValue.getHtmlValue(tagName));
    }

    @JSMethod("createElementNS")
    <E extends HTMLElement> E innerCreateElementNS(String namespaceURI, String tagName);

    default <E extends Event> E createEvent(EventClass eventClass) {
        return innerCreateEvent(UseHTMLValue.getHtmlValue(eventClass));
    }

    @JSMethod("createEvent")
    <E extends Event> E innerCreateEvent(String eventClass);

    @JSMethod("createNodeIterator")
    NodeIterator createNodeIterator(Node root, int whatToShow, NodeFilter filter);

    @JSMethod
    Range createRange();

    @JSMethod
    <E extends HTMLElement> E elementFromPoint(double x, double y);

    @JSMethod
    <E extends HTMLElement> E[] elementsFromPoint(double x, double y);

    @JSMethod
    void exitPointerLock();

    @JSMethod
    void releaseCapture();

    @JSMethod
    void close();

    @JSMethod
    boolean execCommand(String command, boolean userInterface, String value);

    @JSMethod
    <E extends HTMLElement> NodeList<E> getElementsByName(String elementName);

    @JSMethod
    Selection getSelection();

    @JSMethod
    boolean hasFocus();

    @JSMethod
    void open();

    @JSMethod
    boolean queryCommandEnabled(String command);

    @JSMethod
    boolean queryCommandIndeterm(String command);

    @JSMethod
    boolean queryCommandState(String command);

    @JSMethod
    boolean queryCommandSupported(String command);

    @JSMethod
    String queryCommandValue(String command);

    @JSMethod
    void write(String text);

    @JSMethod
    void writeln(String text);
}
