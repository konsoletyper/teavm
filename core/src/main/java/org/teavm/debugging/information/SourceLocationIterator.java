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
package org.teavm.debugging.information;

public class SourceLocationIterator {
    private DebugInformation debugInformation;
    private LayerIterator layerIterator;
    private LayerInfo[] layerSourceIterators;
    private boolean endReached;
    private int currentLayer;
    private GeneratedLocation lastLocation;

    public SourceLocationIterator(DebugInformation debugInformation) {
        this.debugInformation = debugInformation;
        layerIterator = new LayerIterator(debugInformation);
        layerSourceIterators = new LayerInfo[debugInformation.layerCount()];
        for (int i = 0; i < layerSourceIterators.length; ++i) {
            layerSourceIterators[i] = new LayerInfo(new LayerSourceLocationIterator(
                    debugInformation, debugInformation.layers[i]));
        }

        if (!layerIterator.isEndReached()) {
            currentLayer = layerIterator.getLayer();
            layerIterator.next();
        } else {
            currentLayer = 0;
        }

        lastLocation = layerSourceIterators[currentLayer].lastLocation;
        if (lastLocation == null) {
            endReached = true;
        }
    }

    public GeneratedLocation getLocation() {
        return lastLocation;
    }

    public boolean isEndReached() {
        return endReached;
    }

    public void next() {
        if (endReached) {
            throw new IllegalStateException();
        }

        LayerInfo currentIterator = layerSourceIterators[currentLayer];
        if (currentLayer == 0 && currentIterator.iterator.isEndReached()) {
            endReached = true;
            return;
        }

        if (currentIterator.iterator.isEndReached() || (!layerIterator.isEndReached()
                && currentIterator.iterator.getLocation().compareTo(layerIterator.getLocation()) >= 0)) {
            currentLayer = layerIterator.getLayer();
            lastLocation = layerIterator.getLocation();
            layerIterator.next();

            currentIterator = layerSourceIterators[currentLayer];
            while (!currentIterator.iterator.isEndReached()
                    && currentIterator.iterator.getLocation().compareTo(lastLocation) <= 0) {
                currentIterator.next();
            }
        } else {
            currentIterator.next();
            lastLocation = currentIterator.lastLocation;
        }
    }

    public int getFileNameId() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return layerSourceIterators[currentLayer].lastFileId;
    }

    public String getFileName() {
        int fileId = getFileNameId();
        return fileId >= 0 ? debugInformation.getFileName(fileId) : null;
    }

    public int getLine() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return layerSourceIterators[currentLayer].lastLine;
    }

    static class LayerInfo {
        LayerSourceLocationIterator iterator;
        int lastFileId;
        int lastLine;
        GeneratedLocation lastLocation;

        LayerInfo(LayerSourceLocationIterator iterator) {
            this.iterator = iterator;
            if (!iterator.isEndReached()) {
                next();
            }
        }

        void next() {
            this.lastFileId = iterator.getFileNameId();
            this.lastLine = iterator.getLine();
            this.lastLocation = iterator.getLocation();
            iterator.next();
        }
    }
}
