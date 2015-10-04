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
package org.teavm.samples.video;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLSourceElement;
import org.teavm.jso.dom.html.HTMLVideoElement;

public final class Player {
    private static HTMLDocument document = Window.current().getDocument();

    private Player() {
    }

    public static void main(String[] args) {
        HTMLSourceElement sourceMp4 = document.createElement("source").cast();
        sourceMp4.setAttribute("id", "mp4");
        sourceMp4.setSrc("http://media.w3.org/2010/05/sintel/trailer.mp4");
        sourceMp4.setAttribute("type", "video/mp4");

        HTMLSourceElement sourceWebm = document.createElement("source").cast();
        sourceWebm.setAttribute("id", "webm");
        sourceWebm.setSrc("http://media.w3.org/2010/05/sintel/trailer.webm");
        sourceWebm.setAttribute("type", "video/webm");

        HTMLSourceElement sourceOgv = document.createElement("source").cast();
        sourceOgv.setAttribute("id", "ogv");
        sourceOgv.setSrc("http://media.w3.org/2010/05/sintel/trailer.ogv");
        sourceOgv.setAttribute("type", "video/ogg");

        HTMLElement p = document.createElement("p");
        p.appendChild(document.createTextNode("Your user agent does not support the HTML5 Video element."));

        HTMLVideoElement video = document.createElement("video").cast();
        video.setAttribute("id", "video");
        video.setControls(true);
        video.setPreload("none");
        video.setMediaGroup("myVideoGroup");
        video.setPoster("http://media.w3.org/2010/05/sintel/poster.png");
        video.appendChild(sourceMp4);
        video.appendChild(sourceWebm);
        video.appendChild(sourceOgv);
        video.appendChild(p);

        HTMLElement divVideo = document.createElement("div");
        divVideo.appendChild(video);

        HTMLButtonElement loadButton = document.createElement("button").cast();
        loadButton.appendChild(document.createTextNode("load()"));
        loadButton.addEventListener("click", evt -> video.load());

        HTMLButtonElement playButton = document.createElement("button").cast();
        playButton.appendChild(document.createTextNode("play()"));
        playButton.addEventListener("click", evt -> video.play());

        HTMLButtonElement pauseButton = document.createElement("button").cast();
        pauseButton.appendChild(document.createTextNode("pause()"));
        pauseButton.addEventListener("click", evt -> video.pause());

        HTMLButtonElement currentTimePlusButton = document.createElement("button").cast();
        currentTimePlusButton.appendChild(document.createTextNode("currentTime+=10"));
        currentTimePlusButton.addEventListener("click", evt -> video.setCurrentTime(video.getCurrentTime() + 10));

        HTMLButtonElement currentTimeMinusButton = document.createElement("button").cast();
        currentTimeMinusButton.appendChild(document.createTextNode("currentTime-=10"));
        currentTimeMinusButton.addEventListener("click", evt -> video.setCurrentTime(video.getCurrentTime() - 10));

        HTMLButtonElement currentTime50Button = document.createElement("button").cast();
        currentTime50Button.appendChild(document.createTextNode("currentTime=50"));
        currentTime50Button.addEventListener("click", evt -> video.setCurrentTime(50));

        HTMLButtonElement playbackRateIncrementButton = document.createElement("button").cast();
        playbackRateIncrementButton.appendChild(document.createTextNode("playbackRate++"));
        playbackRateIncrementButton.addEventListener("click", evt -> video.setPlaybackRate(
                video.getPlaybackRate() + 1));

        HTMLButtonElement playbackRateDecrementButton = document.createElement("button").cast();
        playbackRateDecrementButton.appendChild(document.createTextNode("playbackRate--"));
        playbackRateDecrementButton.addEventListener("click", evt -> video.setPlaybackRate(
                video.getPlaybackRate() - 1));

        HTMLButtonElement playbackRatePlusButton = document.createElement("button").cast();
        playbackRatePlusButton.appendChild(document.createTextNode("playbackRate+=0.1"));
        playbackRatePlusButton.addEventListener("click", evt -> video.setPlaybackRate(video.getPlaybackRate() + 0.1));

        HTMLButtonElement playbackRateMinusButton = document.createElement("button").cast();
        playbackRateMinusButton.appendChild(document.createTextNode("playbackRate-=0.1"));
        playbackRateMinusButton.addEventListener("click", evt -> video.setPlaybackRate(video.getPlaybackRate() - 0.1));

        HTMLButtonElement volumePlusButton = document.createElement("button").cast();
        volumePlusButton.appendChild(document.createTextNode("volume+=0.1"));
        volumePlusButton.addEventListener("click", evt -> video.setVolume(video.getVolume() + 0.1f));

        HTMLButtonElement volumeMinusButton = document.createElement("button").cast();
        volumeMinusButton.appendChild(document.createTextNode("volume-=0.1"));
        volumeMinusButton.addEventListener("click", evt -> video.setVolume(video.getVolume() - 0.1f));

        HTMLButtonElement muteButton = document.createElement("button").cast();
        muteButton.appendChild(document.createTextNode("muted=true"));
        muteButton.addEventListener("click", evt -> video.setMuted(true));

        HTMLButtonElement unmuteButton = document.createElement("button").cast();
        unmuteButton.appendChild(document.createTextNode("muted=false"));
        unmuteButton.addEventListener("click", evt -> video.setMuted(false));

        HTMLElement divButtons = document.createElement("div");
        divButtons.setAttribute("id", "buttons");
        divButtons.appendChild(loadButton);
        divButtons.appendChild(playButton);
        divButtons.appendChild(pauseButton);
        divButtons.appendChild(document.createElement("br"));
        divButtons.appendChild(currentTimePlusButton);
        divButtons.appendChild(currentTimeMinusButton);
        divButtons.appendChild(currentTime50Button);
        divButtons.appendChild(document.createElement("br"));
        divButtons.appendChild(playbackRateIncrementButton);
        divButtons.appendChild(playbackRateDecrementButton);
        divButtons.appendChild(playbackRatePlusButton);
        divButtons.appendChild(playbackRateMinusButton);
        divButtons.appendChild(document.createElement("br"));
        divButtons.appendChild(volumePlusButton);
        divButtons.appendChild(volumeMinusButton);
        divButtons.appendChild(muteButton);
        divButtons.appendChild(unmuteButton);

        HTMLBodyElement body = document.getBody();
        body.appendChild(divVideo);
        body.appendChild(divButtons);
    }

}
