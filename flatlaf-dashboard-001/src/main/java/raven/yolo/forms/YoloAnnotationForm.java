package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.component.ClassPanel;
import raven.yolo.component.ImageListPanel;
import raven.yolo.components.ImageViewer;
import raven.yolo.manager.ProjectManager;
import raven.yolo.model.ClassManager;
import raven.yolo.model.YoloImage;
import raven.yolo.model.YoloProject;
import raven.yolo.model.TrainingConfig;
import raven.yolo.forms.TrainingConfigDialog;
import raven.yolo.forms.ExportDatasetDialog;
import raven.yolo.forms.PythonSetupDialog;
import raven.yolo.forms.ProjectCreationDialog;
import raven.yolo.training.TrainingManager;
import raven.yolo.training.PythonSetupManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

public class YoloAnnotationForm extends JPanel {
    private ImageListPanel imageListPanel;
    private ClassPanel classPanel;
    private ImageViewer imageViewer;
    private WelcomePanel welcomePanel;
    private JPanel mainContentPanel;
    private CardLayout cardLayout;
    private JMenuBar menuBar;
    private JLabel statusLabel;
    private JLabel projectInfoLabel;
    private JPanel trainingPanel;
    private JLabel trainingStatusLabel;
    private JButton trainButton;
    private JButton pythonSetupButton;
    
    public YoloAnnotationForm() {
        initComponents();
        setupLayout();        setupEventHandlers();
        setupKeyBindings();
        updateProjectUI();
        
        // Listen for project changes
        ProjectManager.getInstance().addProjectListener(this::onProjectChanged);
    }    private void initComponents() {
        // Create menu bar
        createMenuBar();
        
        // Create panels
        imageListPanel = new ImageListPanel();
        classPanel = new ClassPanel();
        imageViewer = new ImageViewer();
        welcomePanel = new WelcomePanel();
        trainingPanel = createTrainingPanel();
        
        // Create card layout for switching between welcome and main content
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        
        // Status bar
        statusLabel = new JLabel("Ready");
        projectInfoLabel = new JLabel("No project loaded");
        trainingStatusLabel = new JLabel("Python environment: Checking...");
        
        // Set background
        setBackground(Color.DARK_GRAY);
        imageViewer.setBackground(Color.DARK_GRAY);
        
        // Check Python environment status
        updatePythonEnvironmentStatus();
    }
    
