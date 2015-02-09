/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import org.teavm.javascript.spi.Rename;
import org.teavm.javascript.spi.Superclass;
import org.teavm.platform.Platform;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@Superclass("")
public class TObject {
    @Rename("fakeInit")
    public TObject() {
    }

    @Rename("<init>")
    private void init() {
        Platform.getPlatformObject(this).setId(Platform.nextObjectId());
    }

    @Rename("getClass")
    public final TClass<?> getClass0() {
        return TClass.getClass(Platform.getPlatformObject(this).getPlatformClass());
    }

    @Override
    public int hashCode() {
        return identity();
    }

    @Rename("equals")
    public boolean equals0(TObject other) {
        return this == other;
    }

    @Override
    public String toString() {
        return getClass().getName() + "@" + TInteger.toHexString(identity());
    }

    int identity() {
        return Platform.getPlatformObject(this).getId();
    }

    @Override
    protected Object clone() throws TCloneNotSupportedException {
        if (!(this instanceof TCloneable) && Platform.getPlatformObject(this)
                .getPlatformClass().getMetadata().getArrayItem() == null) {
            throw new TCloneNotSupportedException();
        }
        Platform.getPlatformObject(this).setId(Platform.nextObjectId());
        return Platform.clone(this);
    }

    @Rename("notify")
    public final void notify0() {
    }

    @Rename("notifyAll")
    public final void notifyAll0() {
    }

    @SuppressWarnings("unused")
    @Rename("wait")
    public final void wait0(long timeout) throws TInterruptedException {
    }

    @SuppressWarnings("unused")
    @Rename("wait")
    public final void wait0(long timeout, int nanos) throws TInterruptedException {
    }

    @Rename("wait")
    public final void wait0() throws TInterruptedException {
    }

    @Override
    protected void finalize() throws TThrowable {
    }

    public static TObject wrap(Object obj) {
        return (TObject)obj;
    }
}
