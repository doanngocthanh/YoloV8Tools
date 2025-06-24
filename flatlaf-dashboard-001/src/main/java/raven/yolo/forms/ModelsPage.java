package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Models page for listing project IDs from JSON files and their training directories
 */
public class ModelsPage extends JPanel {
    
    private DefaultTableModel tableModel;
    private JTable modelsTable;
    private JTextField searchField;
    private JButton refreshButton;    private JButton loadModelButton;
    private JButton deleteModelButton;
    private JButton convertModelButton;
    private JButton testModelButton;
    private JLabel statusLabel;
    
    public ModelsPage() {
        initComponents();
        setupLayout();
        setupEventHandlers();
        refreshModels();
    }
    
    private void initComponents() {
        // Search field
        searchField = new JTextField();
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search models...");
        searchField.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Buttons
        refreshButton = new JButton("Refresh");
        refreshButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;background:$Component.accentColor");
        
        loadModelButton = new JButton("Load Model");
        loadModelButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;background:$Component.focusColor");
        loadModelButton.setEnabled(false);
          deleteModelButton = new JButton("Delete Model");
        deleteModelButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;background:$Component.error.focusedBorderColor");
        deleteModelButton.setEnabled(false);
        
        convertModelButton = new JButton("Convert Model");
        convertModelButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;background:$Component.focusColor");
        convertModelButton.setEnabled(false);
        
        testModelButton = new JButton("Test Model");
        testModelButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;background:$Component.accentColor");
        testModelButton.setEnabled(false);
        
        // Status label
        statusLabel = new JLabel("Ready");
        statusLabel.putClientProperty(FlatClientProperties.STYLE, "font:-1");
        
        // Table
        String[] columnNames = {"Project ID", "Model Name", "Model Type", "File Size", "Date Created", "Training Directory"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        modelsTable = new JTable(tableModel);
        modelsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modelsTable.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines:true;showVerticalLines:true");
        modelsTable.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "height:30");
        modelsTable.setRowHeight(25);
        
        // Set column widths
        modelsTable.getColumnModel().getColumn(0).setPreferredWidth(120); // Project ID
        modelsTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Model Name
        modelsTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Model Type
        modelsTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // File Size
        modelsTable.getColumnModel().getColumn(4).setPreferredWidth(120); // Date Created
        modelsTable.getColumnModel().getColumn(5).setPreferredWidth(200); // Training Directory
        
        // Set background
        setOpaque(false);
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("fill,insets 20", "[fill]", "[][10][fill][10][]"));
        
        // Title
        JLabel titleLabel = new JLabel("Available Models");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +6");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, "wrap 15");
          // Search and control panel
        JPanel controlPanel = new JPanel(new MigLayout("fill,insets 0", "[fill][grow 0][grow 0][grow 0][grow 0][grow 0]", "[]"));
        controlPanel.setOpaque(false);
        
        controlPanel.add(searchField, "");
        controlPanel.add(refreshButton, "w 80!");
        controlPanel.add(convertModelButton, "w 120!");
        controlPanel.add(testModelButton, "w 100!");
        controlPanel.add(loadModelButton, "w 100!");
        controlPanel.add(deleteModelButton, "w 120!");
        