    private void createMenuBar() {
        menuBar = new JMenuBar();
          // Project menu
        JMenu projectMenu = new JMenu("Project");
        projectMenu.setMnemonic(KeyEvent.VK_P);
        
        JMenuItem projectManager = new JMenuItem("Project Manager...");
        projectManager.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK));
        projectManager.addActionListener(e -> openProjectManager());
        
        JMenuItem newProject = new JMenuItem("New Project...");        newProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newProject.addActionListener(e -> createNewProject());
        
        JMenuItem openProject = new JMenuItem("Open Project...");
        openProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openProject.addActionListener(e -> openProject());
        
        JMenuItem saveProject = new JMenuItem("Save Project");
        saveProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveProject.addActionListener(e -> saveProject());
        
        projectMenu.add(projectManager);
        projectMenu.addSeparator();
        projectMenu.add(newProject);
        projectMenu.add(openProject);
        projectMenu.addSeparator();
        projectMenu.add(saveProject);
        
        // Export menu
        JMenu exportMenu = new JMenu("Export");
        exportMenu.setMnemonic(KeyEvent.VK_E);
        
        JMenuItem exportDataset = new JMenuItem("Export Dataset...");
        exportDataset.addActionListener(e -> exportDataset());
        
        exportMenu.add(exportDataset);
        
        // Tools menu
        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic(KeyEvent.VK_T);
        
        JMenuItem clearAnnotations = new JMenuItem("Clear All Annotations");
        clearAnnotations.addActionListener(e -> clearAnnotations());
        
        toolsMenu.add(clearAnnotations);
        
        menuBar.add(projectMenu);
        menuBar.add(exportMenu);
        menuBar.add(toolsMenu);
    }
      private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Create welcome panel
        JPanel welcomeCard = new JPanel(new BorderLayout());
        welcomeCard.add(welcomePanel, BorderLayout.CENTER);
        
        // Create main annotation panel
        JPanel annotationCard = createMainAnnotationPanel();
        
        // Add cards to card layout
        mainContentPanel.add(welcomeCard, "WELCOME");
        mainContentPanel.add(annotationCard, "ANNOTATION");
        
        // Status bar
        JPanel statusPanel = new JPanel(new MigLayout("fill,insets 5", "[fill][grow 0]", "[]"));
        statusPanel.add(statusLabel, "");
        statusPanel.add(projectInfoLabel, "");
        
        add(mainContentPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        
        // Show welcome panel initially
        cardLayout.show(mainContentPanel, "WELCOME");
    }    private JPanel createMainAnnotationPanel() {
        // Main content panel with optimized layout for full screen
        JPanel mainPanel = new JPanel(new MigLayout("fill,insets 2", "[280!][fill,grow][320!]", "[fill][grow 0]"));
        
        // Left panel (images and classes) - increased width
        JPanel leftPanel = new JPanel(new MigLayout("fill,insets 2", "[fill]", "[fill][280!]"));
        
        // Image list with better scrolling
        JScrollPane imageScrollPane = new JScrollPane(imageListPanel);
        imageScrollPane.setBorder(BorderFactory.createTitledBorder("Project Images"));
        imageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        imageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        leftPanel.add(imageScrollPane, "wrap");
        leftPanel.add(classPanel, "");
        
        // Center panel (image viewer) - maximized for annotation
        JPanel centerPanel = new JPanel(new BorderLayout(2, 2));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Annotation Workspace"));
        
        // Create image viewer container with zoom controls
        JPanel imageContainer = createImageViewerContainer();
        centerPanel.add(imageContainer, BorderLayout.CENTER);
        
        // Add annotation controls at bottom
        JPanel controlPanel = createAnnotationControlPanel();
        centerPanel.add(controlPanel, BorderLayout.SOUTH);
        
        // Right panel (training and tools) - increased width
        JPanel rightPanel = createRightPanel();
        
        mainPanel.add(leftPanel, "");
        mainPanel.add(centerPanel, "");
        mainPanel.add(rightPanel, "");
        
        return mainPanel;
    }
    
    /**
     * Create image viewer container with zoom and navigation controls
     */
    private JPanel createImageViewerContainer() {
        JPanel container = new JPanel(new BorderLayout());
        
        // Add image viewer
        container.add(imageViewer, BorderLayout.CENTER);
        
        // Add zoom controls
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        zoomPanel.setOpaque(false);
        
        JButton zoomInBtn = new JButton("+");
        JButton zoomOutBtn = new JButton("-");
        JButton zoomFitBtn = new JButton("Fit");
        JButton zoom100Btn = new JButton("100%");
        
        // Style zoom buttons
        for (JButton btn : new JButton[]{zoomInBtn, zoomOutBtn, zoomFitBtn, zoom100Btn}) {
            btn.setPreferredSize(new Dimension(50, 25));
            btn.setMargin(new Insets(2, 2, 2, 2));
        }
        
        zoomPanel.add(new JLabel("Zoom:"));
        zoomPanel.add(zoomInBtn);
        zoomPanel.add(zoomOutBtn);
        zoomPanel.add(zoomFitBtn);
        zoomPanel.add(zoom100Btn);
        
        container.add(zoomPanel, BorderLayout.NORTH);
        
        return container;
    }
    
    /**
     * Create enhanced annotation control panel
     */
    private JPanel createAnnotationControlPanel() {
        JPanel panel = new JPanel(new MigLayout("fill,insets 5", "[fill]", "[]"));
        
        // Instructions panel
        JPanel instructionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        instructionPanel.setOpaque(false);
        
        JLabel instructionLabel = new JLabel(
            "<html><b>Instructions:</b> " +
            "Left-click and drag to create bounding box • " +
            "Click to select • Drag to move • " +
            "Drag handles to resize • Right-click for menu • " +
            "Delete/Backspace to remove selected</html>"
        );
        instructionLabel.setFont(instructionLabel.getFont().deriveFont(Font.PLAIN, 11f));
        instructionPanel.add(instructionLabel);
        
        panel.add(instructionPanel, "wrap");
        
        return panel;
    }
    
    /**
     * Create right panel with training and tools
     */
    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new MigLayout("fill,insets 2", "[fill]", "[fill][grow 0]"));
        
        // Training panel in scroll pane
        JScrollPane trainingScrollPane = new JScrollPane(trainingPanel);
        trainingScrollPane.setBorder(BorderFactory.createTitledBorder("Training & Export"));
        trainingScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        trainingScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Quick stats panel
        JPanel statsPanel = createQuickStatsPanel();
        
        rightPanel.add(trainingScrollPane, "");
        rightPanel.add(statsPanel, "wrap, h 80!");
        
        return rightPanel;
    }
    
    /**
     * Create quick statistics panel
     */
    private JPanel createQuickStatsPanel() {
        JPanel panel = new JPanel(new MigLayout("fill,insets 5", "[fill]", "[]"));
        panel.setBorder(BorderFactory.createTitledBorder("Quick Stats"));
        
        JLabel statsLabel = new JLabel("<html>Images: 0<br>Annotations: 0<br>Classes: 0</html>");
        panel.add(statsLabel, "");
        
        return panel;    }
    
    private void setupEventHandlers() {
        // Connect image selection to viewer
        imageListPanel.setImageSelectionListener(image -> {
            imageViewer.loadImage(image);
            updateStatus("Loaded: " + image.getFilename());
        });
          // Connect class selection to viewer
        classPanel.setClassSelectionListener((classId, className) -> {
            imageViewer.setCurrentClass(classId, className);
        });
        
        // Force initial selection if classes exist
        SwingUtilities.invokeLater(() -> {
            if (classPanel.getSelectedClassIndex() >= 0) {
                String selectedClass = classPanel.getSelectedClassName();
                if (selectedClass != null) {
                    imageViewer.setCurrentClass(classPanel.getSelectedClassIndex(), selectedClass);
                }
            }
        });
    }
    
    private void setupKeyBindings() {
        // Setup keyboard shortcuts for annotation
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
          // Undo last annotation (Ctrl+Z)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                imageViewer.removeLastAnnotation();
            }
        });
          // Delete selected annotation (Delete)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelected");
        actionMap.put("deleteSelected", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                imageViewer.deleteSelectedBoundingBox();
                updateStatus("Selected annotation deleted");
            }
        });
        
        // Clear all annotations (Ctrl+Delete)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK), "clearAll");
        actionMap.put("clearAll", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                clearAnnotations();
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
                updateStatus("Created project: " + project.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error creating project: " + e.getMessage(), 
                                            "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void openProject() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Project Directory");
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            try {
                YoloProject project = ProjectManager.getInstance().loadProject(selectedDir.getAbsolutePath());
                updateStatus("Opened project: " + project.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error opening project: " + e.getMessage(), 
                                            "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void saveProject() {
        try {
            ProjectManager.getInstance().saveCurrentProject();
            updateStatus("Project saved");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving project: " + e.getMessage(), 
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void exportDataset() {
        if (ProjectManager.getInstance().getCurrentProject() == null) {
            JOptionPane.showMessageDialog(this, "No project is currently open.", 
                                        "No Project", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Export Directory");
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File exportDir = fileChooser.getSelectedFile();
            try {
                ProjectManager.getInstance().exportDataset(exportDir.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Dataset exported successfully to: " + exportDir.getAbsolutePath(), 
                                            "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                updateStatus("Dataset exported to: " + exportDir.getAbsolutePath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error exporting dataset: " + e.getMessage(), 
                                            "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void clearAnnotations() {
        if (imageViewer.getCurrentYoloImage() != null) {
            int result = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to clear all annotations for this image?", 
                "Confirm Clear", JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                imageViewer.clearAnnotations();
                updateStatus("All annotations cleared");
            }
        }
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }    private void updateProjectUI() {
        YoloProject project = ProjectManager.getInstance().getCurrentProject();
        if (project != null) {
            projectInfoLabel.setText("Project: " + project.getName() + " | Images: " + project.getImages().size() + " | Classes: " + project.getClasses().size());
            // Switch to annotation panel when project is loaded
            cardLayout.show(mainContentPanel, "ANNOTATION");
        } else {
            projectInfoLabel.setText("No project loaded");
            // Switch to welcome panel when no project
            cardLayout.show(mainContentPanel, "WELCOME");
        }        // Update training panel
        updateTrainingPanel();
    }    private void onProjectChanged(YoloProject project) {
        SwingUtilities.invokeLater(() -> {
            // Set current project for Python environment
            if (project != null) {
                String workspacePath = raven.yolo.manager.WorkspaceManager.getInstance().getWorkspacePath();
                PythonSetupManager.getInstance().setCurrentProject(project.getId(), workspacePath);
                
                // Ensure ImageViewer has a default class if project has classes
                if (!project.getClasses().isEmpty()) {
                    // Set first class as default
                    imageViewer.setCurrentClass(0, project.getClasses().get(0));
                }
            } else {
                PythonSetupManager.getInstance().setCurrentProject(null, null);
                
                // Clear current class
                imageViewer.setCurrentClass(-1, null);
            }
            
            updateProjectUI();
            updateTrainingPanel();
            updatePythonEnvironmentStatus();
        });
    }
    
    private JPanel createTrainingPanel() {
        JPanel panel = new JPanel(new MigLayout("fill,insets 10", "[fill]", "[][][][][][][][][][grow]"));
        
        // Environment Status
        JLabel envTitle = new JLabel("Environment Status");
        envTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        panel.add(envTitle, "wrap");
        
        trainingStatusLabel = new JLabel("Checking...");
        trainingStatusLabel.putClientProperty(FlatClientProperties.STYLE, "foreground:$Component.infoForeground");
        panel.add(trainingStatusLabel, "wrap 10");
        
        // Python Setup
        pythonSetupButton = new JButton("Setup Python Environment");
        pythonSetupButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        pythonSetupButton.addActionListener(e -> showPythonSetupDialog());
        panel.add(pythonSetupButton, "wrap 10");
        
        // Project Info
        JLabel projectTitle = new JLabel("Project Information");
        projectTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        panel.add(projectTitle, "wrap");
        
        JLabel projectStats = new JLabel("<html>No project loaded</html>");
        projectStats.setName("projectStats");
        panel.add(projectStats, "wrap 10");
        
        // Export Dataset
        JButton exportButton = new JButton("Export Dataset");
        exportButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;background:$Component.accentColor");
        exportButton.addActionListener(e -> exportDataset());
        panel.add(exportButton, "wrap 5");
        
        // Training
        JLabel trainingTitle = new JLabel("Model Training");
        trainingTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        panel.add(trainingTitle, "wrap");
        
        trainButton = new JButton("Start Training");
        trainButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;background:$Component.focusColor");
        trainButton.addActionListener(e -> startTraining());
        panel.add(trainButton, "wrap 5");
        
        JButton configButton = new JButton("Training Config");
        configButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        configButton.addActionListener(e -> showTrainingConfig());
        panel.add(configButton, "wrap 10");
        
        // Training Progress (initially hidden)
        JLabel progressLabel = new JLabel("Training Progress:");
        progressLabel.setVisible(false);
        progressLabel.setName("progressLabel");
        panel.add(progressLabel, "wrap");
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setName("progressBar");
        panel.add(progressBar, "wrap");
        
        return panel;
    }
    
    private void updateTrainingPanel() {
        YoloProject project = ProjectManager.getInstance().getCurrentProject();
        
        // Find project stats label
        Component[] components = trainingPanel.getComponents();
        JLabel projectStats = null;
        for (Component comp : components) {
            if (comp instanceof JLabel && "projectStats".equals(comp.getName())) {
                projectStats = (JLabel) comp;
                break;
            }
        }
        
        if (projectStats != null) {
            if (project != null) {
                int totalImages = project.getImages().size();
                int annotatedImages = (int) project.getImages().stream()
                    .mapToLong(img -> img.getAnnotations().size())
                    .filter(count -> count > 0)
                    .count();
                int totalAnnotations = project.getImages().stream()
                    .mapToInt(img -> img.getAnnotations().size())
                    .sum();
                
                String stats = String.format(
                    "<html><b>%s</b><br>" +
                    "Images: %d<br>" +
                    "Annotated: %d<br>" +
                    "Total annotations: %d<br>" +
                    "Classes: %d</html>",
                    project.getName(),
                    totalImages,
                    annotatedImages,
                    totalAnnotations,
                    project.getClasses().size()
                );
                projectStats.setText(stats);
            } else {
                projectStats.setText("<html>No project loaded</html>");
            }
        }
        
        // Enable/disable buttons based on project availability
        boolean hasProject = project != null;
        trainButton.setEnabled(hasProject);
    }
      private void updatePythonEnvironmentStatus() {
        SwingUtilities.invokeLater(() -> {
            // Force refresh to detect any newly installed Python
            PythonSetupManager setupManager = PythonSetupManager.getInstance();
            setupManager.refreshPythonEnvironment();
            
            String pythonCmd = setupManager.findPythonCommand();
            boolean ultralyticsInstalled = setupManager.isUltralyticsInstalled();
            
            if (pythonCmd != null && ultralyticsInstalled) {
                trainingStatusLabel.setText("✓ Python and Ultralytics ready");
                trainingStatusLabel.putClientProperty(FlatClientProperties.STYLE, "foreground:$Component.accentColor");
                pythonSetupButton.setText("Reconfigure Environment");
            } else if (pythonCmd != null) {
                trainingStatusLabel.setText("⚠ Python found, Ultralytics missing");
                trainingStatusLabel.putClientProperty(FlatClientProperties.STYLE, "foreground:$Component.warningForeground");
                pythonSetupButton.setText("Install Ultralytics");
            } else {
                trainingStatusLabel.setText("✗ Python not found");
                trainingStatusLabel.putClientProperty(FlatClientProperties.STYLE, "foreground:$Component.errorForeground");
                pythonSetupButton.setText("Setup Python Environment");
            }
        });
    }
      private void showPythonSetupDialog() {
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        PythonSetupDialog dialog = new PythonSetupDialog(parentFrame);
        dialog.setVisible(true);
        // Update status after dialog closes - force refresh to detect changes
        updatePythonEnvironmentStatus();
    }
    
    private void startTraining() {
        YoloProject project = ProjectManager.getInstance().getCurrentProject();
        if (project == null) {
            JOptionPane.showMessageDialog(this, 
                "Please open or create a project first.", 
                "No Project", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check if we have annotations
        boolean hasAnnotations = project.getImages().stream()
            .anyMatch(img -> !img.getAnnotations().isEmpty());
        
        if (!hasAnnotations) {
            JOptionPane.showMessageDialog(this, 
                "Project has no annotations. Please annotate some images first.", 
                "No Annotations", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check Python environment
        if (!PythonSetupManager.getInstance().isPythonEnvironmentReady()) {
            int result = JOptionPane.showConfirmDialog(this,
                "Python environment is not ready. Would you like to set it up now?",
                "Environment Not Ready",
                JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                showPythonSetupDialog();
            }
            return;
        }
          // Start training with simple default config
        TrainingManager trainingManager = TrainingManager.getInstance();
        
        SwingUtilities.invokeLater(() -> {
            // Show progress indicators
            setTrainingProgress(true, "Preparing training...", 0);
            
            // Create simple training config
            TrainingConfig config = new TrainingConfig();
            config.setEpochs(100);
            config.setImageSize(640);
            config.setBatchSize(16);
            
            // Start training in background thread
            new Thread(() -> {
                try {
                    trainingManager.startTraining(config).thenAccept(success -> {
                        SwingUtilities.invokeLater(() -> {
                            setTrainingProgress(false, "", 0);
                            if (success) {
                                JOptionPane.showMessageDialog(this, 
                                    "Training completed successfully!", 
                                    "Training Complete", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(this, 
                                    "Training failed. Check console for details.", 
                                    "Training Failed", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }).exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> {
                            setTrainingProgress(false, "", 0);
                            JOptionPane.showMessageDialog(this, 
                                "Training error: " + ex.getMessage(), 
                                "Training Error", JOptionPane.ERROR_MESSAGE);
                        });
                        return null;
                    });
                    
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        setTrainingProgress(false, "", 0);
                        JOptionPane.showMessageDialog(this, 
                            "Training error: " + e.getMessage(), 
                            "Training Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });
    }
    
    private void showTrainingConfig() {
        YoloProject project = ProjectManager.getInstance().getCurrentProject();
        if (project == null) {
            JOptionPane.showMessageDialog(this, 
                "Please open or create a project first.", 
                "No Project", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        TrainingConfigDialog dialog = new TrainingConfigDialog(parentFrame);
        dialog.setVisible(true);
    }
    
    private void setTrainingProgress(boolean visible, String message, int progress) {
        Component[] components = trainingPanel.getComponents();
        JLabel progressLabel = null;
        JProgressBar progressBar = null;
        
        for (Component comp : components) {
            if (comp instanceof JLabel && "progressLabel".equals(comp.getName())) {
                progressLabel = (JLabel) comp;
            } else if (comp instanceof JProgressBar && "progressBar".equals(comp.getName())) {
                progressBar = (JProgressBar) comp;
            }
        }
        
        if (progressLabel != null && progressBar != null) {
            progressLabel.setVisible(visible);
            progressBar.setVisible(visible);
            
            if (visible) {
                progressLabel.setText("Training Progress: " + message);
                progressBar.setValue(progress);
                progressBar.setString(progress + "%");
                progressBar.setStringPainted(true);
            }
            
            trainingPanel.revalidate();
            trainingPanel.repaint();
        }
        
        // Enable/disable train button
        trainButton.setEnabled(!visible);
    }
    
    public JMenuBar getCustomMenuBar() {
        return menuBar;
    }
    
    private void openProjectManager() {
        ProjectManagerDialog dialog = new ProjectManagerDialog((Frame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            YoloProject project = dialog.getSelectedProject();
            if (project != null) {
                ProjectManager.getInstance().setCurrentProject(project);
                updateStatus("Opened project: " + project.getName());
            }
        }
    }
}
