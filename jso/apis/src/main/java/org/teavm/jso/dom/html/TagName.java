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

import org.teavm.jso.dom.html.use.UseHTMLValue;

/**
 * https://developer.mozilla.org/en-US/docs/Web/HTML/Element
 */
public enum TagName implements UseHTMLValue<String> {
    A("a"),
    ABBR("abbr"),
    AREA("area"),
    APPLET("applet"),
    ACRONYM("acronym"),
    ADDRESS("address"),
    ARTICLE("article"),
    ASIDE("aside"),
    BASE("base"),
    AUDIO("audio"),
    B("b"),
    BASEFONT("basefont"),
    BDI("bdi"),
    BDO("bdo"),
    BODY("body"),
    BIG("big"),
    BLINK("blink"),
    BLOCKQUOTE("blockquote"),
    BR("br"),
    BUTTON("button"),
    CANVAS("canvas"),
    CODE("code"),
    CITE("cite"),
    CAPTION("caption"),
    CENTER("center"),
    COL("col"),
    COLGROUP("colgroup"),
    COMMAND("command"),
    DATA("data"),
    DIR("dir"),
    DATALIST("datalist"),
    DD("dd"),
    DEL("del"),
    DIALOG("dialog"),
    DETAILS("details"),
    DFN("dfn"),
    DIV("div"),
    FIGURE("figure"),
    DL("dl"),
    DT("dt"),
    ELEMENT("element"),
    EM("em"),
    EMBED("embed"),
    FIELDSET("fieldset"),
    FIGCAPTION("figcaption"),
    FONT("font"),
    FOOTER("footer"),
    FORM("form"),
    FRAME("frame"),
    FRAMESET("FRAMESET"),
    H1("H1"),
    H2("H2"),
    H3("H3"),
    H4("H4"),
    H5("H5"),
    H6("H6"),
    HEAD("HEAD"),
    HEADER("HEADER"),
    HGROUP("HGROUP"),
    HR("HR"),
    HTML("HTML"),
    I("I"),
    IFRAME("IFRAME"),
    IMG("IMG"),
    INPUT("INPUT"),
    INS("INS"),
    KBD("KBD"),
    LABEL("LABEL"),
    LEGEND("LEGEND"),
    LI("LI"),
    LINK("LINK"),
    LISTING("LISTING"),
    MAIN("MAIN"),
    MAP("MAP"),
    OL("OL"),
    MARK("MARK"),
    MARQUEE("MARQUEE"),
    MENU("MENU"),
    NOSCRIPT("NOSCRIPT"),
    MENUITEM("MENUITEM"),
    OPTION("OPTION"),
    META("META"),
    METER("METER"),
    NAV("NAV"),
    NOEMBED("NOEMBED"),
    NOFRAMES("NOFRAMES"),
    PLAINTEXT("PLAINTEXT"),
    OBJECT("OBJECT"),
    OPTGROUP("OPTGROUP"),
    OUTPUT("OUTPUT"),
    PARAM("PARAM"),
    P("P"),
    PRE("PRE"),
    SHADOW("SHADOW"),
    PROGRESS("PROGRESS"),
    Q("Q"),
    RTC("RTC"),
    RT("RT"),
    RP("RP"),
    RUBY("RUBY"),
    S("S"),
    SCRIPT("SCRIPT"),
    SAMP("SAMP"),
    SECTION("SECTION"),
    SMALL("SMALL"),
    SELECT("SELECT"),
    SPACER("SPACER"),
    SOURCE("SOURCE"),
    SPAN("SPAN"),
    SUMMARY("SUMMARY"),
    STRONG("STRONG"),
    STRIKE("STRIKE"),
    UL("UL"),
    U("U"),
    STYLE("STYLE"),
    SUB("SUB"),
    TABLE("TABLE"),
    WBR("WBR"),
    SUP("SUP"),
    TBODY("TBODY"),
    TD("TD"),
    TEMPLATE("TEMPLATE"),
    TH("TH"),
    TEXTAREA("TEXTAREA"),
    TFOOT("TFOOT"),
    THEAD("THEAD"),
    TIME("TIME"),
    VIDEO("VIDEO"),
    TITLE("TITLE"),
    TR("TR"),
    TRACK("TRACK"),
    TT("TT"),
    VAR("VAR");

    private final String value;

    TagName(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
