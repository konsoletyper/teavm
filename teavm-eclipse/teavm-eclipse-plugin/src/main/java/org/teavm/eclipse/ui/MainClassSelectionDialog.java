package org.teavm.eclipse.ui;

import static org.teavm.eclipse.TeaVMEclipsePlugin.MAIN_METHOD_DIALOG_ID;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.teavm.eclipse.TeaVMEclipsePlugin;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class MainClassSelectionDialog extends FilteredItemsSelectionDialog {
    private IJavaProject javaProject;

    public MainClassSelectionDialog(Shell shell, IJavaProject javaProject) {
        super(shell, false);
        this.javaProject = javaProject;
        setTitle("Selecting main class");
        LabelProvider labelProvider = new LabelProvider()  {
            @Override public String getText(Object element) {
                return getElementName(element);
            }
        };
        setListLabelProvider(labelProvider);
        setDetailsLabelProvider(labelProvider);
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
                return type.getTypeQualifiedName().toLowerCase().contains(getPattern().toLowerCase());
            }
            @Override public boolean isConsistentItem(Object item) {
                return item instanceof IType;
            }
        };
    }

    @Override
    protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter filter,
            IProgressMonitor progressMonitor) throws CoreException {
        IType[] mainTypes = findMainTypes(progressMonitor);
        for (IType type : mainTypes) {
            contentProvider.add(type, filter);
        }
    }

    @Override
    protected IDialogSettings getDialogSettings() {
        IDialogSettings settings = TeaVMEclipsePlugin.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(MAIN_METHOD_DIALOG_ID);
        if (section == null) {
            section = settings.addNewSection(MAIN_METHOD_DIALOG_ID);
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

    private IType[] findMainTypes(IProgressMonitor progressMonitor) {
        IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaProject[] { javaProject },
                IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS |
                IJavaSearchScope.APPLICATION_LIBRARIES);
        SearchPattern pattern = SearchPattern.createPattern("main(String[]) void", IJavaSearchConstants.METHOD,
                IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
        SearchParticipant[] participants = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
        MainClassCollector collector = new MainClassCollector();
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

    private static class MainClassCollector extends SearchRequestor {
        private Set<IType> types = new HashSet<>();

        public Set<IType> getTypes() {
            return types;
        }

        @Override
        public void acceptSearchMatch(SearchMatch match) throws CoreException {
            IMethod method = (IMethod)match.getElement();
            if ((method.getFlags() & Flags.AccStatic) != 0) {
                types.add(method.getDeclaringType());
            }
        }
    }
}
