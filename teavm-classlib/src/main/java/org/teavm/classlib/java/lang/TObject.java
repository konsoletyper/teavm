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

<<<<<<< HEAD

import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.InjectedBy;
import org.teavm.javascript.ni.Rename;
import org.teavm.javascript.ni.Superclass;
import org.teavm.runtime.Async;
=======
import org.teavm.javascript.spi.Rename;
import org.teavm.javascript.spi.Superclass;
import org.teavm.platform.Platform;
>>>>>>> dd25ae4759716d735fe6f93a54c8bfab2e7fc7bf

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@Superclass("")
public class TObject {
    
    private TThread owner;
    private TObject monitorLock;
    private int monitorCount=0;
    
    static void monitorEnter(TObject o){
        if ( o.monitorLock == null ){
            o.monitorLock = new TObject();
        }
        while (o.owner != null && o.owner != TThread.currentThread() ){
            try {
                o.monitorLock.wait();
            } catch (InterruptedException ex) {
                
            }
        }
        o.owner = TThread.currentThread();
        o.monitorCount++;
        
    }
    
    static void monitorExit(TObject o){
        
        o.monitorCount--;
        if ( o.monitorCount == 0 ){
            if ( o.monitorLock != null ){
                o.owner = null;
                o.monitorLock.notifyAll();
            }
        }
    }
    
    static boolean holdsLock(TObject o){
        return o.owner == TThread.currentThread();
    }
    
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

    @GeneratedBy(ObjectNativeGenerator.class)
    @Rename("notify")
    public native final void notify0();

    
    @GeneratedBy(ObjectNativeGenerator.class)
    @Rename("notifyAll")
    public native final void notifyAll0();
    
    
    @Rename("wait")
    public final void wait0(long timeout) throws TInterruptedException{
        try {
            wait(timeout, 0);
        } catch ( InterruptedException ex){
            throw new TInterruptedException();
        }
    }
    
    @Async
    @GeneratedBy(ObjectNativeGenerator.class)
    @Rename("wait")
    public native final void wait0(long timeout, int nanos) throws TInterruptedException;

    @Rename("wait")
    public final void wait0() throws TInterruptedException {
        try {
            wait(0l);
        } catch (InterruptedException ex) {
            throw new TInterruptedException();
        }
    }

    @Override
    protected void finalize() throws TThrowable {
    }

    public static TObject wrap(Object obj) {
        return (TObject)obj;
    }
}
