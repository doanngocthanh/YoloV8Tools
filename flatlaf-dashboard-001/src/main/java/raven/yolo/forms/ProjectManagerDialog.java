package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.manager.ProjectManager;
import raven.yolo.model.YoloProject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class ProjectManagerDialog extends JDialog {
    
    private DefaultListModel<ProjectInfo> projectListModel;
    private JList<ProjectInfo> projectList;    private JButton newProjectButton;
    private JButton openProjectButton;
    private JButton renameProjectButton;
    private JButton deleteProjectButton;
    private JButton openButton;
    private JButton cancelButton;
    
    private boolean confirmed = false;
    private YoloProject selectedProject = null;
    
    public ProjectManagerDialog(Frame parent) {
        super(parent, "Project Manager", true);
        initComponents();
        setupLayout();
        setupEventHandlers();
        loadRecentProjects();
        
        setSize(600, 400);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    private void initComponents() {
        projectListModel = new DefaultListModel<>();
        projectList = new JList<>(projectListModel);
        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectList.setCellRenderer(new ProjectListCellRenderer());
        
        newProjectButton = new JButton("New Project");
        newProjectButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
          openProjectButton = new JButton("Browse...");
        openProjectButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        renameProjectButton = new JButton("Rename");
        renameProjectButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        renameProjectButton.setEnabled(false);
        
        deleteProjectButton = new JButton("Delete");
        deleteProjectButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        deleteProjectButton.setEnabled(false);
        
        openButton = new JButton("Open");
        openButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        openButton.setEnabled(false);
        
        cancelButton = new JButton("Cancel");
        cancelButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("fill,insets 20", "[fill][grow 0]", "[grow 0][fill][grow 0]"));
        
        // Title
        JLabel titleLabel = new JLabel("Project Manager");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        add(titleLabel, "span,center,wrap 20");
        
        // Project list
        JScrollPane scrollPane = new JScrollPane(projectList);
        scrollPane.setPreferredSize(new Dimension(400, 250));
        scrollPane.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        add(scrollPane, "");
          // Side buttons
        JPanel sidePanel = new JPanel(new MigLayout("fill,insets 0", "[fill]", "[][][][]push"));
        sidePanel.add(newProjectButton, "wrap");
        sidePanel.add(openProjectButton, "wrap");
        sidePanel.add(renameProjectButton, "wrap");
        sidePanel.add(deleteProjectButton, "wrap");
        
        add(sidePanel, "wrap");
        
        // Bottom buttons
        JPanel bottomPanel = new JPanel(new MigLayout("fill,insets 0", "push[fill][fill]", "[]"));
        bottomPanel.add(cancelButton, "w 80!");
        bottomPanel.add(openButton, "w 80!");
        
        add(bottomPanel, "span,right");
    }
      private void setupEventHandlers() {
        newProjectButton.addActionListener(e -> createNewProject());
        openProjectButton.addActionListener(e -> browseForProject());
        renameProjectButton.addActionListener(e -> renameSelectedProject());
        deleteProjectButton.addActionListener(e -> deleteSelectedProject());
        openButton.addActionListener(e -> openSelectedProject());
        cancelButton.addActionListener(e -> cancelDialog());
        
        projectList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
        
        projectList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedProject();
                }
            }
        });
    }
    
    private void createNewProject() {
        ProjectCreationDialog dialog = new ProjectCreationDialog((Frame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            try {
                YoloProject project = ProjectManager.getInstance().createProject(
                    dialog.getProjectName(),
                    dialog.getProjectDescription(),
                    dialog.getProjectPath()
                );
                selectedProject = project;
                confirmed = true;
                dispose();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error creating project: " + e.getMessage(), 
                                            "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void browseForProject() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Project Directory");
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            try {
                YoloProject project = ProjectManager.getInstance().loadProject(selectedDir.getAbsolutePath());
                selectedProject = project;
                confirmed = true;
                dispose();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error opening project: " + e.getMessage(), 
                                            "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deleteSelectedProject() {
        ProjectInfo selectedInfo = projectList.getSelectedValue();
        if (selectedInfo != null) {
            int result = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete project '" + selectedInfo.name + "'?\n" +
                "This will permanently delete all project files!", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (result == JOptionPane.YES_OPTION) {
                try {
                    ProjectManager.getInstance().deleteProject(selectedInfo.path);
                    projectListModel.removeElement(selectedInfo);
                    updateButtonStates();
                    JOptionPane.showMessageDialog(this, "Project deleted successfully.", 
                                                "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error deleting project: " + e.getMessage(), 
                                                "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void renameSelectedProject() {
        ProjectInfo selectedInfo = projectList.getSelectedValue();
        if (selectedInfo != null) {
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
                    ProjectManager.getInstance().renameProject(selectedInfo.path, newName);
                    
                    // Update the list item
                    selectedInfo.name = newName;
                    projectList.repaint();
                    
                    JOptionPane.showMessageDialog(this, "Project renamed successfully.", 
                                                "Success", JOptionPane.INFORMATION_MESSAGE);
                    
                    // Refresh the project list
                    loadRecentProjects();
                    
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error renaming project: " + e.getMessage(), 
                                                "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void openSelectedProject() {
        ProjectInfo selectedInfo = projectList.getSelectedValue();
        if (selectedInfo != null) {
            try {
                YoloProject project = ProjectManager.getInstance().loadProject(selectedInfo.path);
                selectedProject = project;
                confirmed = true;
                dispose();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error opening project: " + e.getMessage(), 
                                            "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void cancelDialog() {
        confirmed = false;
        dispose();
    }
      private void updateButtonStates() {
        boolean hasSelection = projectList.getSelectedIndex() >= 0;
        renameProjectButton.setEnabled(hasSelection);
        deleteProjectButton.setEnabled(hasSelection);
        openButton.setEnabled(hasSelection);
    }
    
    private void loadRecentProjects() {
        projectListModel.clear();
        
        // Scan for projects in common locations
        scanForProjects(System.getProperty("user.home") + File.separator + "Desktop");
        scanForProjects(System.getProperty("user.home") + File.separator + "Documents");
        scanForProjects(".");
    }
    
    private void scanForProjects(String directory) {
        try {
            File dir = new File(directory);
            if (dir.exists() && dir.isDirectory()) {
                File[] subdirs = dir.listFiles(File::isDirectory);
                if (subdirs != null) {
                    for (File subdir : subdirs) {
                        File projectFile = new File(subdir, "project.json");
                        if (projectFile.exists()) {
                            try {
                                YoloProject project = ProjectManager.getInstance().loadProject(subdir.getAbsolutePath());
                                ProjectInfo info = new ProjectInfo(project.getName(), project.getDescription(), 
                                                                 subdir.getAbsolutePath(), project.getImages().size());
                                projectListModel.addElement(info);
                            } catch (Exception e) {
                                // Skip invalid projects
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore scanning errors
        }
    }
    
    // Getters
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public YoloProject getSelectedProject() {
        return selectedProject;
    }
    
    // Inner classes
    private static class ProjectInfo {
        String name;
        String description;
        String path;
        int imageCount;
        
        ProjectInfo(String name, String description, String path, int imageCount) {
            this.name = name;
            this.description = description;
            this.path = path;
            this.imageCount = imageCount;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    private static class ProjectListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof ProjectInfo) {
                ProjectInfo info = (ProjectInfo) value;
                
                String displayText = String.format("<html><b>%s</b><br/>%s<br/><small>%d images • %s</small></html>", 
                    info.name,
                    info.description.isEmpty() ? "No description" : info.description,
                    info.imageCount,
                    info.path);
                
                setText(displayText);
                setIcon(UIManager.getIcon("FileView.directoryIcon"));
                setPreferredSize(new Dimension(350, 60));
            }
            
            return this;
        }
    }
}
