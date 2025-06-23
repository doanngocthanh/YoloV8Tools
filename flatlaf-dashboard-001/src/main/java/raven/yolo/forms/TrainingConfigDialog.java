package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.model.TrainingConfig;
import raven.yolo.model.YoloProject;
import raven.yolo.training.TrainingManager;
import raven.yolo.training.PythonSetupManager;
import raven.yolo.manager.ProjectManager;
import raven.yolo.manager.WorkspaceManager;

import javax.swing.*;
import java.awt.*;

/**
 * Training Configuration Dialog
 */
public class TrainingConfigDialog extends JDialog implements TrainingManager.TrainingListener {
    
    private TrainingConfig config;
    private JSpinner epochsSpinner;
    private JSpinner batchSizeSpinner;
    private JSpinner imageSizeSpinner;
    private JSpinner learningRateSpinner;
    private JComboBox<String> modelVariantCombo;
    private JComboBox<String> deviceCombo;
    private JSpinner workersSpinner;
    private JSpinner patienceSpinner;
    private JCheckBox cacheCheckBox;
    private JCheckBox augmentCheckBox;
    private JSlider mosaicSlider;
    private JSlider mixupSlider;
    private JSlider copyPasteSlider;
      private JButton startTrainingButton;
    private JButton stopTrainingButton;
    private JButton cancelButton;
    private JProgressBar progressBar;
    private JTextArea logArea;
    
    public TrainingConfigDialog(Frame parent) {
        super(parent, "Training Configuration", true);
        this.config = new TrainingConfig();
        
        initComponents();
        setupLayout();
        setupEventHandlers();
        loadConfiguration();
        
        setSize(600, 700);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // Register as training listener
        TrainingManager.getInstance().addTrainingListener(this);
    }
    
    @Override
    public void setVisible(boolean b) {
        if (b) {
            // Refresh Python environment when dialog is shown
            refreshTrainingEnvironment();
        }
        super.setVisible(b);
    }
    
