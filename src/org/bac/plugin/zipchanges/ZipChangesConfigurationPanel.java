/*
 * Copyright (c) 2007, Your Corporation. All Rights Reserved.
 */
package org.bac.plugin.zipchanges;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * @author Bart Cremers
 * @since 26-okt-2007
 */
public class ZipChangesConfigurationPanel implements ActionListener {

    private TextFieldWithBrowseButton fileName;
    private JPanel contentPane;

    private void createUIComponents() {
        fileName = new TextFieldWithBrowseButton();
        fileName.addActionListener(this);
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    public void setFileName(File file) {
        fileName.setText(file.getAbsolutePath());
    }

    public String getFileName() {
        return fileName.getText();
    }

    public void actionPerformed(ActionEvent actionevent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(fileName.getText()));
        if (chooser.showSaveDialog(contentPane) == JFileChooser.APPROVE_OPTION) {
            fileName.setText(chooser.getSelectedFile().getPath());
        }
    }
}
