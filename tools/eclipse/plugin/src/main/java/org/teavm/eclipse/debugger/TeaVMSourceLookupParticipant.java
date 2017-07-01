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
package org.teavm.eclipse.debugger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.ArchiveSourceContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.sourcelookup.containers.PackageFragmentRootSourceContainer;
import org.teavm.debugging.CallFrame;
import org.teavm.debugging.information.SourceLocation;
import org.teavm.debugging.javascript.JavaScriptLocation;

public class TeaVMSourceLookupParticipant extends AbstractSourceLookupParticipant {
    private Map<ISourceContainer, ISourceContainer> delegateContainers = new HashMap<>();

    @Override
    protected ISourceContainer getDelegateContainer(ISourceContainer container) {
        ISourceContainer delegate = delegateContainers.get(container);
        return delegate != null ? delegate : super.getDelegateContainer(container);
    }

    @Override
    public String getSourceName(Object object) throws CoreException {
        if (object instanceof TeaVMJavaStackFrame) {
            TeaVMJavaStackFrame stackFrame = (TeaVMJavaStackFrame)object;
            SourceLocation location = stackFrame.callFrame.getLocation();
            if (location != null) {
                return location.getFileName();
            }
            JavaScriptLocation jsLocation = stackFrame.callFrame.getOriginalLocation();
            return jsLocation != null ? jsLocation.getScript() : null;
        } else if (object instanceof TeaVMJSStackFrame) {
            TeaVMJSStackFrame stackFrame = (TeaVMJSStackFrame)object;
            JavaScriptLocation location = stackFrame.callFrame.getLocation();
            return location != null ? location.getScript() : null;
        } else {
            return null;
        }
    }

    @Override
    public Object[] findSourceElements(Object object) throws CoreException {
        List<Object> result = new ArrayList<>(Arrays.asList(super.findSourceElements(object)));
        if (object instanceof TeaVMJSStackFrame) {
            TeaVMJSStackFrame stackFrame = (TeaVMJSStackFrame)object;
            JavaScriptLocation location = stackFrame.getCallFrame().getLocation();
            if (location != null) {
                addUrlElement(result, location);
            }
        } else if (object instanceof TeaVMJavaStackFrame) {
            TeaVMJavaStackFrame stackFrame = (TeaVMJavaStackFrame)object;
            CallFrame callFrame = stackFrame.getCallFrame();
            if (callFrame.getMethod() == null && callFrame.getLocation() != null) {
                addUrlElement(result, callFrame.getOriginalLocation());
            }
        }
        return result.toArray();
    }

    private void addUrlElement(List<Object> elements, JavaScriptLocation location) {
        URL url;
        try {
            url = new URL(location.getScript());
        } catch (MalformedURLException e) {
            url = null;
        }
        if (url != null) {
            elements.add(url);
        }
    }

    @Override
    public void sourceContainersChanged(ISourceLookupDirector director) {
        delegateContainers.clear();
        ISourceContainer[] containers = director.getSourceContainers();
        for (int i = 0; i < containers.length; i++) {
            ISourceContainer container = containers[i];
            if (container.getType().getId().equals(ArchiveSourceContainer.TYPE_ID)) {
                IFile file = ((ArchiveSourceContainer)container).getFile();
                IProject project = file.getProject();
                IJavaProject javaProject = JavaCore.create(project);
                if (javaProject.exists()) {
                    try {
                        IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
                        for (int j = 0; j < roots.length; j++) {
                            IPackageFragmentRoot root = roots[j];
                            if (file.equals(root.getUnderlyingResource())) {
                                delegateContainers.put(container, new PackageFragmentRootSourceContainer(root));
                            } else {
                                IPath path = root.getSourceAttachmentPath();
                                if (path != null) {
                                    if (file.getFullPath().equals(path)) {
                                        delegateContainers.put(container, new PackageFragmentRootSourceContainer(root));
                                    }
                                }
                            }
                        }
                    } catch (JavaModelException e) {
                    }
                }
            }
        }
    }
}
