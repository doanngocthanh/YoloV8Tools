    package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.manager.WorkspaceManager;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Workspace Setup Dialog
 * Hiển thị khi lần đầu khởi động hoặc khi muốn thay đổi workspace
 */
public class WorkspaceSetupDialog extends JDialog {
    
    private JTextField workspacePathField;
    private JButton browseButton;
    private JButton okButton;
    private JButton cancelButton;
    private JLabel descriptionLabel;
    
    private boolean confirmed = false;
    private String selectedWorkspacePath;
    
    public WorkspaceSetupDialog(Frame parent, boolean isFirstTime) {
        super(parent, isFirstTime ? "Setup Workspace" : "Change Workspace", true);
        
        initComponents(isFirstTime);
        setupLayout();
        setupEventHandlers();
        
        setSize(550, 300);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // Nếu là lần đầu thì không cho phép đóng mà không chọn workspace
        if (isFirstTime) {
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        }
    }
    
    private void initComponents(boolean isFirstTime) {
        // Description
        if (isFirstTime) {
            descriptionLabel = new JLabel("<html><h3>Welcome to YOLO Annotation Tool!</h3>" +
                    "<p>Please select a workspace directory where your projects will be stored.</p>" +
                    "<p>The workspace will contain all your annotation projects, settings, and exported datasets.</p></html>");
        } else {
            descriptionLabel = new JLabel("<html><h3>Change Workspace</h3>" +
                    "<p>Select a new workspace directory for your projects.</p></html>");
        }
        descriptionLabel.putClientProperty(FlatClientProperties.STYLE, "font:+1");
        
        // Workspace path field
        workspacePathField = new JTextField();
        workspacePathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Select workspace directory...");
        workspacePathField.setEditable(false);
        
        // Browse button
        browseButton = new JButton("Browse...");
        browseButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Action buttons
        okButton = new JButton(isFirstTime ? "Create Workspace" : "Change Workspace");
        okButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        okButton.setEnabled(false);
        
        cancelButton = new JButton("Cancel");
        cancelButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Set default workspace path
        String defaultPath = FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath() + 
                File.separator + "YoloAnnotationTool";
        workspacePathField.setText(defaultPath);
        selectedWorkspacePath = defaultPath;
        okButton.setEnabled(true);
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("insets 20", "[grow,fill]", "[][][grow][30][]"));
        
        add(descriptionLabel, "wrap, gapbottom 20");
        
        // Workspace path selection
        JPanel pathPanel = new JPanel(new MigLayout("insets 0", "[grow,fill][100]", "[]"));
        pathPanel.setBorder(BorderFactory.createTitledBorder("Workspace Directory"));
        pathPanel.add(workspacePathField);
        pathPanel.add(browseButton);
        add(pathPanel, "wrap, gapbottom 20");
        
        // Info panel
        JPanel infoPanel = new JPanel(new MigLayout("insets 10", "[grow,fill]", "[]"));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Workspace Structure"));
        
        JTextArea infoArea = new JTextArea(
                "The workspace will contain:\n" +
                "• project-name-1/\n" +
                "  ├── images/          (source images)\n" +
                "  ├── labels/          (YOLO format annotations)\n" +
                "  ├── exports/         (exported datasets)\n" +
                "  └── project.json     (project configuration)\n" +
                "• project-name-2/\n" +
                "• workspace-config.json (workspace settings)"
        );
        infoArea.setEditable(false);
        infoArea.setOpaque(false);
        infoArea.putClientProperty(FlatClientProperties.STYLE, "font:monospaced-1");
        
        infoPanel.add(infoArea);
        add(infoPanel, "wrap");
        
        // Button panel
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[grow][]20[]", "[]"));
        buttonPanel.add(new JLabel(), "grow");
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        add(buttonPanel, "dock south");
    }
    
    private void setupEventHandlers() {
        browseButton.addActionListener(e -> browseWorkspace());
        
        okButton.addActionListener(e -> {
            if (validateAndSetWorkspace()) {
                confirmed = true;
                dispose();
            }
        });
        
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
    }
    
    private void browseWorkspace() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Workspace Directory");
        
        // Set current path if exists
        if (selectedWorkspacePath != null) {
            File currentDir = new File(selectedWorkspacePath).getParentFile();
            if (currentDir != null && currentDir.exists()) {
                chooser.setCurrentDirectory(currentDir);
            }
        }
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            selectedWorkspacePath = selectedDir.getAbsolutePath();
            workspacePathField.setText(selectedWorkspacePath);
            okButton.setEnabled(true);
        }
    }
    
    private boolean validateAndSetWorkspace() {
        if (selectedWorkspacePath == null || selectedWorkspacePath.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please select a workspace directory.", 
                "Invalid Workspace", 
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        try {
            // Create workspace directory if not exists
            File workspaceDir = new File(selectedWorkspacePath);
            if (!workspaceDir.exists()) {
                if (!workspaceDir.mkdirs()) {
                    JOptionPane.showMessageDialog(this, 
                        "Failed to create workspace directory.", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            
            // Check if directory is writable
            if (!workspaceDir.canWrite()) {
                JOptionPane.showMessageDialog(this, 
                    "Workspace directory is not writable. Please select another location.", 
                    "Permission Error", 
                    JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            // Set workspace in WorkspaceManager
            WorkspaceManager.getInstance().setWorkspacePath(selectedWorkspacePath);
            
            return true;
            
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Failed to setup workspace: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public String getSelectedWorkspacePath() {
        return selectedWorkspacePath;
    }
}
