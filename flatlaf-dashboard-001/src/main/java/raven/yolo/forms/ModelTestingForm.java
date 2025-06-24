package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.utils.ModelConverter;
import raven.yolo.utils.DetectionParserNew;
import raven.yolo.utils.ImageRenderer;
import raven.yolo.model.DetectionResult;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Form for testing trained models with uploaded images
 */
public class ModelTestingForm extends JPanel {
    
    private JComboBox<ModelInfo> modelComboBox;    private JButton refreshModelsButton;
    private JButton uploadImageButton;
    private JButton runInferenceButton;
    private JButton toggleDetectionsButton;
    private JLabel imageLabel;
    private JTextArea resultsArea;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JSlider confidenceSlider;
    private JLabel confidenceLabel;      private BufferedImage currentImage;
    private BufferedImage originalImage; // Store original image without detections
    private String currentImagePath;
    private List<ModelInfo> availableModels;
    private String preselectedModelPath;
    private String projectPath;
    private List<DetectionResult> lastDetections; // Store last detection results
      public ModelTestingForm() {
        // Auto-detect project path from current project
        String autoProjectPath = null;
        try {
            raven.yolo.model.YoloProject currentProject = raven.yolo.manager.ProjectManager.getInstance().getCurrentProject();
            if (currentProject != null) {
                String workspacePath = raven.yolo.manager.WorkspaceManager.getInstance().getCurrentWorkspacePath();
                autoProjectPath = workspacePath + File.separator + currentProject.getId();
                
                // Ensure Python environment is set up
                raven.yolo.training.PythonSetupManager.getInstance().setCurrentProject(currentProject.getId(), workspacePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.preselectedModelPath = null;
        this.projectPath = autoProjectPath;
        initComponents();
        setupLayout();
        setupEventHandlers();
        refreshAvailableModels();
    }
    
    public ModelTestingForm(String modelPath, String projectPath) {
        this.preselectedModelPath = modelPath;
        this.projectPath = projectPath;
        initComponents();
        setupLayout();
        setupEventHandlers();
        refreshAvailableModels();
    }
    
    private void initComponents() {        // Model selection
        modelComboBox = new JComboBox<>();
        
        refreshModelsButton = new JButton("Refresh");
        refreshModelsButton.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor");
        
        // Image upload
        uploadImageButton = new JButton("Upload Image");
        uploadImageButton.putClientProperty(FlatClientProperties.STYLE, "background:$Component.focusColor");
          runInferenceButton = new JButton("Run Inference");
        runInferenceButton.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor");
        runInferenceButton.setEnabled(false);
        
        toggleDetectionsButton = new JButton("Show Original");
        toggleDetectionsButton.putClientProperty(FlatClientProperties.STYLE, "background:$Component.borderColor");
        toggleDetectionsButton.setEnabled(false);
        
        // Image display
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imageLabel.setPreferredSize(new Dimension(400, 300));
        imageLabel.setText("No image selected");
          // Results
        resultsArea = new JTextArea(10, 30);
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
          // Progress
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        
        statusLabel = new JLabel("Ready");
        statusLabel.putClientProperty(FlatClientProperties.STYLE, "font:-1");
          // Confidence threshold
        confidenceSlider = new JSlider(0, 100, 50);
        confidenceLabel = new JLabel("Confidence: 0.50");
        
        availableModels = new ArrayList<>();
        
        // Set background
        setOpaque(false);
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("fill,insets 20", "[fill]", "[][][][fill][]"));
        
        // Title
        JLabel titleLabel = new JLabel("Model Testing");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +6");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, "wrap 15");
          // Model selection panel
        JPanel modelPanel = new JPanel(new MigLayout("fill,insets 10", "[grow 0][fill][grow 0]", "[][]"));
        modelPanel.setBorder(BorderFactory.createTitledBorder("Model Selection"));
        
        modelPanel.add(new JLabel("Model:"), "");
        modelPanel.add(modelComboBox, "");
        modelPanel.add(refreshModelsButton, "w 80!, wrap 10");
        
        // Confidence threshold
        modelPanel.add(confidenceLabel, "span 2");
        modelPanel.add(confidenceSlider, "wrap");
        
        add(modelPanel, "wrap 10");
          // Image and controls panel
        JPanel imagePanel = new JPanel(new MigLayout("fill,insets 10", "[grow 0][fill]", "[][fill]"));
        imagePanel.setBorder(BorderFactory.createTitledBorder("Image Testing"));
          JPanel controlPanel = new JPanel(new MigLayout("fill,insets 0", "[]20[]20[]", "[]"));
        controlPanel.add(uploadImageButton, "");
        controlPanel.add(runInferenceButton, "");
        controlPanel.add(toggleDetectionsButton, "");
        
        imagePanel.add(controlPanel, "span 2, wrap 10");
        
        // Image display        
        JScrollPane imageScrollPane = new JScrollPane(imageLabel);
        imageScrollPane.setPreferredSize(new Dimension(450, 350));
        imagePanel.add(imageScrollPane, "");
        
        // Results panel
        JPanel resultsPanel = new JPanel(new MigLayout("fill,insets 0", "[fill]", "[fill]"));
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Detection Results"));
        
        JScrollPane resultsScrollPane = new JScrollPane(resultsArea);
        resultsPanel.add(resultsScrollPane, "");
        
        imagePanel.add(resultsPanel, "");
        
        add(imagePanel, "wrap 10");
        
        // Progress and status
        add(progressBar, "wrap 5");
        add(statusLabel, "");
    }
    
    private void setupEventHandlers() {        refreshModelsButton.addActionListener(e -> refreshAvailableModels());
        uploadImageButton.addActionListener(e -> uploadImage());
        runInferenceButton.addActionListener(e -> runInference());
        toggleDetectionsButton.addActionListener(e -> toggleDetections());
        
        modelComboBox.addActionListener(e -> updateRunButtonState());
        
        confidenceSlider.addChangeListener(e -> {
            double confidence = confidenceSlider.getValue() / 100.0;
            confidenceLabel.setText(String.format("Confidence: %.2f", confidence));
        });
    }
      private void refreshAvailableModels() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Scanning for models...");
            modelComboBox.removeAllItems();
            availableModels.clear();
            
            try {
                scanForModels();
                
                ModelInfo preselectedModel = null;
                for (ModelInfo model : availableModels) {
                    modelComboBox.addItem(model);
                    
                    // Check if this is the preselected model
                    if (preselectedModelPath != null && preselectedModelPath.equals(model.modelPath)) {
                        preselectedModel = model;
                    }
                }
                
                // Select the preselected model if found
                if (preselectedModel != null) {
                    modelComboBox.setSelectedItem(preselectedModel);
                }
                
                statusLabel.setText("Found " + availableModels.size() + " models");
                updateRunButtonState();
                
            } catch (Exception e) {
                statusLabel.setText("Error scanning models: " + e.getMessage());
                JOptionPane.showMessageDialog(this, 
                    "Error scanning for models: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    private void scanForModels() {
        // Get workspace path
        String workspacePath = System.getProperty("user.home") + File.separator + "YoloV8Workspace";
        
        try {
            workspacePath = raven.yolo.manager.WorkspaceManager.getInstance().getCurrentWorkspacePath();
        } catch (Exception e) {
            // Fallback to default
        }
        
        File workspaceDir = new File(workspacePath);
        if (!workspaceDir.exists()) {
            return;
        }
        
        // Scan for project directories
        File[] projectDirs = workspaceDir.listFiles(File::isDirectory);
        if (projectDirs == null) {
            return;
        }
        
        for (File projectDir : projectDirs) {
            try {
                scanProjectForModels(projectDir);
            } catch (Exception e) {
                System.err.println("Error scanning project " + projectDir.getName() + ": " + e.getMessage());
            }
        }
    }
    
    private void scanProjectForModels(File projectDir) {
        // Look for training directories
        File trainingDir = new File(projectDir, "training");
        if (trainingDir.exists()) {
            scanDirectoryForModels(trainingDir, projectDir.getName());
        }
        
        // Look for runs directory
        File runsDir = new File(projectDir, "runs");
        if (runsDir.exists()) {
            scanDirectoryForModels(runsDir, projectDir.getName());
        }
    }
    
    private void scanDirectoryForModels(File directory, String projectId) {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectoryForModels(file, projectId);
            } else if (isModelFile(file)) {
                ModelInfo model = new ModelInfo();
                model.projectId = projectId;
                model.modelName = file.getName();
                model.modelPath = file.getAbsolutePath();
                model.isInferenceSupported = ModelConverter.isInferenceSupported(file.getAbsolutePath());
                
                if (model.isInferenceSupported) {
                    availableModels.add(model);
                }
            }
        }
    }
    
    private boolean isModelFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".pt") || name.endsWith(".onnx") || 
               name.endsWith(".engine") || name.endsWith(".torchscript");
    }
      private void uploadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Image Files", "jpg", "jpeg", "png", "bmp", "gif"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            currentImagePath = selectedFile.getAbsolutePath();
            
            try {
                // Store both original and current image
                originalImage = ImageIO.read(selectedFile);
                currentImage = originalImage;                // Clear previous detections
                lastDetections = null;
                updateToggleButtonState();
                toggleDetectionsButton.setEnabled(false);
                toggleDetectionsButton.setText("Show Original");
                
                displayImage(currentImage);
                statusLabel.setText("Image loaded: " + selectedFile.getName());
                updateRunButtonState();
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "Error loading image: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("Error loading image");
            }
        }
    }
    
    private void displayImage(BufferedImage image) {
        if (image == null) return;
        
        // Calculate scaled dimensions to fit in label
        int labelWidth = imageLabel.getWidth();
        int labelHeight = imageLabel.getHeight();
        
        if (labelWidth <= 0) labelWidth = 400;
        if (labelHeight <= 0) labelHeight = 300;
        
        double scaleX = (double) labelWidth / image.getWidth();
        double scaleY = (double) labelHeight / image.getHeight();
        double scale = Math.min(scaleX, scaleY);
        
        int scaledWidth = (int) (image.getWidth() * scale);
        int scaledHeight = (int) (image.getHeight() * scale);
        
        Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setText("");
    }
    
    private void updateRunButtonState() {
        boolean canRun = modelComboBox.getSelectedItem() != null && currentImage != null;
        runInferenceButton.setEnabled(canRun);
    }
    
    private void runInference() {
        ModelInfo selectedModel = (ModelInfo) modelComboBox.getSelectedItem();
        if (selectedModel == null || currentImagePath == null) {
            JOptionPane.showMessageDialog(this, "Please select a model and upload an image first.", 
                "Missing Requirements", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Disable UI during inference
        runInferenceButton.setEnabled(false);
        uploadImageButton.setEnabled(false);
        modelComboBox.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Running inference...");
        resultsArea.setText("Processing...");
        
        // Run inference in background
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return performInference(selectedModel.modelPath, currentImagePath);
            }
            
            @Override
            protected void done() {
                try {
                    String results = get();
                    
                    // Re-enable UI
                    runInferenceButton.setEnabled(true);
                    uploadImageButton.setEnabled(true);
                    modelComboBox.setEnabled(true);
                    progressBar.setVisible(false);
                    
                    resultsArea.setText(results);
                    statusLabel.setText("Inference completed");
                    
                } catch (Exception e) {
                    // Re-enable UI
                    runInferenceButton.setEnabled(true);
                    uploadImageButton.setEnabled(true);
                    modelComboBox.setEnabled(true);
                    progressBar.setVisible(false);
                    
                    String errorMsg = "Error during inference: " + e.getMessage();
                    resultsArea.setText(errorMsg);
                    statusLabel.setText("Inference failed");
                    
                    JOptionPane.showMessageDialog(ModelTestingForm.this, 
                        errorMsg, "Inference Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }
      private String performInference(String modelPath, String imagePath) throws Exception {
        double confidence = confidenceSlider.getValue() / 100.0;
        
        // Use ModelConverter's runInference method with proper Python environment
        String currentProjectPath = projectPath;
        if (currentProjectPath == null) {
            // Extract project path from model path if not provided
            try {
                String workspacePath = raven.yolo.manager.WorkspaceManager.getInstance().getCurrentWorkspacePath();
                if (modelPath.startsWith(workspacePath)) {
                    String relativePath = modelPath.replace(workspacePath, "");
                    String[] pathParts = relativePath.split(java.util.regex.Pattern.quote(File.separator));
                    if (pathParts.length > 1) {
                        currentProjectPath = workspacePath + File.separator + pathParts[1];
                    }
                }
            } catch (Exception e) {
                currentProjectPath = System.getProperty("user.home") + File.separator + "YoloV8Workspace";
            }
        }
        
        try {
            // Use ModelConverter's runInference method
            String rawOutput = ModelConverter.runInference(modelPath, imagePath, confidence, currentProjectPath);
            return formatInferenceResults(rawOutput);
        } catch (Exception e) {
            throw new Exception("Inference failed: " + e.getMessage(), e);
        }
    }    /**
     * Format the raw inference output for display
     */    private String formatInferenceResults(String rawOutput) {
        try {
            // Debug logging
            System.out.println("=== RAW OUTPUT DEBUG ===");
            System.out.println("Raw output length: " + rawOutput.length());
            System.out.println("Raw output content:");
            System.out.println(rawOutput);
            System.out.println("=== END RAW OUTPUT ===");
            
            // Parse detections from output
            List<DetectionResult> detections;
              // Try JSON format first
            detections = DetectionParserNew.parseDetections(rawOutput);
            System.out.println("JSON Parser found " + detections.size() + " detections");
            
            // If no JSON detections found, try simple text format
            if (detections.isEmpty()) {
                detections = DetectionParserNew.parseSimpleFormat(rawOutput);
                System.out.println("Simple Parser found " + detections.size() + " detections");
            }
            
            // Store detections for rendering
            lastDetections = detections;
            final List<DetectionResult> finalDetections = detections;              // Render detections on image if we have both image and detections
            if (originalImage != null && !detections.isEmpty()) {
                System.out.println("Rendering " + detections.size() + " detections on image");
                SwingUtilities.invokeLater(() -> {
                    BufferedImage imageWithDetections = ImageRenderer.renderDetections(originalImage, finalDetections);
                    currentImage = imageWithDetections;
                    displayImage(currentImage);
                    updateToggleButtonState();
                });
            } else {
                System.out.println("Not rendering detections - originalImage: " + (originalImage != null) + ", detections count: " + detections.size());
                SwingUtilities.invokeLater(() -> {
                    updateToggleButtonState();
                });
            }
            
            // Return formatted text summary
            if (!detections.isEmpty()) {
                String summary = ImageRenderer.getDetectionSummary(detections);
                System.out.println("Generated summary:");
                System.out.println(summary);
                return summary;
            } else {
                // Fallback to original formatting logic
                System.out.println("No detections found, using fallback formatting");
                return formatRawOutput(rawOutput);
            }
            
        } catch (Exception e) {
            System.err.println("Error formatting results: " + e.getMessage());
            e.printStackTrace();
            return formatRawOutput(rawOutput);
        }
    }
    
    /**
     * Fallback method for formatting raw output when parsing fails
     */
    private String formatRawOutput(String rawOutput) {
        try {
            // Find JSON part in the output
            String jsonPart = rawOutput.trim();
            int jsonStart = jsonPart.indexOf("{");
            int jsonEnd = jsonPart.lastIndexOf("}");
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonPart = jsonPart.substring(jsonStart, jsonEnd + 1);
                
                // Check for error in output
                if (jsonPart.contains("\"error\"")) {
                    // Extract error message
                    int errorStart = jsonPart.indexOf("\"error\":");
                    if (errorStart >= 0) {
                        String errorPart = jsonPart.substring(errorStart);
                        int colonIndex = errorPart.indexOf(":");
                        int endIndex = errorPart.indexOf(",");
                        if (endIndex == -1) endIndex = errorPart.indexOf("}");
                        
                        if (colonIndex >= 0 && endIndex > colonIndex) {
                            String errorMsg = errorPart.substring(colonIndex + 1, endIndex)
                                .trim().replace("\"", "");
                            return "Inference Error:\n" + errorMsg + "\n\nPlease check:\n" +
                                   "- Model file is valid\n" +
                                   "- Image file is accessible\n" +
                                   "- Python environment has ultralytics installed";
                        }
                    }
                }
                
                // Parse detections
                if (jsonPart.contains("\"detections\"")) {
                    StringBuilder formatted = new StringBuilder();
                    formatted.append("Detection Results:\n");
                    formatted.append("==================\n\n");
                    
                    // Count detections first
                    int detectionCount = 0;
                    String[] segments = jsonPart.split("\\{");
                    
                    for (String segment : segments) {
                        if (segment.contains("\"class\"") && segment.contains("\"confidence\"") && segment.contains("\"bbox\"")) {
                            detectionCount++;
                            formatted.append("Detection ").append(detectionCount).append(":\n");
                            
                            // Extract class
                            if (segment.contains("\"class\":")) {
                                String classStr = extractValue(segment, "\"class\":");
                                formatted.append("  Class ID: ").append(classStr).append("\n");
                            }
                            
                            // Extract confidence
                            if (segment.contains("\"confidence\":")) {
                                String confStr = extractValue(segment, "\"confidence\":");
                                try {
                                    double conf = Double.parseDouble(confStr);
                                    formatted.append("  Confidence: ").append(String.format("%.2f%%", conf * 100)).append("\n");
                                } catch (NumberFormatException e) {
                                    formatted.append("  Confidence: ").append(confStr).append("\n");
                                }
                            }
                            
                            // Extract bbox
                            if (segment.contains("\"bbox\":")) {
                                String bboxStr = extractBbox(segment);
                                formatted.append("  Bounding Box: ").append(bboxStr).append("\n\n");
                            }
                        }
                    }
                    
                    if (detectionCount == 0) {
                        return "No detections found.\n\nThis could mean:\n" +
                               "- No objects were detected above the confidence threshold\n" +
                               "- The model was not trained on objects present in the image\n" +
                               "- Try lowering the confidence threshold";
                    }
                    
                    formatted.append("Total detections: ").append(detectionCount);
                    return formatted.toString();
                }
            }
            
            // Fallback to raw output
            return "Raw output:\n" + rawOutput;
            
        } catch (Exception e) {
            return "Error formatting results:\n" + rawOutput + "\n\nError: " + e.getMessage();
        }
    }
    
    private String extractValue(String segment, String key) {
        int keyIndex = segment.indexOf(key);
        if (keyIndex >= 0) {
            String afterKey = segment.substring(keyIndex + key.length()).trim();
            int commaIndex = afterKey.indexOf(",");
            int braceIndex = afterKey.indexOf("}");
            int endIndex = (commaIndex >= 0 && braceIndex >= 0) ? Math.min(commaIndex, braceIndex) : 
                          (commaIndex >= 0 ? commaIndex : braceIndex);
            
            if (endIndex > 0) {
                return afterKey.substring(0, endIndex).trim();
            }
        }
        return "";
    }
    
    private String extractBbox(String segment) {
        int bboxIndex = segment.indexOf("\"bbox\":");
        if (bboxIndex >= 0) {
            String afterBbox = segment.substring(bboxIndex + 7).trim();
            int startBracket = afterBbox.indexOf("[");
            int endBracket = afterBbox.indexOf("]");
            
            if (startBracket >= 0 && endBracket > startBracket) {
                return afterBbox.substring(startBracket, endBracket + 1);
            }
        }
        return "";
    }
    
    // Helper class for model information
    private static class ModelInfo {
        String projectId;
        String modelName;
        String modelPath;
        boolean isInferenceSupported;
        
        @Override
        public String toString() {
            return String.format("[%s] %s", projectId, modelName);
        }
    }

    /**
     * Toggle between showing original image and image with detections
     */
    private void toggleDetections() {
        if (originalImage == null) return;
        
        if (currentImage == originalImage) {
            // Currently showing original, switch to detections
            if (lastDetections != null && !lastDetections.isEmpty()) {
                BufferedImage imageWithDetections = ImageRenderer.renderDetections(originalImage, lastDetections);
                currentImage = imageWithDetections;
                displayImage(currentImage);
                toggleDetectionsButton.setText("Show Original");
            }
        } else {
            // Currently showing detections, switch to original
            currentImage = originalImage;
            displayImage(currentImage);
            toggleDetectionsButton.setText("Show Detections");
        }
    }
    
    /**
     * Update toggle button state based on detections availability
     */
    private void updateToggleButtonState() {
        boolean hasDetections = lastDetections != null && !lastDetections.isEmpty();
        toggleDetectionsButton.setEnabled(hasDetections && originalImage != null);
        
        if (hasDetections) {
            if (currentImage == originalImage) {
                toggleDetectionsButton.setText("Show Detections");
            } else {
                toggleDetectionsButton.setText("Show Original");
            }
        } else {
            toggleDetectionsButton.setText("No Detections");
        }
    }
}
