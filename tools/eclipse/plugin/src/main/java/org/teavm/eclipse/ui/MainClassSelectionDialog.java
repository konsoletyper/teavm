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
package org.teavm.eclipse.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.swt.widgets.Shell;

public class MainClassSelectionDialog extends ClassSelectionDialog {
    public MainClassSelectionDialog(Shell shell, IJavaProject javaProject) {
        super(shell, javaProject);
        setTitle("Selecting main class");
    }

    @Override
    protected SearchPattern createSearchPattern(String text) {
        return SearchPattern.createPattern("main(String[]) void", IJavaSearchConstants.METHOD,
                IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
    }

    @Override
    protected IType acceptMatch(SearchMatch match) throws CoreException {
        IMethod method = (IMethod)match.getElement();
        if ((method.getFlags() & Flags.AccStatic) != 0) {
            return method.getDeclaringType();
        }
        return null;
    }
}
