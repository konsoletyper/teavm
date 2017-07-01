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

import static org.teavm.eclipse.TeaVMEclipsePlugin.CLASS_DIALOG_ID;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.teavm.eclipse.TeaVMEclipsePlugin;

public abstract class ClassSelectionDialog extends FilteredItemsSelectionDialog {
    private IJavaProject javaProject;

    public ClassSelectionDialog(Shell shell, IJavaProject javaProject) {
        super(shell, false);
        this.javaProject = javaProject;
        LabelProvider labelProvider = new LabelProvider()  {
            @Override public String getText(Object element) {
                return getElementName(element);
            }
        };
        setListLabelProvider(labelProvider);
        setDetailsLabelProvider(labelProvider);
    }

    @Override
    protected String getInitialPattern() {
        return "";
    }

    @Override
    protected Control createExtendedContentArea(Composite parent) {
        return null;
    }

    @Override
    protected ItemsFilter createFilter() {
        return new ItemsFilter() {
            @Override public boolean matchItem(Object item) {
                IType type = (IType)item;
                return type.getFullyQualifiedName().toLowerCase().contains(getPattern().toLowerCase());
            }
            @Override public boolean isConsistentItem(Object item) {
                return item instanceof IType;
            }
        };
    }

    @Override
    protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter filter,
            IProgressMonitor progressMonitor) throws CoreException {
        IType[] mainTypes = findTypes(filter.getPattern(), progressMonitor);
        for (IType type : mainTypes) {
            contentProvider.add(type, filter);
        }
    }

    @Override
    protected IDialogSettings getDialogSettings() {
        IDialogSettings settings = TeaVMEclipsePlugin.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(CLASS_DIALOG_ID);
        if (section == null) {
            section = settings.addNewSection(CLASS_DIALOG_ID);
        }
        return section;
    }

    @Override
    public String getElementName(Object element) {
        IType type = (IType)element;
        return getTypeName(type);
    }

    private String getTypeName(IType type) {
        if (type == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(type.getTypeQualifiedName());
        if (type.getPackageFragment() != null) {
            sb.append(" in ").append(type.getPackageFragment().getElementName());
        }
        return sb.toString();
    }

    @Override
    protected Comparator<?> getItemsComparator() {
        return new Comparator<IType>() {
            @Override public int compare(IType o1, IType o2) {
                return getTypeName(o1).compareTo(getTypeName(o2));
            }
        };
    }

    @Override
    protected IStatus validateItem(Object item) {
        return Status.OK_STATUS;
    }

    private IType[] findTypes(String patternText, IProgressMonitor progressMonitor) {
        IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaProject[] { javaProject },
                IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS | IJavaSearchScope.SYSTEM_LIBRARIES |
                IJavaSearchScope.APPLICATION_LIBRARIES);
        SearchPattern pattern = createSearchPattern(patternText);
        SearchParticipant[] participants = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
        ClassCollector collector = new ClassCollector();
        try {
            new SearchEngine().search(pattern, participants, scope, collector, progressMonitor);
        } catch (CoreException e) {
            logError(e);
            return new IType[0];
        }
        IType[] foundTypes = collector.getTypes().toArray(new IType[collector.getTypes().size()]);
        return foundTypes;
    }

    private void logError(Throwable e) {
        IStatus status = TeaVMEclipsePlugin.makeError(e);
        TeaVMEclipsePlugin.getDefault().getLog().log(status);
        ErrorDialog.openError(getShell(), "Error", "Error", status);
    }

    private class ClassCollector extends SearchRequestor {
        private Set<IType> types = new HashSet<>();

        public Set<IType> getTypes() {
            return types;
        }

        @Override
        public void acceptSearchMatch(SearchMatch match) throws CoreException {
            IType type = acceptMatch(match);
            if (type != null) {
                types.add(type);
            }
        }
    }

    protected abstract SearchPattern createSearchPattern(String text);

    protected abstract IType acceptMatch(SearchMatch match) throws CoreException;
}
