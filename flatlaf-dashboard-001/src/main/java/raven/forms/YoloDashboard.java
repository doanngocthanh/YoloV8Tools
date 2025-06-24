package raven.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.manager.ProjectManager;
import raven.yolo.manager.WorkspaceManager;
import raven.yolo.model.WorkspaceConfig;
import raven.yolo.model.YoloProject;
import raven.yolo.forms.ProjectCreationDialog;
import raven.yolo.forms.WorkspaceSetupDialog;
import raven.yolo.forms.YoloAnnotationForm;
import raven.yolo.forms.ImportDatasetDialog;
import raven.yolo.forms.ProjectManagerDialog;
import raven.components.FormManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.format.DateTimeFormatter;

public class YoloDashboard extends JPanel {

    private JLabel workspacePathLabel;
    private JLabel projectNameLabel;
    private JLabel projectPathLabel;
    private JLabel imageCountLabel;
    private JLabel classCountLabel;
    private JLabel annotatedCountLabel;
    private JProgressBar progressBar;
    private JPanel recentProjectsPanel;
    private JButton createProjectButton;
    private JButton openProjectButton;
    private JButton importDatasetButton;
    private JButton manageProjectsButton;
    private JButton changeWorkspaceButton;
    private JButton annotationToolButton;

    public YoloDashboard() {
        initComponents();
        setupLayout();
        setupEventHandlers();
        updateDashboard();

        // Listen for project changes
        ProjectManager.getInstance().addProjectListener(this::onProjectChanged);
    }

    private void initComponents() {
        setOpaque(false);

        // Workspace info
        workspacePathLabel = new JLabel();
        workspacePathLabel.putClientProperty(FlatClientProperties.STYLE,
                "font:-1;foreground:$Label.disabledForeground");

        // Project info labels
        projectNameLabel = new JLabel("No project loaded");
        projectNameLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");

        projectPathLabel = new JLabel("");
        projectPathLabel.putClientProperty(FlatClientProperties.STYLE, "font:-1;foreground:$Label.disabledForeground");

        imageCountLabel = new JLabel("Images: 0");
        imageCountLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");

        classCountLabel = new JLabel("Classes: 0");
        classCountLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");

        annotatedCountLabel = new JLabel("Annotated: 0");
        annotatedCountLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.putClientProperty(FlatClientProperties.STYLE, "arc:10");

        // Recent projects panel
        recentProjectsPanel = new JPanel(new MigLayout("insets 0", "[grow,fill]", "[]"));

        // Quick action buttons
        createProjectButton = new JButton("Create New Project");
        createProjectButton.putClientProperty(FlatClientProperties.STYLE, "arc:10;font:bold");
        createProjectButton.setPreferredSize(new Dimension(200, 50));
        openProjectButton = new JButton("New Project");
        openProjectButton.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        openProjectButton.setPreferredSize(new Dimension(150, 40));

        importDatasetButton = new JButton("Import Dataset");
        importDatasetButton.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        importDatasetButton.setPreferredSize(new Dimension(150, 40));

        manageProjectsButton = new JButton("Manage Projects");
        manageProjectsButton.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        manageProjectsButton.setPreferredSize(new Dimension(150, 40));

        changeWorkspaceButton = new JButton("Change Workspace");
        changeWorkspaceButton.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        changeWorkspaceButton.setPreferredSize(new Dimension(150, 40));

        annotationToolButton = new JButton("Open Annotation Tool");
        annotationToolButton.putClientProperty(FlatClientProperties.STYLE, "arc:10;font:bold");
        annotationToolButton.setPreferredSize(new Dimension(200, 50));
        annotationToolButton.setEnabled(false);

        // Secondary action buttons
        openProjectButton = createSecondaryButton("📂 New Project");
        importDatasetButton = createSecondaryButton("📥 Import Dataset");
        manageProjectsButton = createSecondaryButton("⚙️ Manage Projects");
        changeWorkspaceButton = createSecondaryButton("📁 Change Workspace");
    }

