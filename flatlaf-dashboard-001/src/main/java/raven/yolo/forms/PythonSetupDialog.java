package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.training.PythonSetupManager;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Python Setup Dialog
 * Kiểm tra và cài đặt Python environment cho training
 */
public class PythonSetupDialog extends JDialog {
      private JLabel statusLabel;
    private JLabel pythonVersionLabel;
    private JLabel ultralyticsStatusLabel;
    private JButton checkButton;    private JButton installButton;
    private JButton setupVenvButton;
    private JButton trySimpleInstallButton;    private JButton installPythonButton;
    private JButton downloadPythonButton;
    private JButton downloadVcRedistButton;
    private JButton closeButton;
    private JProgressBar progressBar;
    private JTextArea logArea;
    
    private PythonSetupManager pythonManager;
      public PythonSetupDialog(Frame parent) {
        super(parent, "Python Environment Setup", true);
        this.pythonManager = PythonSetupManager.getInstance();
        
        // Ensure we have a current project context
        ensureProjectContext();
        
        initComponents();
        setupLayout();
        setupEventHandlers();
        checkEnvironment();
        
        setSize(600, 500);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    /**
     * Ensure we have a project context for Python setup
     */
    private void ensureProjectContext() {
        try {
            // Try to get current project
            var projectManager = raven.yolo.manager.ProjectManager.getInstance();
            var currentProject = projectManager.getCurrentProject();
            
            if (currentProject != null) {
                // Set project context for Python manager
                var workspaceManager = raven.yolo.manager.WorkspaceManager.getInstance();
                String workspacePath = workspaceManager.getCurrentWorkspacePath();
                pythonManager.setCurrentProject(currentProject.getId(), workspacePath);
            } else {
                // Create a temporary project context if none exists
                var workspaceManager = raven.yolo.manager.WorkspaceManager.getInstance();
                String workspacePath = workspaceManager.getCurrentWorkspacePath();
                pythonManager.setCurrentProject("default", workspacePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to default project
            pythonManager.setCurrentProject("default", System.getProperty("user.home"));
        }
    }
    
    private void initComponents() {
        // Status labels
        statusLabel = new JLabel("Checking Python environment...");
        statusLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");
        
        pythonVersionLabel = new JLabel("Python: Not found");
        ultralyticsStatusLabel = new JLabel("Ultralytics: Not installed");
        
        // Buttons
        checkButton = new JButton("Check Environment");
        checkButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        installButton = new JButton("Install Ultralytics");
        installButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        installButton.setEnabled(false);
          setupVenvButton = new JButton("Setup Virtual Environment");
        setupVenvButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
          trySimpleInstallButton = new JButton("Try Simple Install");
        trySimpleInstallButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        trySimpleInstallButton.setEnabled(false);
        
        installPythonButton = new JButton("Install Python");
        installPythonButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;background:$Component.accentColor");
        installPythonButton.setToolTipText("Install Python automatically using Windows Package Manager");
          downloadPythonButton = new JButton("Download Python");
        downloadPythonButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        downloadPythonButton.setToolTipText("Open Python.org download page in browser");
          // Visual C++ Redistributable button (initially hidden)
        downloadVcRedistButton = new JButton("Download Visual C++ Redistributable");
        downloadVcRedistButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;background:$Component.warningBackground");
        downloadVcRedistButton.setToolTipText("Download Microsoft Visual C++ Redistributable (required for PyTorch/Ultralytics)");
        downloadVcRedistButton.setVisible(false);
        downloadVcRedistButton.addActionListener(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://aka.ms/vs/16/release/vc_redist.x64.exe"));
            } catch (Exception ex) {
                addToLog("Error opening download link: " + ex.getMessage());
            }
        });
        
        closeButton = new JButton("Close");
        closeButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        
        // Log area
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.putClientProperty(FlatClientProperties.STYLE, "background:$TextField.background");
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("insets 20", "[grow,fill]", "[][][][][][grow,fill][]"));
        
        // Title
        JLabel titleLabel = new JLabel("Python Environment for YOLO Training");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");
        add(titleLabel, "wrap, gapbottom 20");
        
        // Status panel
        JPanel statusPanel = new JPanel(new MigLayout("insets 10", "[grow,fill]", "[]5[]5[]"));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Environment Status"));
        
        statusPanel.add(statusLabel, "wrap");
        statusPanel.add(pythonVersionLabel, "wrap");
        statusPanel.add(ultralyticsStatusLabel, "wrap");
          add(statusPanel, "wrap, gapbottom 10");
          // Action buttons - simplified layout
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[fill]", "[]5[]5[]"));
        
        // Python installation (only show if needed)
        buttonPanel.add(installPythonButton, "wrap");
        buttonPanel.add(downloadPythonButton, "wrap");
        
        // Visual C++ Redistributable (only show when needed)
        buttonPanel.add(downloadVcRedistButton, "wrap");
        
        // Environment setup (only show if Python available)
        buttonPanel.add(setupVenvButton, "wrap");
        
        // Quick install (only show if Python available but no venv)
        buttonPanel.add(installButton, "wrap");
        buttonPanel.add(trySimpleInstallButton, "wrap");
        
        // Check button (always available)
        buttonPanel.add(checkButton, "wrap");
        
        add(buttonPanel, "wrap, gapbottom 10");
        
        // Update button visibility
        updateButtonVisibility();
        
        // Progress bar
        add(progressBar, "wrap, gapbottom 10");
        
        // Log area
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Installation Log"));
        add(logScrollPane, "grow, wrap, gapbottom 10");
        
        // Close button
        add(closeButton, "right");
    }    private void setupEventHandlers() {
        checkButton.addActionListener(e -> {
            // Force refresh when user explicitly clicks check
            pythonManager.forceRefreshPythonEnvironment();
            checkEnvironment();
        });
        
        installButton.addActionListener(e -> installUltralytics());
        
        trySimpleInstallButton.addActionListener(e -> trySimpleInstall());
        
        setupVenvButton.addActionListener(e -> setupVirtualEnvironment());
        
        installPythonButton.addActionListener(e -> installPython());
        
        downloadPythonButton.addActionListener(e -> downloadPython());
        
        closeButton.addActionListener(e -> dispose());
    }    private void checkEnvironment() {
        SwingUtilities.invokeLater(() -> {
            addToLog("Checking Python environment for current project...");
            
            // Force refresh only when user explicitly clicks check
            pythonManager.forceRefreshPythonEnvironment();
            
            // Check Python (system or project-specific)
            String pythonCmd = pythonManager.getProjectPythonCommand();
            if (pythonCmd != null) {
                if (pythonManager.hasProjectVirtualEnvironment()) {
                    pythonVersionLabel.setText("Python: Found in project environment (" + pythonCmd + ")");
                    addToLog("Using project virtual environment: " + pythonManager.getCurrentVenvPath());
                } else {
                    String systemPython = pythonManager.findPythonCommand();
                    pythonVersionLabel.setText("Python: Found (system) (" + systemPython + ")");
                    addToLog("Using system Python installation");
                }
                pythonVersionLabel.setForeground(Color.GREEN.darker());
                installButton.setEnabled(true);
                trySimpleInstallButton.setEnabled(true);
                
                // Get detailed environment info
                String envInfo = pythonManager.getPythonEnvironmentInfo();
                addToLog("Python Environment Details:");
                addToLog(envInfo);
                
            } else {
                pythonVersionLabel.setText("Python: Not found");
                pythonVersionLabel.setForeground(Color.RED);
                installButton.setEnabled(false);
                trySimpleInstallButton.setEnabled(false);
                addToLog("Python not found in system PATH");
                addToLog("Please install Python or add it to your system PATH");
            }
              // Check Ultralytics in project environment
            boolean hasUltralytics = pythonManager.isUltralyticsInstalledInProject();
            if (hasUltralytics) {
                ultralyticsStatusLabel.setText("Ultralytics: Installed in project ✓");
                ultralyticsStatusLabel.setForeground(Color.GREEN.darker());
                statusLabel.setText("Project environment ready for training!");
                statusLabel.setForeground(Color.GREEN.darker());
                addToLog("Ultralytics is installed in project environment");
                downloadVcRedistButton.setVisible(false); // Hide VC++ button if working
            } else {
                ultralyticsStatusLabel.setText("Ultralytics: Not installed in project");
                ultralyticsStatusLabel.setForeground(Color.RED);
                statusLabel.setText("Project environment needs Ultralytics package");
                statusLabel.setForeground(Color.ORANGE);
                if (pythonCmd != null) {
                    addToLog("Ultralytics not installed in project environment - use buttons below to setup");
                    
                    // Check if this might be a Visual C++ Redistributable issue
                    checkForVisualCppIssue();
                }
            }
            
            addToLog("Environment check completed.");
            updateButtonVisibility();
        });
    }
    
