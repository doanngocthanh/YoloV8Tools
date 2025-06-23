package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.manager.WorkspaceManager;
import raven.yolo.manager.ProjectManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ProjectCreationDialog extends JDialog {
      private JTextField nameField;
    private JTextField descriptionField;
    private JLabel workspacePathLabel;
    private JButton createButton;
    private JButton cancelButton;
    
    private boolean confirmed = false;
    
    public ProjectCreationDialog(Frame parent) {
        super(parent, "Create New Project", true);
        initComponents();
        setupLayout();
        setupEventHandlers();
        
        setSize(500, 250);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    private void initComponents() {
        nameField = new JTextField();
        nameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter project name...");
        
        descriptionField = new JTextField();
        descriptionField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter project description...");
        
        // Show current workspace path
        workspacePathLabel = new JLabel(WorkspaceManager.getInstance().getWorkspacePath());
        workspacePathLabel.putClientProperty(FlatClientProperties.STYLE, "font:-1;foreground:$Label.disabledForeground");
        
        createButton = new JButton("Create Project");
        createButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        cancelButton = new JButton("Cancel");
        cancelButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
    }
      private void setupLayout() {
        setLayout(new MigLayout("fill,insets 20", "[grow 0][fill]", "[][][]20[]"));
        
        // Title
        JLabel titleLabel = new JLabel("Create New YOLO Project");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        add(titleLabel, "span,center,wrap 20");
        
        // Form fields
        add(new JLabel("Project Name:"), "");
        add(nameField, "wrap");
        
        add(new JLabel("Description:"), "");
        add(descriptionField, "wrap");
        
        add(new JLabel("Workspace:"), "");
        add(workspacePathLabel, "wrap");
        
        // Info label
        JLabel infoLabel = new JLabel("<html><i>Project will be created in: " + 
            WorkspaceManager.getInstance().getWorkspacePath() + File.separator + "[project-name]</i></html>");
        infoLabel.putClientProperty(FlatClientProperties.STYLE, "font:-1;foreground:$Label.disabledForeground");
        add(infoLabel, "span,wrap 10");
        
        // Buttons
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[fill][fill]", "[]"));
        buttonPanel.add(cancelButton, "");
        buttonPanel.add(createButton, "");
        
        add(buttonPanel, "span,right");
    }
      private void setupEventHandlers() {
        createButton.addActionListener(e -> createProject());
        cancelButton.addActionListener(e -> cancelDialog());
        
        // Enable create button only when name field is filled
        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCreateButton(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCreateButton(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCreateButton(); }
        });
        
        updateCreateButton();
    }    
    private void createProject() {
        String name = nameField.getText().trim();
        String description = descriptionField.getText().trim();
        
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a project name.", 
                                        "Validation Error", JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return;
        }
        
        // Check if project name is valid (no special characters)
        if (!name.matches("[a-zA-Z0-9_-]+")) {
            JOptionPane.showMessageDialog(this, 
                "Project name can only contain letters, numbers, underscores, and dashes.", 
                "Invalid Project Name", JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return;
        }
        
        // Check if project already exists in workspace
        if (WorkspaceManager.getInstance().projectExists(name)) {
            JOptionPane.showMessageDialog(this, 
                "A project with this name already exists in the workspace.", 
                "Project Exists", JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return;
        }
        
        try {
            // Create project in workspace
            String projectPath = WorkspaceManager.getInstance().getProjectPath(name);
            ProjectManager.getInstance().createProject(name, description, projectPath);
            
            confirmed = true;
            dispose();
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Failed to create project: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void cancelDialog() {
        confirmed = false;
        dispose();
    }
    
    private void updateCreateButton() {
        boolean enabled = !nameField.getText().trim().isEmpty();
        createButton.setEnabled(enabled);
    }
    
    // Getters
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public String getProjectName() {
        return nameField.getText().trim();
    }
    
    public String getProjectDescription() {
        return descriptionField.getText().trim();
    }
    
    public String getProjectPath() {
        return WorkspaceManager.getInstance().getProjectPath(getProjectName());
    }
}