    private JLabel createStatsLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, 
                "font:bold 16;foreground:" + toHex(color));
        return label;
    }

    private JButton createPrimaryButton(String text, Color color) {
        JButton button = new JButton(text);
        button.putClientProperty(FlatClientProperties.STYLE, 
                "arc:12;font:bold 14;minimumHeight:48;" +
                "background:" + toHex(color) + ";" +
                "foreground:#FFFFFF;" +
                "hoverBackground:" + toHex(color.darker()) + ";" +
                "pressedBackground:" + toHex(color.darker().darker()) + ";" +
                "borderWidth:0");
        button.setPreferredSize(new Dimension(220, 48));
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.putClientProperty(FlatClientProperties.STYLE, 
                "arc:10;font:bold 13;minimumHeight:42;" +
                "background:#FFFFFF;" +
                "foreground:#374151;" +
                "hoverBackground:#F3F4F6;" +
                "pressedBackground:#E5E7EB;" +
                "borderColor:#D1D5DB;borderWidth:1");
        button.setPreferredSize(new Dimension(180, 42));
        return button;
    }

    private String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void setupLayout() {
        setLayout(new MigLayout("insets 20", "[grow,fill]", "[][][][grow,fill][]"));

        // Header with workspace info
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[grow][]", "[][]"));
        headerPanel.setBorder(BorderFactory.createTitledBorder("Workspace"));

        JLabel workspaceLabel = new JLabel("Current Workspace:");
        workspaceLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        headerPanel.add(workspaceLabel);
        headerPanel.add(changeWorkspaceButton, "wrap");
        headerPanel.add(workspacePathLabel, "spanx 2");

        add(headerPanel, "wrap, gapbottom 15");

        // Current project info
        JPanel projectInfoPanel = new JPanel(new MigLayout("insets 10", "[grow]", "[]5[]10[]5[]5[]10[]"));
        projectInfoPanel.setBorder(BorderFactory.createTitledBorder("Current Project"));

        projectInfoPanel.add(projectNameLabel, "wrap");
        projectInfoPanel.add(projectPathLabel, "wrap");

        JPanel statsPanel = new JPanel(new MigLayout("insets 0", "[][20][][20][]", "[]"));
        statsPanel.add(imageCountLabel);
        statsPanel.add(classCountLabel);
        statsPanel.add(annotatedCountLabel);
        projectInfoPanel.add(statsPanel, "wrap");

        projectInfoPanel.add(new JLabel("Progress:"), "wrap");
        projectInfoPanel.add(progressBar, "growx, wrap");
        JPanel actionPanel = new JPanel(new MigLayout("insets 0", "[]20[]20[]", "[]"));
        actionPanel.add(createProjectButton);
        actionPanel.add(importDatasetButton);
        actionPanel.add(annotationToolButton);
        projectInfoPanel.add(actionPanel, "center, wrap");

        // Management panel
        JPanel managementPanel = new JPanel(new MigLayout("insets 0", "[]20[]", "[]"));
        managementPanel.add(manageProjectsButton);
        managementPanel.add(changeWorkspaceButton);
        projectInfoPanel.add(managementPanel, "center");

        add(projectInfoPanel, "wrap, gapbottom 15");

        // Recent projects
        JPanel recentPanel = new JPanel(new MigLayout("insets 10", "[grow,fill]", "[grow,fill][]"));
        recentPanel.setBorder(BorderFactory.createTitledBorder("Recent Projects"));

        JScrollPane scrollPane = new JScrollPane(recentProjectsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(0, 200));
        recentPanel.add(scrollPane, "wrap");
        recentPanel.add(openProjectButton, "center");

        add(recentPanel, "grow");
    }

    private void setupEventHandlers() {
        createProjectButton.addActionListener(e -> showCreateProjectDialog());
        openProjectButton.addActionListener(e -> createNewProject());
        importDatasetButton.addActionListener(e -> showImportDatasetDialog());
        manageProjectsButton.addActionListener(e -> showManageProjectsDialog());
        changeWorkspaceButton.addActionListener(e -> showChangeWorkspaceDialog());
        annotationToolButton.addActionListener(e -> openAnnotationTool());
    }

    private void showCreateProjectDialog() {
        Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
        ProjectCreationDialog dialog = new ProjectCreationDialog(parentFrame);
        dialog.setVisible(true);
    }

    private void showManageProjectsDialog() {
        Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
        ProjectManagerDialog dialog = new ProjectManagerDialog(parentFrame);
        dialog.setVisible(true);

        // Update dashboard after managing projects
        updateDashboard();
        updateRecentProjects();
    }

    private void showImportDatasetDialog() {
        Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
        ImportDatasetDialog dialog = new ImportDatasetDialog(parentFrame);
        dialog.setImportCompleteListener(() -> {
            // Update dashboard after successful import
            updateDashboard();
            updateRecentProjects();
        });
        dialog.setVisible(true);
    }    private void createNewProject() {
        // Get workspace path first
        String workspacePath = WorkspaceManager.getInstance().getWorkspacePath();
        if (workspacePath == null || workspacePath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No workspace is configured. Please set up a workspace first.",
                    "No Workspace",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show loading state
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        SwingWorker<YoloProject, Void> worker = new SwingWorker<YoloProject, Void>() {
            private String errorMessage = null;
            
            @Override
            protected YoloProject doInBackground() throws Exception {
                try {
                    // Generate unique project name and path
                    String projectName = "NewProject";
                    String projectPath = workspacePath + File.separator + projectName;
                    int counter = 1;

                    // Ensure unique project path
                    while (new File(projectPath).exists()) {
                        projectName = "NewProject" + counter;
                        projectPath = workspacePath + File.separator + projectName;
                        counter++;
                    }

                    // Create project directory
                    File projectDir = new File(projectPath);
                    if (!projectDir.mkdirs()) {
                        errorMessage = "Failed to create project directory: " + projectPath;
                        return null;
                    }

                    // Create new project
                    YoloProject project = ProjectManager.getInstance().createProject(projectName, "Auto-generated project", projectPath);
                    ProjectManager.getInstance().setCurrentProject(project);
                    
                    return project;
                } catch (Exception ex) {
                    errorMessage = "Failed to create project: " + ex.getMessage();
                    ex.printStackTrace();
                    return null;
                }
            }
            
            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                
                try {
                    YoloProject project = get();
                    if (project != null) {
                        // Update dashboard with new project
                        updateDashboard();

                        JOptionPane.showMessageDialog(YoloDashboard.this,
                                "New project '" + project.getName() + "' created successfully!\n\nYou can rename it later in the project settings.",
                                "Project Created",
                                JOptionPane.INFORMATION_MESSAGE);

                        // Open annotation tool with the new project
                        openAnnotationTool();
                    } else {
                        JOptionPane.showMessageDialog(YoloDashboard.this,
                                errorMessage != null ? errorMessage : "Unknown error creating project",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(YoloDashboard.this,
                            "Unexpected error: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
          worker.execute();
    }

    private void showChangeWorkspaceDialog() {
        Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
        WorkspaceSetupDialog dialog = new WorkspaceSetupDialog(parentFrame, false);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            updateDashboard();
            updateRecentProjects();
        }
    }

    private void openAnnotationTool() {
        FormManager.getInstance().showForm("YOLO Annotation Tool", new YoloAnnotationForm(), true);
    }

    private void updateDashboard() {
        SwingUtilities.invokeLater(() -> {
            // Update workspace info
            WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
            workspacePathLabel.setText(workspaceManager.getWorkspacePath());

            // Update project info
            YoloProject currentProject = ProjectManager.getInstance().getCurrentProject();
            if (currentProject != null) {
                projectNameLabel.setText(currentProject.getName());
                projectPathLabel.setText(currentProject.getProjectPath());

                int imageCount = currentProject.getImages().size();
                int classCount = currentProject.getClasses().size();
                int annotatedCount = (int) currentProject.getImages().stream()
                        .mapToLong(img -> img.getAnnotations().size())
                        .count();

                imageCountLabel.setText("Images: " + imageCount);
                classCountLabel.setText("Classes: " + classCount);
                annotatedCountLabel.setText("Annotations: " + annotatedCount);

                // Update progress
                if (imageCount > 0) {
                    int progress = (annotatedCount * 100) / imageCount;
                    progressBar.setValue(progress);
                    progressBar.setString(progress + "% Complete");
                } else {
                    progressBar.setValue(0);
                    progressBar.setString("No images");
                }

                annotationToolButton.setEnabled(true);
            } else {
                projectNameLabel.setText("No project loaded");
                projectPathLabel.setText("Create or open a project to get started");
                imageCountLabel.setText("Images: 0");
                classCountLabel.setText("Classes: 0");
                annotatedCountLabel.setText("Annotations: 0");
                progressBar.setValue(0);
                progressBar.setString("No project");
                annotationToolButton.setEnabled(false);
            }

            updateRecentProjects();
        });
    }

    private void updateRecentProjects() {
        recentProjectsPanel.removeAll();

        var recentProjects = WorkspaceManager.getInstance().getRecentProjects();
        if (recentProjects.isEmpty()) {
            JLabel noProjectsLabel = new JLabel("No recent projects");
            noProjectsLabel.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");
            recentProjectsPanel.add(noProjectsLabel, "center");
        } else {
            recentProjectsPanel.setLayout(new MigLayout("insets 0", "[grow,fill]", "[]5"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

            for (WorkspaceConfig.RecentProject recentProject : recentProjects) {
                JPanel projectPanel = createRecentProjectPanel(recentProject, formatter);
                recentProjectsPanel.add(projectPanel, "wrap");
            }
        }

        recentProjectsPanel.revalidate();
        recentProjectsPanel.repaint();
    }

    private JPanel createRecentProjectPanel(WorkspaceConfig.RecentProject recentProject, DateTimeFormatter formatter) {
        JPanel panel = new JPanel(new MigLayout("insets 10", "[grow][]", "[][]"));
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setToolTipText("Click to open project");
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Project name
        JLabel nameLabel = new JLabel(recentProject.getProjectName());
        nameLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");
        panel.add(nameLabel);

        // Last opened time
        JLabel timeLabel = new JLabel(recentProject.getLastOpened().format(formatter));
        timeLabel.putClientProperty(FlatClientProperties.STYLE, "font:-1;foreground:$Label.disabledForeground");
        panel.add(timeLabel, "wrap");

        // Project path
        JLabel pathLabel = new JLabel(recentProject.getProjectPath());
        pathLabel.putClientProperty(FlatClientProperties.STYLE, "font:-1;foreground:$Label.disabledForeground");
        panel.add(pathLabel, "spanx 2");
        // Click handler to open project
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Show context menu
                    showProjectContextMenu(e, recentProject);
                } else if (e.getClickCount() == 2) {
                    // Double click to open project
                    openProject(recentProject);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showProjectContextMenu(e, recentProject);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(UIManager.getColor("List.hoverBackground"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(UIManager.getColor("Panel.background"));
            }
        });

        return panel;
    }    private void showProjectContextMenu(MouseEvent e, WorkspaceConfig.RecentProject recentProject) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem openItem = new JMenuItem("Open Project");
        openItem.addActionListener(ev -> openProject(recentProject));
        contextMenu.add(openItem);
        
        // Add "New Project from Labels" option
        JMenuItem newFromLabelsItem = new JMenuItem("New Project from Labels");
        newFromLabelsItem.addActionListener(ev -> createProjectFromLabels(recentProject));
        contextMenu.add(newFromLabelsItem);

        contextMenu.addSeparator();

        JMenuItem renameItem = new JMenuItem("Rename Project");
        renameItem.addActionListener(ev -> renameProject(recentProject));
        contextMenu.add(renameItem);

        JMenuItem deleteItem = new JMenuItem("Delete Project");
        deleteItem.addActionListener(ev -> deleteProject(recentProject));
        contextMenu.add(deleteItem);

        contextMenu.addSeparator();

        JMenuItem removeItem = new JMenuItem("Remove from Recent");
        removeItem.addActionListener(ev -> removeFromRecent(recentProject));
        contextMenu.add(removeItem);

        contextMenu.show(e.getComponent(), e.getX(), e.getY());
    }    private void openProject(WorkspaceConfig.RecentProject recentProject) {
        // Show loading state
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String errorMessage = null;
            
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    // Check if project still exists
                    File projectFile = new File(recentProject.getProjectPath(), "project.json");
                    if (projectFile.exists()) {
                        ProjectManager.getInstance().loadProject(recentProject.getProjectPath());
                        return true;
                    } else {
                        errorMessage = "Project no longer exists";
                        return false;
                    }
                } catch (Exception ex) {
                    errorMessage = "Failed to open project: " + ex.getMessage();
                    ex.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                
                try {
                    boolean success = get();
                    if (!success) {
                        if ("Project no longer exists".equals(errorMessage)) {
                            int result = JOptionPane.showConfirmDialog(
                                    YoloDashboard.this,
                                    "Project no longer exists. Remove from recent projects?",
                                    "Project Not Found",
                                    JOptionPane.YES_NO_OPTION);
                            if (result == JOptionPane.YES_OPTION) {
                                WorkspaceManager.getInstance().removeRecentProject(recentProject.getProjectPath());
                                updateRecentProjects();
                            }
                        } else {
                            JOptionPane.showMessageDialog(YoloDashboard.this,
                                    errorMessage,
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(YoloDashboard.this,
                            "Unexpected error: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
          worker.execute();
    }

    private void renameProject(WorkspaceConfig.RecentProject recentProject) {
        String newName = JOptionPane.showInputDialog(this,
                "Enter new project name:",
                "Rename Project",
                JOptionPane.PLAIN_MESSAGE);

        if (newName != null && !newName.trim().isEmpty()) {
            newName = newName.trim();

            // Validate name (no special characters for directory names)
            if (!newName.matches("^[a-zA-Z0-9\\s_-]+$")) {
                JOptionPane.showMessageDialog(this,
                        "Project name can only contain letters, numbers, spaces, underscores and dashes.",
                        "Invalid Name", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                ProjectManager.getInstance().renameProject(recentProject.getProjectPath(), newName);
                JOptionPane.showMessageDialog(this, "Project renamed successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);

                // Update dashboard
                updateDashboard();
                updateRecentProjects();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error renaming project: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteProject(WorkspaceConfig.RecentProject recentProject) {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete project '" + recentProject.getProjectName() + "'?\n" +
                        "This will permanently delete all project files!",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            try {
                ProjectManager.getInstance().deleteProject(recentProject.getProjectPath());
                JOptionPane.showMessageDialog(this, "Project deleted successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);

                // Update dashboard
                updateDashboard();
                updateRecentProjects();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error deleting project: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeFromRecent(WorkspaceConfig.RecentProject recentProject) {
        try {
            WorkspaceManager.getInstance().removeRecentProject(recentProject.getProjectPath());
            updateRecentProjects();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error removing project from recent: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }    private void onProjectChanged(YoloProject project) {
        SwingUtilities.invokeLater(this::updateDashboard);
    }
    
    /**
     * Create a new project from labeled objects in the specified project
     */
    private void createProjectFromLabels(WorkspaceConfig.RecentProject recentProject) {
        try {
            // Check if project exists
            File projectFile = new File(recentProject.getProjectPath(), "project.json");
            if (!projectFile.exists()) {
                JOptionPane.showMessageDialog(this, 
                    "Project no longer exists: " + recentProject.getProjectName(), 
                    "Project Not Found", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
              // Load the project temporarily to check for annotations
            YoloProject sourceProject = ProjectManager.getInstance().loadProject(recentProject.getProjectPath());
            if (sourceProject == null) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to load project: " + recentProject.getProjectName(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Count labeled objects
            int totalLabeledObjects = 0;
            for (raven.yolo.model.YoloImage image : sourceProject.getImages()) {
                totalLabeledObjects += image.getAnnotations().size();
            }
            
            if (totalLabeledObjects == 0) {
                JOptionPane.showMessageDialog(this, 
                    "No labeled objects found in project: " + recentProject.getProjectName() + 
                    "\nPlease add some annotations first.", 
                    "No Labels", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Show confirmation
            int result = JOptionPane.showConfirmDialog(this,
                String.format(
                    "Create a new project with %d cropped objects from '%s'?\n\n" +
                    "Each labeled bounding box will be cropped and saved as a separate image.\n" +
                    "The new project will contain these cropped images as the dataset.", 
                    totalLabeledObjects, recentProject.getProjectName()),
                "Create Project from Labels",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
                
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
            
            // Show dialog to create new project from labels
            raven.yolo.forms.CreateProjectFromLabelsDialog dialog = new raven.yolo.forms.CreateProjectFromLabelsDialog(
                (Frame) SwingUtilities.getWindowAncestor(this), 
                sourceProject);
            dialog.setVisible(true);
            
            if (dialog.isConfirmed()) {
                // Refresh the dashboard to show the new project
                updateDashboard();
                updateRecentProjects();
                JOptionPane.showMessageDialog(this, 
                    "New project created successfully from labeled objects!", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error creating project from labels: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
