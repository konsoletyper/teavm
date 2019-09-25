/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.siyeh.ig.fixes.MemberSignature;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
import org.jetbrains.annotations.NotNull;
import org.teavm.idea.jps.remote.TeaVMBuilderAssistant;
import org.teavm.idea.jps.remote.TeaVMElementLocation;

public class TeaVMJPSRemoteService extends UnicastRemoteObject implements ApplicationComponent, TeaVMBuilderAssistant {
    private static final int MIN_PORT = 10000;
    private static final int MAX_PORT = 1 << 16;
    private ProjectManager projectManager;
    private int port;
    private Registry registry;

    public TeaVMJPSRemoteService() throws RemoteException {
        super();
    }

    private synchronized ProjectManager getProjectManager() {
        if (projectManager == null) {
            projectManager = ProjectManager.getInstance();
        }
        return projectManager;
    }

    @Override
    public void initComponent() {
        Random random = new Random();
        for (int i = 0; i < 20; ++i) {
            port = random.nextInt(MAX_PORT - MIN_PORT) + MIN_PORT;
            try {
                registry = LocateRegistry.createRegistry(port);
            } catch (RemoteException e) {
                continue;
            }
            try {
                registry.bind(TeaVMBuilderAssistant.ID, this);
            } catch (RemoteException | AlreadyBoundException e) {
                throw new IllegalStateException("Could not bind remote build assistant service", e);
            }
            return;
        }
        throw new IllegalStateException("Could not create RMI registry");
    }

    public int getPort() {
        return port;
    }

    @Override
    public void disposeComponent() {
        try {
            registry.unbind(TeaVMBuilderAssistant.ID);
            UnicastRemoteObject.unexportObject(registry, true);
        } catch (RemoteException | NotBoundException e) {
            throw new IllegalStateException("Could not clean-up RMI server", e);
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "TeaVM JPS service";
    }

    @Override
    public TeaVMElementLocation getMethodLocation(String className, String methodName, String methodDesc) {
        TeaVMElementLocation[] resultHolder = new TeaVMElementLocation[1];

        ApplicationManager.getApplication().runReadAction(() -> {
            for (Project project : getProjectManager().getOpenProjects()) {
                JavaPsiFacade psi = JavaPsiFacade.getInstance(project);
                PsiClass cls = psi.findClass(className, GlobalSearchScope.allScope(project));
                if (cls == null) {
                    continue;
                }

                for (PsiMethod method : cls.getAllMethods()) {
                    if (!method.getName().equals(methodName) || !getMethodSignature(method).equals(methodDesc)) {
                        continue;
                    }
                    resultHolder[0] = getMethodLocation(method);
                    return;
                }
            }
        });

        return resultHolder[0];
    }

    private String getMethodSignature(PsiMethod method) {
        StringBuilder sb = new StringBuilder("(");
        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            sb.append(MemberSignature.createTypeSignature(parameter.getType()));
        }
        sb.append(")");

        PsiType returnType = method.getReturnType();
        sb.append(MemberSignature.createTypeSignature(returnType != null ? returnType : PsiType.VOID));
        return sb.toString();
    }

    private TeaVMElementLocation getMethodLocation(PsiMethod method) {
        PsiElement element = method.getNameIdentifier();
        if (element == null) {
            element = method.getNavigationElement();
        }
        PsiFile psiFile = element.getContainingFile();
        Document document = psiFile.getViewProvider().getDocument();
        int offset = element.getTextRange().getStartOffset();
        int line = offset >= 0 ? document.getLineNumber(offset) + 1 : -1;
        int column = offset >= 0 ? offset - document.getLineStartOffset(line) + 1 : -1;
        return new TeaVMElementLocation(offset, element.getTextRange().getEndOffset(),
                line, column, psiFile.getVirtualFile().getPath());
    }
}
