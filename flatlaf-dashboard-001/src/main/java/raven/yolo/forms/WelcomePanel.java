package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.manager.ProjectManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class WelcomePanel extends JPanel {
    
    private JButton createProjectButton;
    private JButton openProjectButton;
    private JLabel welcomeLabel;
    private JLabel instructionLabel;
    
    public WelcomePanel() {
        initComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initComponents() {
        setBackground(Color.DARK_GRAY);
        
        welcomeLabel = new JLabel("Welcome to YOLO Annotation Tool");
        welcomeLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +8;foreground:#FFFFFF");
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        instructionLabel = new JLabel("<html><center>To get started, you need to create a new project or open an existing one.<br/>" +
                                    "A project helps organize your images, classes, and annotations.</center></html>");
        instructionLabel.putClientProperty(FlatClientProperties.STYLE, "font:+2;foreground:#CCCCCC");
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        createProjectButton = new JButton("Create New Project");
        createProjectButton.putClientProperty(FlatClientProperties.STYLE, 
            "arc:10;background:#0066CC;foreground:#FFFFFF;font:bold +2");
        createProjectButton.setPreferredSize(new Dimension(200, 50));
        
        openProjectButton = new JButton("Open Existing Project");
        openProjectButton.putClientProperty(FlatClientProperties.STYLE, 
            "arc:10;background:#666666;foreground:#FFFFFF;font:bold +2");
        openProjectButton.setPreferredSize(new Dimension(200, 50));
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("fill,center", "[center]", "[grow][grow 0][grow 0][grow 0][grow 0][grow]"));
        
        add(new JLabel(), "wrap"); // Spacer
        add(welcomeLabel, "center,wrap 30");
        add(instructionLabel, "center,wrap 50");
        add(createProjectButton, "center,wrap 20");
        add(openProjectButton, "center,wrap");
        add(new JLabel(), ""); // Spacer
    }
    
    private void setupEventHandlers() {
        createProjectButton.addActionListener(e -> createNewProject());
        openProjectButton.addActionListener(e -> openExistingProject());
    }
    
    private void createNewProject() {
        ProjectCreationDialog dialog = new ProjectCreationDialog(
            (Frame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            try {
                ProjectManager.getInstance().createProject(
                    dialog.getProjectName(),
                    dialog.getProjectDescription(),
                    dialog.getProjectPath()
                );
                
                JOptionPane.showMessageDialog(this, 
                    "Project created successfully!\nYou can now start adding images and classes.",
                    "Project Created", 
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "Error creating project: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void openExistingProject() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Project Directory");
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            try {
                ProjectManager.getInstance().loadProject(selectedDir.getAbsolutePath());
                
                JOptionPane.showMessageDialog(this, 
                    "Project opened successfully!",
                    "Project Opened", 
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "Error opening project: " + e.getMessage() + 
                    "\n\nMake sure the directory contains a valid project.json file.",
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
