/*
 *  Copyright 2019 Alexey Andreev.
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

class LayerIterator {
    private int currentLayer;
    private MethodIterator[] methodIterators;
    private GeneratedLocation currentLocation;

    LayerIterator(DebugInformation debugInformation) {
        methodIterators = new MethodIterator[debugInformation.layerCount()];
        for (int i = 0; i < methodIterators.length; ++i) {
            methodIterators[i] = new MethodIterator(debugInformation.layers[i]);
        }

        if (!isEndReached()) {
            nextImpl();
        }
    }

    public boolean isEndReached() {
        return currentLayer == 0 && methodIterators[currentLayer].isEndReached();
    }

    public int getLayer() {
        if (isEndReached()) {
            throw new IllegalStateException();
        }
        return currentLayer;
    }

    public GeneratedLocation getLocation() {
        if (isEndReached()) {
            throw new IllegalStateException();
        }
        return currentLocation;
    }

    public void next() {
        if (isEndReached()) {
            throw new IllegalStateException();
        }
        int previous = currentLayer;
        do {
            nextImpl();
        } while (!isEndReached() && previous == currentLayer);
    }

    private void nextImpl() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }

        if (canEnterLayer()) {
            ++currentLayer;
            while (canEnterLayer()) {
                ++currentLayer;
            }
            currentLocation = methodIterators[currentLayer].getLocation();
            methodIterators[currentLayer].next();
        } else if (canExitLayer()) {
            GeneratedLocation limitLocation = methodIterators[currentLayer].getLocation();
            moveToNextNotEmpty();
            while (true) {
                --currentLayer;
                moveToLocation(limitLocation);
                if (currentLayer <= 0 || methodIterators[currentLayer].getMethodId() >= 0
                        || !methodIterators[currentLayer].getLocation().equals(limitLocation)) {
                    break;
                }
                moveToNextNotEmpty();
            }
            currentLocation = limitLocation;
        } else {
            currentLocation = methodIterators[currentLayer].getLocation();
            methodIterators[currentLayer].next();
        }
    }

    private boolean canEnterLayer() {
        return currentLayer < methodIterators.length - 1
                && !methodIterators[currentLayer + 1].isEndReached()
                && !methodIterators[currentLayer].isEndReached()
                && isLocationLessOrEqual(currentLayer + 1, currentLayer);
    }

    private boolean canExitLayer() {
        if (currentLayer == 0) {
            return false;
        }
        return methodIterators[currentLayer].getMethodId() < 0;
    }

    private void moveToNextNotEmpty() {
        while (!methodIterators[currentLayer].isEndReached() && methodIterators[currentLayer].getMethodId() < 0) {
            methodIterators[currentLayer].next();
        }
    }

    private void moveToLocation(GeneratedLocation location) {
        while (true) {
            GeneratedLocation nextLocation = methodIterators[currentLayer].getLocation();
            if (nextLocation.compareTo(location) >= 0) {
                break;
            }
            currentLocation = location;
            methodIterators[currentLayer].next();
        }
    }

    private boolean isLocationLessOrEqual(int firstLayer, int secondLayer) {
        return methodIterators[firstLayer].getLocation().compareTo(methodIterators[secondLayer].getLocation()) <= 0;
    }
}
