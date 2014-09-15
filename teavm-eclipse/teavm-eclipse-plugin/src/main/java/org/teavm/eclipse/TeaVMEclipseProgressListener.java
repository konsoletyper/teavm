package org.teavm.eclipse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

/**
 *
 * @author Alexey Andreev
 */
public class TeaVMEclipseProgressListener implements TeaVMProgressListener {
    private IProgressMonitor progressMonitor;

    public TeaVMEclipseProgressListener(IProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    @Override
    public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) {
        String taskName = "Building";
        switch (phase) {
            case DECOMPILATION:
                taskName = "Decompiling";
                break;
            case DEPENDENCY_CHECKING:
                taskName = "Dependency checking";
                break;
            case DEVIRTUALIZATION:
                taskName = "Applying devirtualization";
                break;
            case LINKING:
                taskName = "Linking";
                break;
            case RENDERING:
                taskName = "Rendering";
                break;
        }
        progressMonitor.beginTask(taskName, count);
        return progressMonitor.isCanceled() ? TeaVMProgressFeedback.CANCEL : TeaVMProgressFeedback.CONTINUE;
    }

    @Override
    public TeaVMProgressFeedback progressReached(int progress) {
        progressMonitor.worked(progress);
        return progressMonitor.isCanceled() ? TeaVMProgressFeedback.CANCEL : TeaVMProgressFeedback.CONTINUE;
    }
}
