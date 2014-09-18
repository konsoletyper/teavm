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

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
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
