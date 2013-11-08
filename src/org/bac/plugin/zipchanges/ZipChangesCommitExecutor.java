/*
 * Copyright (c) 2007, Your Corporation. All Rights Reserved.
 */
package org.bac.plugin.zipchanges;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.changes.CommitSession;
import com.intellij.openapi.vcs.changes.shelf.ShelveChangesManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Icons;
import com.intellij.vcsUtil.VcsUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Bart Cremers
 * @since 26-okt-2007
 */
public class ZipChangesCommitExecutor implements CommitExecutor, ProjectComponent, JDOMExternalizable {

    private final Project project;
    private final ChangeListManager changeListManager;
    public String PATCH_PATH;

    public static ZipChangesCommitExecutor getInstance(Project project) {
        return project.getComponent(ZipChangesCommitExecutor.class);
    }

    public ZipChangesCommitExecutor(Project project, ChangeListManager changelistmanager) {
        PATCH_PATH = "";
        this.project = project;
        changeListManager = changelistmanager;
    }

    @NotNull
    public Icon getActionIcon() {
        return Icons.TASK_ICON;
    }

    public String getActionText() {
        return "Zip Changelist...";
    }

    public String getActionDescription() {
        return "Create a Zip File from the selected changes";
    }

    @NotNull
    public CommitSession createCommitSession() {
        return new ZipChangesCommitSession();
    }

    public void projectOpened() {
        changeListManager.registerCommitExecutor(this);
    }

    public void projectClosed() {
    }

    @NotNull
    public String getComponentName() {
        return "ZipChangesCommitExecutor";
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

    public void readExternal(Element element) throws InvalidDataException {
        DefaultJDOMExternalizer.readExternal(this, element);
    }

    public void writeExternal(Element element) throws WriteExternalException {
        DefaultJDOMExternalizer.writeExternal(this, element);
    }

    private class ZipChangesCommitSession implements CommitSession {

        private final ZipChangesConfigurationPanel configurationPanel;

        private ZipChangesCommitSession() {
            this.configurationPanel = new ZipChangesConfigurationPanel();
        }

        @Nullable
        public JComponent getAdditionalConfigurationUI() {
            return configurationPanel.getContentPane();
        }

        public JComponent getAdditionalConfigurationUI(Collection<Change> collection, String s) {
            if (PATCH_PATH.length() == 0) {
                //noinspection ConstantConditions
                PATCH_PATH = project.getBaseDir().getPresentableUrl();
            }
            //File file = ShelveChangesManager.suggestPatchName(s, new File(PATCH_PATH));
            File file = ShelveChangesManager.suggestPatchName(project, s, new File(PATCH_PATH), s);
            if (!file.getName().endsWith(".zip")) {
                String path = file.getPath();
                if (path.lastIndexOf('.') >= 0) {
                    path = path.substring(0, path.lastIndexOf('.'));
                }
                file = new File(path + ".zip");
            }
            configurationPanel.setFileName(file);
            return configurationPanel.getContentPane();
        }

        public boolean canExecute(Collection<Change> changes, String s) {
            return true;
        }

        public void execute(Collection<Change> changes, String s) {

            try {
                Set<VirtualFile> vcsRoots = new HashSet<VirtualFile>();

                for (Change change : changes) {
                    if (change.getAfterRevision() != null) {
                        FilePath path = change.getAfterRevision().getFile();
                        VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, path);
                        vcsRoots.add(vcsRoot);
                    }
                }
                if (!vcsRoots.isEmpty()) {
                    Iterator<VirtualFile> fileIterator = vcsRoots.iterator();
                    VirtualFile root = fileIterator.next();

                    while (fileIterator.hasNext()) {
                        root = VfsUtil.getCommonAncestor(root, fileIterator.next());
                    }

                    if (root != null) {
                        ZipRunnable zipRunnable = new ZipRunnable(configurationPanel.getFileName(), root, changes);
                        ProgressManager.getInstance().runProcess(zipRunnable, zipRunnable);
                    } else {
                        Messages.showErrorDialog(project, "No common ancestor found for changes.", "Error");
                    }
                } else {
                    Messages.showErrorDialog(project, "No VCS roots found.", "Error");
                }
            } catch (Exception e) {
                Messages.showErrorDialog(project, e.getMessage(), "Error");
            }
        }

        public void executionCanceled() {
        }

        @Override
        public String getHelpId()
        {
            return null;
        }
    }

    private class ZipRunnable extends ProgressIndicatorBase implements Runnable {

        private final String fileName;
        private final VirtualFile root;
        private final Collection<Change> changes;

        private ZipRunnable(String fileName, VirtualFile root, Collection<Change> changes) {
            this.fileName = fileName;
            this.root = root;
            this.changes = changes;
            setIndeterminate(true);
        }

        public void run() {
            try {
                ZipOutputStream out = new ZipOutputStream(new FileOutputStream(fileName));
                byte[] buf = new byte[1024];
                for (Change change : changes) {
                    if (change.getAfterRevision() != null) {
                        FilePath path = change.getAfterRevision().getFile();
                        String zipEntryPath = VfsUtil.getRelativePath(path.getVirtualFile(), root, '/');
                        out.putNextEntry(new ZipEntry(zipEntryPath));

                        FileInputStream in = new FileInputStream(path.getIOFile());

                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        out.closeEntry();
                        in.close();
                    }
                }
                out.close();
            } catch (IOException e) {
                Messages.showErrorDialog(project, e.getMessage(), "Error");
            }
        }
    }
}
