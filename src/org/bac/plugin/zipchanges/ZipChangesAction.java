package org.bac.plugin.zipchanges;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.actions.VcsContext;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.changes.actions.AbstractCommitChangesAction;

public class ZipChangesAction extends AbstractCommitChangesAction {

    public ZipChangesAction() {
    }

    public String getActionName(VcsContext vcsContext) {
        return "Zip Changes";
    }

    public CommitExecutor getExecutor(Project project) {
        return ZipChangesCommitExecutor.getInstance(project);
    }

    protected void performUpdate(Presentation presentation, VcsContext vcsContext) {
        super.performUpdate(presentation, vcsContext);
        if (presentation.isEnabled()) {

            //noinspection ConstantConditions
            if (vcsContext.getSelectedChanges() == null
                || vcsContext.getSelectedChanges().length == 0) {
                presentation.setEnabled(false);
            }
        }
    }
}