        add(controlPanel, "wrap");
          // Table
        JScrollPane scrollPane = new JScrollPane(modelsTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Models"));
        scrollPane.putClientProperty(FlatClientProperties.STYLE, "");
        add(scrollPane, "wrap");
        
        // Status bar
        add(statusLabel, "");
    }
      private void setupEventHandlers() {
        refreshButton.addActionListener(e -> refreshModels());
        
        loadModelButton.addActionListener(e -> loadSelectedModel());
        
        deleteModelButton.addActionListener(e -> deleteSelectedModel());
        
        convertModelButton.addActionListener(e -> convertSelectedModel());
        
        testModelButton.addActionListener(e -> testSelectedModel());
        
        // Table selection listener
        modelsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = modelsTable.getSelectedRow() != -1;
                loadModelButton.setEnabled(hasSelection);
                deleteModelButton.setEnabled(hasSelection);
                convertModelButton.setEnabled(hasSelection);
                testModelButton.setEnabled(hasSelection);
            }
        });
        
        // Search field listener
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterModels(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterModels(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterModels(); }
        });
    }
    
    private void refreshModels() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Scanning for models...");
            tableModel.setRowCount(0);
            
            try {
                List<ModelInfo> models = scanForModels();
                
                for (ModelInfo model : models) {
                    Object[] row = {
                        model.projectId,
                        model.modelName,
                        model.modelType,
                        formatFileSize(model.fileSize),
                        model.dateCreated,
                        model.trainingDirectory
                    };
                    tableModel.addRow(row);
                }
                
                statusLabel.setText("Found " + models.size() + " models");
                
            } catch (Exception e) {
                statusLabel.setText("Error scanning models: " + e.getMessage());
                JOptionPane.showMessageDialog(this, 
                    "Error scanning for models: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    private List<ModelInfo> scanForModels() throws IOException {
        List<ModelInfo> models = new ArrayList<>();
        
        // Get workspace path
        String workspacePath = System.getProperty("user.home") + File.separator + "YoloV8Workspace";
        
        // Try to get current workspace from WorkspaceManager if available
        try {
            workspacePath = raven.yolo.manager.WorkspaceManager.getInstance().getCurrentWorkspacePath();
        } catch (Exception e) {
            // Fallback to default
        }
        
        File workspaceDir = new File(workspacePath);
        if (!workspaceDir.exists()) {
            return models;
        }
        
        // Scan for project directories
        File[] projectDirs = workspaceDir.listFiles(File::isDirectory);
        if (projectDirs == null) {
            return models;
        }
        
        for (File projectDir : projectDirs) {
            try {
                // Look for project.json file to get project ID
                File projectJsonFile = new File(projectDir, "project.json");
                if (projectJsonFile.exists()) {
                    String projectId = projectDir.getName(); // Use directory name as project ID
                    
                    // Look for training directories
                    File trainingDir = new File(projectDir, "training");
                    if (trainingDir.exists()) {
                        scanTrainingDirectory(trainingDir, projectId, models);
                    }
                    
                    // Also look for runs directory (YOLOv8 default)
                    File runsDir = new File(projectDir, "runs");
                    if (runsDir.exists()) {
                        scanRunsDirectory(runsDir, projectId, models);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error scanning project directory " + projectDir.getName() + ": " + e.getMessage());
            }
        }
        
        return models;
    }
    
    private void scanTrainingDirectory(File trainingDir, String projectId, List<ModelInfo> models) {
        try (Stream<Path> paths = Files.walk(trainingDir.toPath())) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> {
                     String fileName = path.getFileName().toString().toLowerCase();
                     return fileName.endsWith(".pt") || fileName.endsWith(".onnx") || 
                            fileName.endsWith(".engine") || fileName.endsWith(".torchscript");
                 })
                 .forEach(path -> {
                     try {
                         File modelFile = path.toFile();
                         String modelName = modelFile.getName();
                         String modelType = getModelType(modelName);
                         long fileSize = modelFile.length();
                         String dateCreated = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                             .format(new java.util.Date(modelFile.lastModified()));
                         
                         ModelInfo model = new ModelInfo();
                         model.projectId = projectId;
                         model.modelName = modelName;
                         model.modelType = modelType;
                         model.fileSize = fileSize;
                         model.dateCreated = dateCreated;
                         model.trainingDirectory = path.getParent().toString();
                         
                         models.add(model);
                     } catch (Exception e) {
                         System.err.println("Error processing model file " + path + ": " + e.getMessage());
                     }
                 });
        } catch (IOException e) {
            System.err.println("Error scanning training directory " + trainingDir + ": " + e.getMessage());
        }
    }
    
    private void scanRunsDirectory(File runsDir, String projectId, List<ModelInfo> models) {
        try (Stream<Path> paths = Files.walk(runsDir.toPath())) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> {
                     String fileName = path.getFileName().toString().toLowerCase();
                     return fileName.equals("best.pt") || fileName.equals("last.pt") || 
                            fileName.endsWith(".onnx") || fileName.endsWith(".engine");
                 })
                 .forEach(path -> {
                     try {
                         File modelFile = path.toFile();
                         String modelName = modelFile.getName();
                         String modelType = getModelType(modelName);
                         long fileSize = modelFile.length();
                         String dateCreated = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                             .format(new java.util.Date(modelFile.lastModified()));
                         
                         ModelInfo model = new ModelInfo();
                         model.projectId = projectId;
                         model.modelName = modelName;
                         model.modelType = modelType;
                         model.fileSize = fileSize;
                         model.dateCreated = dateCreated;
                         model.trainingDirectory = path.getParent().toString();
                         
                         models.add(model);
                     } catch (Exception e) {
                         System.err.println("Error processing model file " + path + ": " + e.getMessage());
                     }
                 });
        } catch (IOException e) {
            System.err.println("Error scanning runs directory " + runsDir + ": " + e.getMessage());
        }
    }
    
    private String getModelType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pt")) {
            if (lower.contains("best")) return "Best Model (PyTorch)";
            if (lower.contains("last")) return "Last Model (PyTorch)";
            return "PyTorch Model";
        } else if (lower.endsWith(".onnx")) {
            return "ONNX Model";
        } else if (lower.endsWith(".engine")) {
            return "TensorRT Engine";
        } else if (lower.endsWith(".torchscript")) {
            return "TorchScript Model";
        }
        return "Unknown";
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private void filterModels() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            refreshModels();
            return;
        }
        
        // Simple filtering - remove rows that don't match search
        for (int i = tableModel.getRowCount() - 1; i >= 0; i--) {
            boolean matches = false;
            for (int j = 0; j < tableModel.getColumnCount(); j++) {
                Object value = tableModel.getValueAt(i, j);
                if (value != null && value.toString().toLowerCase().contains(searchText)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
                tableModel.removeRow(i);
            }
        }
    }
    
    private void loadSelectedModel() {
        int selectedRow = modelsTable.getSelectedRow();
        if (selectedRow == -1) return;
        
        String projectId = (String) tableModel.getValueAt(selectedRow, 0);
        String modelName = (String) tableModel.getValueAt(selectedRow, 1);
        String trainingDir = (String) tableModel.getValueAt(selectedRow, 5);
        
        // Show model loading dialog or perform loading operation
        int result = JOptionPane.showConfirmDialog(this,
            "Load model '" + modelName + "' from project '" + projectId + "'?\n" +
            "Training Directory: " + trainingDir,
            "Load Model",
            JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            // TODO: Implement model loading logic
            statusLabel.setText("Loading model: " + modelName);
            
            // For now, just show success message
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                    "Model loading functionality will be implemented in the next version.",
                    "Information",
                    JOptionPane.INFORMATION_MESSAGE);
                statusLabel.setText("Ready");
            });
        }
    }
    
    private void deleteSelectedModel() {
        int selectedRow = modelsTable.getSelectedRow();
        if (selectedRow == -1) return;
        
        String projectId = (String) tableModel.getValueAt(selectedRow, 0);
        String modelName = (String) tableModel.getValueAt(selectedRow, 1);
        String trainingDir = (String) tableModel.getValueAt(selectedRow, 5);
        
        int result = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete model '" + modelName + "' from project '" + projectId + "'?\n" +
            "This action cannot be undone!",
            "Delete Model",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (result == JOptionPane.YES_OPTION) {
            try {
                // Construct the full path to the model file
                String modelPath = trainingDir + File.separator + modelName;
                File modelFile = new File(modelPath);
                
                if (modelFile.exists() && modelFile.delete()) {
                    statusLabel.setText("Deleted model: " + modelName);
                    tableModel.removeRow(selectedRow);
                    JOptionPane.showMessageDialog(this,
                        "Model deleted successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    throw new IOException("Failed to delete model file: " + modelPath);
                }
            } catch (Exception e) {
                statusLabel.setText("Error deleting model: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                    "Error deleting model: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
      private void convertSelectedModel() {
        int selectedRow = modelsTable.getSelectedRow();
        if (selectedRow == -1) return;
        
        String projectId = (String) tableModel.getValueAt(selectedRow, 0);
        String modelName = (String) tableModel.getValueAt(selectedRow, 1);
        String trainingDir = (String) tableModel.getValueAt(selectedRow, 5);
        String modelPath = trainingDir + File.separator + modelName;
        
        // Get project path for Python environment
        String projectPath = getProjectPath(projectId);
        
        // Check if model can be converted
        if (!raven.yolo.utils.ModelConverter.getAvailableConversions(modelPath).isEmpty()) {
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            ModelConversionDialog dialog = new ModelConversionDialog(parentFrame, modelPath, projectPath);
            dialog.setVisible(true);
            
            // Refresh models after conversion dialog closes
            SwingUtilities.invokeLater(() -> refreshModels());
        } else {
            JOptionPane.showMessageDialog(this,
                "This model format cannot be converted. Only PyTorch (.pt) models can be converted to other formats.",
                "Conversion Not Available",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
      private void testSelectedModel() {
        int selectedRow = modelsTable.getSelectedRow();
        if (selectedRow == -1) return;
        
        String projectId = (String) tableModel.getValueAt(selectedRow, 0);
        String modelName = (String) tableModel.getValueAt(selectedRow, 1);
        String trainingDir = (String) tableModel.getValueAt(selectedRow, 5);
        String modelPath = trainingDir + File.separator + modelName;
        
        // Get project path for Python environment
        String projectPath = getProjectPath(projectId);
        
        // Check if model supports inference
        if (raven.yolo.utils.ModelConverter.isInferenceSupported(modelPath)) {
            // Open model testing form with project context
            ModelTestingForm testingForm = new ModelTestingForm(modelPath, projectPath);
            raven.components.FormManager.getInstance().showForm(
                "Test Model: " + modelName, 
                testingForm, 
                true
            );
        } else {
            JOptionPane.showMessageDialog(this,
                "This model format is not supported for inference testing.",
                "Testing Not Available",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Get project path from project ID
     */
    private String getProjectPath(String projectId) {
        try {
            String workspacePath = raven.yolo.manager.WorkspaceManager.getInstance().getCurrentWorkspacePath();
            return workspacePath + File.separator + projectId;
        } catch (Exception e) {
            // Fallback to default workspace
            String defaultWorkspace = System.getProperty("user.home") + File.separator + "YoloV8Workspace";
            return defaultWorkspace + File.separator + projectId;
        }
    }

    // Helper class to store model information
    private static class ModelInfo {
        String projectId;
        String modelName;
        String modelType;
        long fileSize;
        String dateCreated;
        String trainingDirectory;
    }
}