    /**
     * Refresh training environment status
     */
    private void refreshTrainingEnvironment() {
        SwingUtilities.invokeLater(() -> {
            try {
                PythonSetupManager pythonManager = PythonSetupManager.getInstance();
                
                // Ensure project context is set
                ProjectManager projectManager = ProjectManager.getInstance();
                YoloProject currentProject = projectManager.getCurrentProject();
                if (currentProject != null) {
                    String workspacePath = WorkspaceManager.getInstance().getCurrentWorkspacePath();
                    pythonManager.setCurrentProject(currentProject.getId(), workspacePath);
                }
                
                // Force refresh environment
                pythonManager.forceRefreshPythonEnvironment();
                
                // Update UI based on environment status
                updateTrainingButtonState();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
      /**
     * Update training button state based on environment
     */
    private void updateTrainingButtonState() {
        PythonSetupManager pythonManager = PythonSetupManager.getInstance();
        String pythonCommand = pythonManager.getProjectPythonCommand();
        boolean hasUltralytics = pythonManager.isUltralyticsInstalledInProject();
        boolean isTraining = TrainingManager.getInstance().isTraining();
        
        // Check dataset validity
        YoloProject currentProject = ProjectManager.getInstance().getCurrentProject();
        String datasetValidation = null;
        if (currentProject != null) {
            datasetValidation = validateDatasetForUI(currentProject);
        }
        
        boolean canTrain = pythonCommand != null && hasUltralytics && !isTraining && datasetValidation == null;
        
        startTrainingButton.setEnabled(canTrain);
        
        // Set appropriate tooltip
        if (!canTrain) {
            if (pythonCommand == null) {
                startTrainingButton.setToolTipText("Python environment not available");
            } else if (!hasUltralytics) {
                startTrainingButton.setToolTipText("Ultralytics not installed in project environment");
            } else if (isTraining) {
                startTrainingButton.setToolTipText("Training already in progress");
            } else if (datasetValidation != null) {
                startTrainingButton.setToolTipText("Dataset issue: " + datasetValidation);
            }
        } else {
            startTrainingButton.setToolTipText("Start model training");
        }
        
        System.out.println("Training button state - Enabled: " + canTrain + 
                          ", Python: " + pythonCommand + 
                          ", Ultralytics: " + hasUltralytics + 
                          ", Dataset: " + (datasetValidation == null ? "OK" : datasetValidation));
    }
    
    /**
     * Quick validation for UI purposes (lighter than full validation)
     */
    private String validateDatasetForUI(YoloProject project) {
        if (project == null) {
            return "No project loaded";
        }
        
        if (project.getClasses().isEmpty()) {
            return "No classes defined";
        }
        
        if (project.getImages().isEmpty()) {
            return "No images in project";
        }
        
        // Count annotated images
        long annotatedCount = project.getImages().stream()
            .filter(img -> !img.getAnnotations().isEmpty())
            .count();
            
        if (annotatedCount == 0) {
            return "No annotated images";
        }
        
        if (annotatedCount < 2) {
            return "Need at least 2 annotated images";
        }
        
        return null; // OK
    }
    
    private void initComponents() {
        // Model configuration
        epochsSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 1000, 1));
        batchSizeSpinner = new JSpinner(new SpinnerNumberModel(16, 1, 128, 1));
        imageSizeSpinner = new JSpinner(new SpinnerNumberModel(640, 320, 1280, 32));
        learningRateSpinner = new JSpinner(new SpinnerNumberModel(0.01, 0.001, 1.0, 0.001));
        
        modelVariantCombo = new JComboBox<>(new String[]{
            "yolov8n.pt", "yolov8s.pt", "yolov8m.pt", "yolov8l.pt", "yolov8x.pt"
        });
        
        deviceCombo = new JComboBox<>(new String[]{"auto", "cpu", "cuda"});
        
        workersSpinner = new JSpinner(new SpinnerNumberModel(8, 0, 32, 1));
        patienceSpinner = new JSpinner(new SpinnerNumberModel(50, 10, 200, 10));
        
        // Augmentation options
        cacheCheckBox = new JCheckBox("Cache images");
        augmentCheckBox = new JCheckBox("Enable augmentation");
        
        mosaicSlider = new JSlider(0, 100, 100);
        mosaicSlider.setMajorTickSpacing(25);
        mosaicSlider.setMinorTickSpacing(5);
        mosaicSlider.setPaintTicks(true);
        mosaicSlider.setPaintLabels(true);
        
        mixupSlider = new JSlider(0, 100, 0);
        mixupSlider.setMajorTickSpacing(25);
        mixupSlider.setMinorTickSpacing(5);
        mixupSlider.setPaintTicks(true);
        mixupSlider.setPaintLabels(true);
        
        copyPasteSlider = new JSlider(0, 100, 0);
        copyPasteSlider.setMajorTickSpacing(25);
        copyPasteSlider.setMinorTickSpacing(5);
        copyPasteSlider.setPaintTicks(true);
        copyPasteSlider.setPaintLabels(true);
        
        // Control buttons
        startTrainingButton = new JButton("Start Training");
        startTrainingButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        stopTrainingButton = new JButton("Stop Training");
        stopTrainingButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        stopTrainingButton.setEnabled(false);
        
        cancelButton = new JButton("Close");
        cancelButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Progress and logging
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("fill,insets 20", "[grow,fill]", "[][grow][60]"));
        
        // Configuration panel
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Basic settings tab
        JPanel basicPanel = new JPanel(new MigLayout("fill,insets 10", "[][grow,fill]", ""));
        basicPanel.add(new JLabel("Epochs:"), "");
        basicPanel.add(epochsSpinner, "wrap");
        basicPanel.add(new JLabel("Batch Size:"), "");
        basicPanel.add(batchSizeSpinner, "wrap");
        basicPanel.add(new JLabel("Image Size:"), "");
        basicPanel.add(imageSizeSpinner, "wrap");
        basicPanel.add(new JLabel("Learning Rate:"), "");
        basicPanel.add(learningRateSpinner, "wrap");
        basicPanel.add(new JLabel("Model Variant:"), "");
        basicPanel.add(modelVariantCombo, "wrap");
        basicPanel.add(new JLabel("Device:"), "");
        basicPanel.add(deviceCombo, "wrap");
        basicPanel.add(new JLabel("Workers:"), "");
        basicPanel.add(workersSpinner, "wrap");
        basicPanel.add(new JLabel("Patience:"), "");
        basicPanel.add(patienceSpinner, "wrap");
        
        tabbedPane.addTab("Basic", basicPanel);
        
        // Advanced settings tab
        JPanel advancedPanel = new JPanel(new MigLayout("fill,insets 10", "[grow,fill]", ""));
        advancedPanel.add(cacheCheckBox, "wrap");
        advancedPanel.add(augmentCheckBox, "wrap 10");
        
        advancedPanel.add(new JLabel("Mosaic (0-1.0):"), "wrap");
        advancedPanel.add(mosaicSlider, "wrap 10");
        
        advancedPanel.add(new JLabel("Mixup (0-1.0):"), "wrap");
        advancedPanel.add(mixupSlider, "wrap 10");
        
        advancedPanel.add(new JLabel("Copy Paste (0-1.0):"), "wrap");
        advancedPanel.add(copyPasteSlider, "wrap");
        
        tabbedPane.addTab("Advanced", advancedPanel);
        
        add(tabbedPane, "wrap");
        
        // Log panel
        JPanel logPanel = new JPanel(new MigLayout("fill,insets 0", "[grow,fill]", "[][grow,fill]"));
        logPanel.setBorder(BorderFactory.createTitledBorder("Training Log"));
        logPanel.add(progressBar, "wrap");
        
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logPanel.add(logScrollPane);
        
        add(logPanel, "wrap");
        
        // Button panel
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[grow][]20[]20[]", "[]"));
        buttonPanel.add(new JLabel(), "grow");
        buttonPanel.add(startTrainingButton);
        buttonPanel.add(stopTrainingButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel);
    }
    
    private void setupEventHandlers() {
        startTrainingButton.addActionListener(e -> startTraining());
        stopTrainingButton.addActionListener(e -> stopTraining());
        cancelButton.addActionListener(e -> dispose());
        
        // Update config when values change
        epochsSpinner.addChangeListener(e -> config.setEpochs((Integer) epochsSpinner.getValue()));
        batchSizeSpinner.addChangeListener(e -> config.setBatchSize((Integer) batchSizeSpinner.getValue()));
        imageSizeSpinner.addChangeListener(e -> config.setImageSize((Integer) imageSizeSpinner.getValue()));
        learningRateSpinner.addChangeListener(e -> config.setLearningRate((Double) learningRateSpinner.getValue()));
        
        modelVariantCombo.addActionListener(e -> config.setModelVariant((String) modelVariantCombo.getSelectedItem()));
        deviceCombo.addActionListener(e -> config.setDevice((String) deviceCombo.getSelectedItem()));
        
        workersSpinner.addChangeListener(e -> config.setWorkers((Integer) workersSpinner.getValue()));
        patienceSpinner.addChangeListener(e -> config.setPatience((Integer) patienceSpinner.getValue()));
        
        cacheCheckBox.addActionListener(e -> config.setCache(cacheCheckBox.isSelected()));
        augmentCheckBox.addActionListener(e -> config.setAugment(augmentCheckBox.isSelected()));
        
        mosaicSlider.addChangeListener(e -> config.setMosaic(mosaicSlider.getValue() / 100.0));
        mixupSlider.addChangeListener(e -> config.setMixup(mixupSlider.getValue() / 100.0));
        copyPasteSlider.addChangeListener(e -> config.setCopyPaste(copyPasteSlider.getValue() / 100.0));
    }
    
    private void loadConfiguration() {
        epochsSpinner.setValue(config.getEpochs());
        batchSizeSpinner.setValue(config.getBatchSize());
        imageSizeSpinner.setValue(config.getImageSize());
        learningRateSpinner.setValue(config.getLearningRate());
        modelVariantCombo.setSelectedItem(config.getModelVariant());
        deviceCombo.setSelectedItem(config.getDevice());
        workersSpinner.setValue(config.getWorkers());
        patienceSpinner.setValue(config.getPatience());
        cacheCheckBox.setSelected(config.isCache());
        augmentCheckBox.setSelected(config.isAugment());
        mosaicSlider.setValue((int) (config.getMosaic() * 100));
        mixupSlider.setValue((int) (config.getMixup() * 100));
        copyPasteSlider.setValue((int) (config.getCopyPaste() * 100));
    }      private void startTraining() {
        if (TrainingManager.getInstance().isTraining()) {
            JOptionPane.showMessageDialog(this, 
                "Training is already in progress!", 
                "Training Active", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
          // Check Python environment for current project
        PythonSetupManager pythonManager = PythonSetupManager.getInstance();
        
        // Force refresh to get latest environment state
        pythonManager.forceRefreshPythonEnvironment();
        
        String pythonCommand = pythonManager.getProjectPythonCommand();
        boolean hasUltralytics = pythonManager.isUltralyticsInstalledInProject();
        
        System.out.println("Training check - Python: " + pythonCommand + ", Ultralytics: " + hasUltralytics);
        
        if (pythonCommand == null || !hasUltralytics) {
            String message = "Project environment is not ready for training:\n";
            if (pythonCommand == null) {
                message += "- Python not found\n";
            }
            if (!hasUltralytics) {
                message += "- Ultralytics not installed in project environment\n";
            }
            message += "\nWould you like to open Python Setup?";
            
            int result = JOptionPane.showConfirmDialog(this,
                message,
                "Project Environment Required",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
              if (result == JOptionPane.YES_OPTION) {
                Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
                
                // Ensure project context is set for Python setup
                try {
                    ProjectManager projectManager = ProjectManager.getInstance();
                    YoloProject currentProject = projectManager.getCurrentProject();
                    if (currentProject != null) {
                        String workspacePath = WorkspaceManager.getInstance().getCurrentWorkspacePath();
                        pythonManager.setCurrentProject(currentProject.getId(), workspacePath);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                  PythonSetupDialog setupDialog = new PythonSetupDialog(parentFrame);
                setupDialog.setVisible(true);
                
                // Refresh environment after user potentially setup Python
                refreshTrainingEnvironment();
            }
            return;
        }
        
        logArea.setText("Preparing training...\n");
        startTrainingButton.setEnabled(false);
        stopTrainingButton.setEnabled(true);
        
        TrainingManager.getInstance().startTraining(config);
    }
      private void stopTraining() {
        // Show confirmation dialog
        int result = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to stop the training?\n\n" +
            "⚠️  Note: Progress bars and [PROGRESS]/[DATASET] messages are normal.\n" +
            "Only stop if you see actual error messages or training hangs.\n\n" +
            "Training progress will be lost if stopped.",
            "Stop Training Confirmation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            TrainingManager.getInstance().stopTraining();
        }
    }
    
    @Override
    public void dispose() {
        TrainingManager.getInstance().removeTrainingListener(this);
        super.dispose();
    }
    
    // Training listener methods    @Override
    public void onTrainingStarted() {
        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(true);
            progressBar.setString("Training started...");
            logArea.setText(""); // Clear previous logs
            logArea.append("=== YOLO Training Started ===\n");
            logArea.append("📝 Note: Progress bars and dataset scanning are normal operations\n");
            logArea.append("⚠️  Lines marked [WARNING] are non-critical warnings\n");
            logArea.append("✅ Training will show epoch progress once dataset preparation completes\n\n");
            logArea.append("Training started!\n");
        });
    }
      @Override
    public void onTrainingProgress(String message) {
        SwingUtilities.invokeLater(() -> {
            // Filter and format training messages
            String formattedMessage = formatTrainingMessage(message);
            if (formattedMessage != null) {
                logArea.append(formattedMessage + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }
    
    private String formatTrainingMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return null;
        }
        
        String message = rawMessage.trim();
        
        // Skip empty ERROR: lines (these are just stream redirects)
        if (message.equals("ERROR:")) {
            return null;
        }
        
        // Handle progress bars and normal output that comes through stderr
        if (message.startsWith("ERROR: ")) {
            String content = message.substring(7); // Remove "ERROR: " prefix
            
            // Skip empty lines
            if (content.trim().isEmpty()) {
                return null;
            }
            
            // Format progress bars
            if (content.contains("%|") && content.contains("[")) {
                return "[PROGRESS] " + content;
            }
            
            // Format scanning messages
            if (content.contains("Scanning") || content.contains("images,") || content.contains("backgrounds,") || content.contains("corrupt")) {
                return "[DATASET] " + content;
            }
            
            // Format warnings (keep but mark as warning)
            if (content.contains("UserWarning") || content.contains("warnings.warn")) {
                return "[WARNING] " + content;
            }
            
            // Other stderr content - mark as info
            return "[INFO] " + content;
        }
        
        // Normal stdout messages - pass through
        return message;
    }
      @Override
    public void onTrainingCompleted() {
        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(false);
            progressBar.setValue(100);
            progressBar.setString("Training completed!");
            logArea.append("Training completed successfully!\n");
            updateTrainingButtonState();
        });
    }
      @Override
    public void onTrainingFailed(String error) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(false);
            progressBar.setValue(0);
            progressBar.setString("Training failed!");
            logArea.append("Training failed: " + error + "\n");
            updateTrainingButtonState();
            
            // Show user-friendly error dialog
            showTrainingErrorDialog(error);
        });
    }
    
