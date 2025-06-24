package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.utils.ModelConverter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Dialog for converting models to different formats
 */
public class ModelConversionDialog extends JDialog {
      private JTextField modelPathField;
    private JButton browseButton;
    private JComboBox<ModelConverter.ModelFormat> formatComboBox;
    private JButton convertButton;
    private JButton cancelButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private String selectedModelPath;
    private String projectPath;
    
    public ModelConversionDialog(Frame parent, String modelPath, String projectPath) {
        super(parent, "Convert Model", true);
        this.selectedModelPath = modelPath;
        this.projectPath = projectPath;
        initComponents();
        setupLayout();
        setupEventHandlers();
        updateAvailableFormats();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
    }
    
    // Backward compatibility constructor
    public ModelConversionDialog(Frame parent, String modelPath) {
        this(parent, modelPath, null);
    }
    
    private void initComponents() {
        // Model path
        modelPathField = new JTextField(selectedModelPath);
        modelPathField.setEditable(false);
        modelPathField.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        browseButton = new JButton("Browse...");
        browseButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Format selection
        formatComboBox = new JComboBox<>();
        formatComboBox.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Buttons
        convertButton = new JButton("Convert");
        convertButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;background:$Component.accentColor");
        
        cancelButton = new JButton("Cancel");
        cancelButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Progress
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        progressBar.setVisible(false);
        
        statusLabel = new JLabel("Ready to convert");
        statusLabel.putClientProperty(FlatClientProperties.STYLE, "font:-1");
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("fill,insets 20", "[fill]", "[][][][][][]"));
        
        // Title
        JLabel titleLabel = new JLabel("Model Conversion");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, "wrap 15");
        
        // Model path panel
        JPanel pathPanel = new JPanel(new MigLayout("fill,insets 0", "[fill][grow 0]", "[]"));
        pathPanel.add(new JLabel("Model File:"), "wrap 5");
        pathPanel.add(modelPathField, "");
        pathPanel.add(browseButton, "w 80!");
        add(pathPanel, "wrap 10");
        
        // Format selection
        add(new JLabel("Convert to:"), "wrap 5");
        add(formatComboBox, "wrap 15");
        
        // Progress
        add(progressBar, "wrap 5");
        add(statusLabel, "wrap 15");
        
        // Buttons
        JPanel buttonPanel = new JPanel(new MigLayout("fill,insets 0", "[fill][fill]", "[]"));
        buttonPanel.add(cancelButton, "");
        buttonPanel.add(convertButton, "");
        add(buttonPanel, "");
    }
      private void setupEventHandlers() {
        browseButton.addActionListener(e -> browseForModel());
        convertButton.addActionListener(e -> convertModel());
        cancelButton.addActionListener(e -> dispose());
        
        modelPathField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateAvailableFormats(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateAvailableFormats(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateAvailableFormats(); }
        });
        
        formatComboBox.addActionListener(e -> updateConvertButtonState());
    }
    
    private void browseForModel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Model Files", "pt", "onnx", "engine", "torchscript"));
        
        if (selectedModelPath != null) {
            fileChooser.setSelectedFile(new File(selectedModelPath));
        }
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedModelPath = fileChooser.getSelectedFile().getAbsolutePath();
            modelPathField.setText(selectedModelPath);
            updateAvailableFormats();
        }
    }
      private void updateAvailableFormats() {
        formatComboBox.removeAllItems();
        
        if (selectedModelPath != null && !selectedModelPath.isEmpty()) {
            List<ModelConverter.ModelFormat> formats = ModelConverter.getAvailableConversions(selectedModelPath);
            
            for (ModelConverter.ModelFormat format : formats) {
                formatComboBox.addItem(format);
            }
            
            updateConvertButtonState();
        } else {
            convertButton.setEnabled(false);
        }
    }
    
    private void updateConvertButtonState() {
        boolean hasModel = selectedModelPath != null && !selectedModelPath.isEmpty();
        boolean hasFormat = formatComboBox.getSelectedItem() != null;
        convertButton.setEnabled(hasModel && hasFormat);
    }
      private void convertModel() {
        if (selectedModelPath == null || selectedModelPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a model file first.", 
                "No Model Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        ModelConverter.ModelFormat selectedFormat = (ModelConverter.ModelFormat) formatComboBox.getSelectedItem();
        if (selectedFormat == null) {
            JOptionPane.showMessageDialog(this, "Please select a conversion format.", 
                "No Format Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Set up Python environment before conversion
        try {
            String currentProjectPath = projectPath;
            if (currentProjectPath != null) {
                // Extract project ID from path
                String projectId = new File(currentProjectPath).getName();
                String workspacePath = new File(currentProjectPath).getParent();
                
                // Ensure Python environment is set up for the current project
                System.out.println("Setting up Python environment for project: " + projectId);
                raven.yolo.training.PythonSetupManager.getInstance().setCurrentProject(projectId, workspacePath);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not set up Python environment: " + e.getMessage());
        }
        
        // Disable UI during conversion
        convertButton.setEnabled(false);
        browseButton.setEnabled(false);
        formatComboBox.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Converting model...");
        
        // Perform conversion in background
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    File modelFile = new File(selectedModelPath);                    String outputPath = modelFile.getParent() + File.separator + 
                        modelFile.getName().replaceFirst("\\.[^.]+$", selectedFormat.getExtension());
                    
                    // Use project path if available, otherwise fall back to deprecated methods
                    String currentProjectPath = projectPath;
                    if (currentProjectPath == null) {
                        // Extract project path from model path
                        try {
                            String workspacePath = raven.yolo.manager.WorkspaceManager.getInstance().getCurrentWorkspacePath();
                            String relativePath = modelFile.getAbsolutePath().replace(workspacePath, "");
                            String[] pathParts = relativePath.split(java.util.regex.Pattern.quote(File.separator));
                            if (pathParts.length > 1) {
                                currentProjectPath = workspacePath + File.separator + pathParts[1];
                            }
                        } catch (Exception e) {
                            currentProjectPath = System.getProperty("user.home") + File.separator + "YoloV8Workspace";
                        }
                    }
                    
                    switch (selectedFormat) {
                        case ONNX:
                            return ModelConverter.convertToOnnx(selectedModelPath, outputPath, currentProjectPath);
                        case TENSORRT:
                            return ModelConverter.convertToTensorRT(selectedModelPath, outputPath, currentProjectPath);
                        case TORCHSCRIPT:
                            return ModelConverter.convertToTorchScript(selectedModelPath, outputPath, currentProjectPath);
                        default:
                            return false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    
                    // Re-enable UI
                    convertButton.setEnabled(true);
                    browseButton.setEnabled(true);
                    formatComboBox.setEnabled(true);
                    progressBar.setVisible(false);
                    
                    if (success) {
                        statusLabel.setText("Conversion completed successfully!");
                        JOptionPane.showMessageDialog(ModelConversionDialog.this, 
                            "Model converted successfully to " + selectedFormat.name() + " format!", 
                            "Conversion Complete", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        statusLabel.setText("Conversion failed");
                        JOptionPane.showMessageDialog(ModelConversionDialog.this, 
                            "Model conversion failed. Please check that Python and Ultralytics are properly installed.", 
                            "Conversion Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    // Re-enable UI
                    convertButton.setEnabled(true);
                    browseButton.setEnabled(true);
                    formatComboBox.setEnabled(true);
                    progressBar.setVisible(false);
                    statusLabel.setText("Conversion error");
                    
                    JOptionPane.showMessageDialog(ModelConversionDialog.this, 
                        "Error during conversion: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }
}
