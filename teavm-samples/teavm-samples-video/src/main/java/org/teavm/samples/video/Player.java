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

import org.teavm.dom.browser.Window;
import org.teavm.dom.html.HTMLBodyElement;
import org.teavm.dom.html.HTMLButtonElement;
import org.teavm.dom.html.HTMLDocument;
import org.teavm.dom.html.HTMLElement;
import org.teavm.dom.html.HTMLSourceElement;
import org.teavm.dom.html.HTMLVideoElement;
import org.teavm.jso.JS;

public final class Player {

    private static Window window = (Window) JS.getGlobal();
    private static HTMLDocument document = window.getDocument();

    private Player() {
    }

    public static void main(String[] args) {
        HTMLSourceElement sourceMp4 = (HTMLSourceElement) document.createElement("source");
        sourceMp4.setAttribute("id", "mp4");
        sourceMp4.setSrc("http://media.w3.org/2010/05/sintel/trailer.mp4");
        sourceMp4.setAttribute("type", "video/mp4");

        HTMLSourceElement sourceWebm = (HTMLSourceElement) document.createElement("source");
        sourceWebm.setAttribute("id", "webm");
        sourceWebm.setSrc("http://media.w3.org/2010/05/sintel/trailer.webm");
        sourceWebm.setAttribute("type", "video/webm");

        HTMLSourceElement sourceOgv = (HTMLSourceElement) document.createElement("source");
        sourceOgv.setAttribute("id", "ogv");
        sourceOgv.setSrc("http://media.w3.org/2010/05/sintel/trailer.ogv");
        sourceOgv.setAttribute("type", "video/ogg");

        HTMLElement p = document.createElement("p");
        p.appendChild(document.createTextNode("Your user agent does not support the HTML5 Video element."));

        HTMLVideoElement video = (HTMLVideoElement) document.createElement("video");
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

        HTMLButtonElement loadButton = (HTMLButtonElement) document.createElement("button");
        loadButton.appendChild(document.createTextNode("load()"));
        loadButton.addEventListener("click", evt -> video.load());

        HTMLButtonElement playButton = (HTMLButtonElement) document.createElement("button");
        playButton.appendChild(document.createTextNode("play()"));
        playButton.addEventListener("click", evt -> video.play());

        HTMLButtonElement pauseButton = (HTMLButtonElement) document.createElement("button");
        pauseButton.appendChild(document.createTextNode("pause()"));
        pauseButton.addEventListener("click", evt -> video.pause());

        HTMLButtonElement currentTimePlusButton = (HTMLButtonElement) document.createElement("button");
        currentTimePlusButton.appendChild(document.createTextNode("currentTime+=10"));
        currentTimePlusButton.addEventListener("click", evt -> video.setCurrentTime(video.getCurrentTime() + 10));

        HTMLButtonElement currentTimeMinusButton = (HTMLButtonElement) document.createElement("button");
        currentTimeMinusButton.appendChild(document.createTextNode("currentTime-=10"));
        currentTimeMinusButton.addEventListener("click", evt -> video.setCurrentTime(video.getCurrentTime() - 10));

        HTMLButtonElement currentTime50Button = (HTMLButtonElement) document.createElement("button");
        currentTime50Button.appendChild(document.createTextNode("currentTime=50"));
        currentTime50Button.addEventListener("click", evt -> video.setCurrentTime(50));

        HTMLButtonElement playbackRateIncrementButton = (HTMLButtonElement) document.createElement("button");
        playbackRateIncrementButton.appendChild(document.createTextNode("playbackRate++"));
        playbackRateIncrementButton.addEventListener("click", evt -> video.setPlaybackRate(
                video.getPlaybackRate() + 1));

        HTMLButtonElement playbackRateDecrementButton = (HTMLButtonElement) document.createElement("button");
        playbackRateDecrementButton.appendChild(document.createTextNode("playbackRate--"));
        playbackRateDecrementButton.addEventListener("click", evt -> video.setPlaybackRate(
                video.getPlaybackRate() - 1));

        HTMLButtonElement playbackRatePlusButton = (HTMLButtonElement) document.createElement("button");
        playbackRatePlusButton.appendChild(document.createTextNode("playbackRate+=0.1"));
        playbackRatePlusButton.addEventListener("click", evt -> video.setPlaybackRate(video.getPlaybackRate() + 0.1));

        HTMLButtonElement playbackRateMinusButton = (HTMLButtonElement) document.createElement("button");
        playbackRateMinusButton.appendChild(document.createTextNode("playbackRate-=0.1"));
        playbackRateMinusButton.addEventListener("click", evt -> video.setPlaybackRate(video.getPlaybackRate() - 0.1));

        HTMLButtonElement volumePlusButton = (HTMLButtonElement) document.createElement("button");
        volumePlusButton.appendChild(document.createTextNode("volume+=0.1"));
        volumePlusButton.addEventListener("click", evt -> video.setVolume(video.getVolume() + 0.1f));

        HTMLButtonElement volumeMinusButton = (HTMLButtonElement) document.createElement("button");
        volumeMinusButton.appendChild(document.createTextNode("volume-=0.1"));
        volumeMinusButton.addEventListener("click", evt -> video.setVolume(video.getVolume() - 0.1f));

        HTMLButtonElement muteButton = (HTMLButtonElement) document.createElement("button");
        muteButton.appendChild(document.createTextNode("muted=true"));
        muteButton.addEventListener("click", evt -> video.setMuted(true));

        HTMLButtonElement unmuteButton = (HTMLButtonElement) document.createElement("button");
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