    /**
     * Show user-friendly error dialog with suggestions
     */
    private void showTrainingErrorDialog(String error) {
        String title = "Training Failed";
        String message = "Training could not be completed.\n\n";
        
        // Analyze error and provide helpful suggestions
        if (error.contains("No annotated images found") || 
            error.contains("No valid annotated images found")) {
            message += "Problem: No annotated images were found for training.\n\n" +
                      "Solution:\n" +
                      "• Go to the Annotation tool and add annotations to your images\n" +
                      "• Make sure to draw bounding boxes around objects\n" +
                      "• Save the annotations before training";
                      
        } else if (error.contains("No classes defined")) {
            message += "Problem: No object classes have been defined.\n\n" +
                      "Solution:\n" +
                      "• Go to the Annotation tool\n" +
                      "• Add at least one class in the Classes panel\n" +
                      "• Annotate your images with that class";
                      
        } else if (error.contains("at least 2 annotated images")) {
            message += "Problem: Not enough annotated images for training.\n\n" +
                      "Solution:\n" +
                      "• Annotate at least 2 images for meaningful training\n" +
                      "• More images (10+ recommended) will give better results";
                      
        } else if (error.contains("No valid annotations found")) {
            message += "Problem: Annotations exist but are invalid (zero-sized boxes).\n\n" +
                      "Solution:\n" +
                      "• Check your annotations in the Annotation tool\n" +
                      "• Make sure bounding boxes have proper size\n" +
                      "• Re-draw any problematic annotations";
                      
        } else if (error.contains("Ultralytics") || error.contains("ultralytics")) {
            message += "Problem: Python environment issue with Ultralytics.\n\n" +
                      "Solution:\n" +
                      "• Go to Tools > Python Environment Setup\n" +
                      "• Install or update Ultralytics\n" +
                      "• Make sure virtual environment is properly configured";
                      
        } else if (error.contains("CUDA") || error.contains("GPU")) {
            message += "Problem: GPU/CUDA related error.\n\n" +
                      "Solution:\n" +
                      "• Try changing device to 'cpu' instead of 'auto' or 'cuda'\n" +
                      "• CPU training is slower but more compatible";
                      
        } else {
            message += "Error details:\n" + error + "\n\n" +
                      "Suggestions:\n" +
                      "• Check the training log for more details\n" +
                      "• Verify your dataset has valid annotations\n" +
                      "• Try with default training settings\n" +
                      "• Contact support if the problem persists";
        }
        
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void onTrainingStopped() {
        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(false);
            progressBar.setValue(0);
            progressBar.setString("Training stopped");
            logArea.append("Training stopped by user.\n");
            updateTrainingButtonState();
        });
    }
    
    @Override
    public void onTrainingError(String error) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("ERROR: " + error + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
