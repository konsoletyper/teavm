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
package org.teavm.samples.webapis;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLSourceElement;
import org.teavm.jso.dom.html.HTMLVideoElement;

public final class Video {
    private static HTMLDocument document = Window.current().getDocument();

    private Video() {
    }

    public static void run() {
        HTMLSourceElement sourceMp4 = document.createElement("source").cast();
        sourceMp4.setSrc("http://media.w3.org/2010/05/sintel/trailer.mp4");
        sourceMp4.setAttribute("type", "video/mp4");

        HTMLSourceElement sourceWebm = document.createElement("source").cast();
        sourceWebm.setSrc("http://media.w3.org/2010/05/sintel/trailer.webm");
        sourceWebm.setAttribute("type", "video/webm");

        HTMLSourceElement sourceOgv = document.createElement("source").cast();
        sourceOgv.setSrc("http://media.w3.org/2010/05/sintel/trailer.ogv");
        sourceOgv.setAttribute("type", "video/ogg");

        var p = document.createElement("p");
        p.appendChild(document.createTextNode("Your user agent does not support the HTML5 Video element."));

        HTMLVideoElement video = document.createElement("video").cast();
        video.setControls(true);
        video.setPreload("none");
        video.setMediaGroup("myVideoGroup");
        video.setPoster("http://media.w3.org/2010/05/sintel/poster.png");
        video.appendChild(sourceMp4);
        video.appendChild(sourceWebm);
        video.appendChild(sourceOgv);
        video.appendChild(p);

        var divVideo = document.createElement("div");
        divVideo.appendChild(video);

        var divButtons = document.createElement("div")
                .withAttr("id", "button")
                .withChild("button", elem -> elem.withText("load()").listenClick(evt -> video.load()))
                .withChild("button", elem -> elem.withText("play()").listenClick(evt -> video.play()))
                .withChild("button", elem -> elem.withText("pause()").listenClick(evt -> video.pause()))
                .withChild("br")
                .withChild("button", elem -> elem.withText("currentTime+=10")
                        .listenClick(evt -> video.addCurrentTime(10)))
                .withChild("button", elem -> elem.withText("currentTime-=10")
                        .listenClick(evt -> video.addCurrentTime(-10)))
                .withChild("button", elem -> elem.withText("currentTime-=50")
                        .listenClick(evt -> video.setCurrentTime(50)))
                .withChild("br")
                .withChild("button", elem -> elem.withText("playbackRate++")
                        .listenClick(evt -> video.addPlaybackRate(1)))
                .withChild("button", elem -> elem.withText("playbackRate--")
                        .listenClick(evt -> video.addPlaybackRate(-1)))
                .withChild("button", elem -> elem.withText("playbackRate+=0.1")
                        .listenClick(evt -> video.addPlaybackRate(0.1)))
                .withChild("button", elem -> elem.withText("playbackRate-=0.1")
                        .listenClick(evt -> video.addPlaybackRate(-0.1)))
                .withChild("br")
                .withChild("button", elem -> elem.withText("volume+=1").listenClick(evt -> video.addVolume(0.1F)))
                .withChild("button", elem -> elem.withText("volume-=1").listenClick(evt -> video.addVolume(-0.1F)))
                .withChild("button", elem -> elem.withText("mute").listenClick(evt -> video.setMuted(true)))
                .withChild("button", elem -> elem.withText("unmute").listenClick(evt -> video.setMuted(false)));

        var body = document.getBody();
        body.appendChild(divVideo);
        body.appendChild(divButtons);
    }

}