    private void installUltralytics() {
        setButtonsEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("Installing ultralytics...");
        
        addToLog("Starting ultralytics installation...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return pythonManager.installUltralytics(new PythonSetupManager.ProgressCallback() {
                    @Override
                    public void onProgress(String message, int percentage) {
                        SwingUtilities.invokeLater(() -> {
                            if (percentage >= 0) {
                                progressBar.setValue(percentage);
                            }
                            progressBar.setString(message);
                            addToLog(message);
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        SwingUtilities.invokeLater(() -> {
                            addToLog("ERROR: " + error);
                        });
                    }
                });
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        addToLog("Ultralytics installation completed successfully!");
                        checkEnvironment();
                    } else {
                        addToLog("Ultralytics installation failed!");
                        addToLog("You can try 'Try Simple Install' or 'Setup Virtual Environment' instead.");
                    }
                } catch (Exception e) {
                    addToLog("Installation error: " + e.getMessage());
                } finally {
                    setButtonsEnabled(true);
                    progressBar.setString("Ready");
                }
            }
        };
        
        worker.execute();
    }
    
    private void trySimpleInstall() {
        setButtonsEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("Trying simple installation...");
        
        addToLog("Attempting simple ultralytics installation with --user flag...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return pythonManager.installUltralytics(new PythonSetupManager.ProgressCallback() {
                    @Override
                    public void onProgress(String message, int percentage) {
                        SwingUtilities.invokeLater(() -> {
                            if (percentage >= 0) {
                                progressBar.setValue(percentage);
                            }
                            progressBar.setString(message);
                            addToLog(message);
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        SwingUtilities.invokeLater(() -> {
                            addToLog("ERROR: " + error);
                        });
                    }
                });
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        addToLog("Simple installation completed successfully!");
                        checkEnvironment();
                    } else {
                        addToLog("Simple installation failed!");
                        addToLog("Please try 'Setup Virtual Environment' for a more isolated installation.");
                    }
                } catch (Exception e) {
                    addToLog("Installation error: " + e.getMessage());
                } finally {
                    setButtonsEnabled(true);
                    progressBar.setString("Ready");
                }
            }
        };
        
        worker.execute();
    }
      private void setupVirtualEnvironment() {
        setButtonsEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("Setting up project virtual environment...");
        
        addToLog("Starting project virtual environment setup...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return pythonManager.setupProjectVirtualEnvironment(new PythonSetupManager.ProgressCallback() {
                    @Override
                    public void onProgress(String message, int percentage) {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(percentage);
                            progressBar.setString(message);
                            addToLog(message);
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        SwingUtilities.invokeLater(() -> {
                            addToLog("ERROR: " + error);
                        });
                    }
                });
            }
              @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        addToLog("Project virtual environment setup completed successfully!");
                        addToLog("Project environment is now ready for training.");
                        
                        // Force refresh environment after successful setup
                        pythonManager.forceRefreshPythonEnvironment();
                        checkEnvironment();
                    } else {
                        addToLog("Project virtual environment setup failed!");
                    }
                } catch (Exception e) {
                    addToLog("Setup error: " + e.getMessage());
                } finally {
                    setButtonsEnabled(true);
                    progressBar.setString("Ready");
                }
            }
        };
        
        worker.execute();
    }private void setButtonsEnabled(boolean enabled) {
        checkButton.setEnabled(enabled);
        installButton.setEnabled(enabled && pythonManager.findPythonCommand() != null);
        trySimpleInstallButton.setEnabled(enabled && pythonManager.findPythonCommand() != null);
        setupVenvButton.setEnabled(enabled);
        installPythonButton.setEnabled(enabled);
        downloadPythonButton.setEnabled(enabled);
    }
    
    /**
     * Install Python automatically using Windows package managers
     */
    private void installPython() {
        int result = JOptionPane.showConfirmDialog(this,
            "This will attempt to install Python automatically using Windows Package Manager or Chocolatey.\n" +
            "The installation may take several minutes and require administrator privileges.\n\n" +
            "Do you want to proceed?",
            "Install Python",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
            
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        
        addToLog("Starting automatic Python installation...");
        setButtonsEnabled(false);
        progressBar.setString("Installing Python...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return pythonManager.installPythonOnWindows(new PythonSetupManager.ProgressCallback() {
                    @Override
                    public void onProgress(String message, int percentage) {
                        SwingUtilities.invokeLater(() -> {
                            addToLog(message);
                            if (percentage >= 0) {
                                progressBar.setValue(percentage);
                                progressBar.setString(percentage + "% - " + message);
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        SwingUtilities.invokeLater(() -> {
                            addToLog("ERROR: " + error);
                            progressBar.setString("Installation failed");
                        });
                    }
                });
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        addToLog("Python installation completed successfully!");
                        addToLog("Please restart the application to use the new Python installation.");
                        JOptionPane.showMessageDialog(PythonSetupDialog.this,
                            "Python has been installed successfully!\n" +
                            "Please restart the application to use the new Python installation.",
                            "Installation Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                        checkEnvironment();
                    } else {
                        addToLog("Python installation failed. Please try manual installation.");
                        JOptionPane.showMessageDialog(PythonSetupDialog.this,
                            "Automatic installation failed.\n" +
                            "Please use the 'Download Python' button to install manually.",
                            "Installation Failed",
                            JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception e) {
                    addToLog("Installation error: " + e.getMessage());
                    JOptionPane.showMessageDialog(PythonSetupDialog.this,
                        "Installation error: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                } finally {
                    setButtonsEnabled(true);
                    progressBar.setValue(0);
                    progressBar.setString("Ready");
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Open Python download page in browser for manual installation
     */
    private void downloadPython() {
        addToLog("Opening Python.org download page...");
        pythonManager.openPythonDownloadPage();
        
        JOptionPane.showMessageDialog(this,
            "The Python download page has been opened in your browser.\n\n" +
            "Please download and install Python, making sure to:\n" +
            "1. Check 'Add Python to PATH' during installation\n" +
            "2. Use the default installation options\n" +
            "3. Restart this application after installation\n\n" +
            "After installation, click 'Check Environment' to verify.",
            "Manual Installation",
            JOptionPane.INFORMATION_MESSAGE);
    }
      private void addToLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }    /**
     * Update button visibility based on current environment status
     */
    private void updateButtonVisibility() {
        String systemPython = pythonManager.findPythonCommand();
        boolean hasPython = systemPython != null;
        boolean hasProjectVenv = pythonManager.hasProjectVirtualEnvironment();
        boolean hasUltralyticsInProject = pythonManager.isUltralyticsInstalledInProject();
        
        // Show Python installation buttons only if no Python at all
        installPythonButton.setVisible(!hasPython);
        downloadPythonButton.setVisible(!hasPython);
        
        if (hasPython) {
            // If Python exists, show environment setup options
            if (hasProjectVenv && hasUltralyticsInProject) {
                // Project environment is fully ready
                setupVenvButton.setVisible(false);
                installButton.setVisible(false);
                trySimpleInstallButton.setVisible(false);
            } else if (hasProjectVenv && !hasUltralyticsInProject) {
                // Project environment exists but needs ultralytics
                setupVenvButton.setVisible(false);
                installButton.setVisible(true);
                trySimpleInstallButton.setVisible(true);
                installButton.setText("Install Ultralytics in Project");
                trySimpleInstallButton.setText("Quick Install");
            } else {
                // No project environment yet, need to create it
                setupVenvButton.setVisible(true);
                installButton.setVisible(false);  // Hide individual install until venv is created
                trySimpleInstallButton.setVisible(false);
                setupVenvButton.setText("Setup Project Environment");
                setupVenvButton.setToolTipText("Create virtual environment for current project with Ultralytics");
            }
        } else {
            // No Python - hide all other buttons
            setupVenvButton.setVisible(false);
            installButton.setVisible(false);
            trySimpleInstallButton.setVisible(false);
        }
        
        // Update close button appearance
        if (hasPython && hasUltralyticsInProject) {
            closeButton.setText("Project Environment Ready - Close");
            closeButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;background:$Component.accentColor");
        } else {
            closeButton.setText("Close");
            closeButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        }
        
        // Force layout update
        revalidate();
        repaint();
    }
    
    /**
     * Check if ultralytics failure might be due to Visual C++ Redistributable issue
     */
    private void checkForVisualCppIssue() {
        SwingUtilities.invokeLater(() -> {
            String pythonCmd = pythonManager.getProjectPythonCommand();
            if (pythonCmd == null) return;
            
            try {
                // Try a quick test import to see the specific error
                ProcessBuilder pb = new ProcessBuilder(pythonCmd, "-c", "import ultralytics");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                
                process.waitFor(5, TimeUnit.SECONDS);
                String outputStr = output.toString();
                
                if (outputStr.contains("Microsoft Visual C++ Redistributable") || 
                    outputStr.contains("c10.dll") || 
                    outputStr.contains("WinError 126")) {
                    
                    addToLog("DETECTED: Visual C++ Redistributable issue!");
                    addToLog("Ultralytics/PyTorch requires Microsoft Visual C++ Redistributable.");
                    downloadVcRedistButton.setVisible(true);
                    downloadVcRedistButton.setText("⚠ Download Visual C++ Redistributable (Required)");
                    
                    // Update layout
                    revalidate();
                    repaint();
                }
                
            } catch (Exception e) {
                // Silent fail - this is just a diagnostic check
            }
        });
    }
}
