package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.manager.ProjectManager;
import raven.yolo.model.YoloProject;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class ExportDatasetDialog extends JDialog {
    
    private JTextField exportPathField;
    private JButton browseButton;
    private JComboBox<String> formatComboBox;
    private JSpinner trainSplitSpinner;
    private JSpinner valSplitSpinner;
    private JCheckBox includeTestSplitCheckBox;
    private JSpinner testSplitSpinner;
    private JCheckBox createDataYamlCheckBox;
    private JCheckBox copyImagesCheckBox;
    private JButton exportButton;
    private JButton cancelButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    
    private boolean exportInProgress = false;
    
    public ExportDatasetDialog(Frame parent) {
        super(parent, "Export Dataset", true);
        initComponents();
        setupLayout();
        setupEventHandlers();
        
        setSize(600, 450);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    private void initComponents() {
        // Export path
        exportPathField = new JTextField();
        exportPathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Select export directory...");
        browseButton = new JButton("Browse...");
        browseButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Format selection
        formatComboBox = new JComboBox<>(new String[]{"YOLO", "COCO", "Pascal VOC"});
        formatComboBox.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Data split configuration
        trainSplitSpinner = new JSpinner(new SpinnerNumberModel(80, 10, 90, 5));
        valSplitSpinner = new JSpinner(new SpinnerNumberModel(20, 10, 90, 5));
        includeTestSplitCheckBox = new JCheckBox("Include Test Split");
        testSplitSpinner = new JSpinner(new SpinnerNumberModel(10, 5, 50, 5));
        testSplitSpinner.setEnabled(false);
        
        // Export options
        createDataYamlCheckBox = new JCheckBox("Create data.yaml file", true);
        copyImagesCheckBox = new JCheckBox("Copy images to export directory", true);
        
        // Control buttons
        exportButton = new JButton("Export Dataset");
        exportButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        cancelButton = new JButton("Cancel");
        cancelButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Progress
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready to export");
        progressBar.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(Color.GRAY);
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("fill,insets 20", "[fill]", "[][][][][][]20[][]"));
        
        // Title
        JLabel titleLabel = new JLabel("Export YOLO Dataset");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, "wrap 20");
        
        // Export path panel
        JPanel pathPanel = new JPanel(new MigLayout("fill,insets 10", "[grow 0][fill][grow 0]", "[]"));
        pathPanel.setBorder(BorderFactory.createTitledBorder("Export Location"));
        pathPanel.add(new JLabel("Path:"), "");
        pathPanel.add(exportPathField, "");
        pathPanel.add(browseButton, "w 80!");
        add(pathPanel, "wrap 15");
        
        // Format panel
        JPanel formatPanel = new JPanel(new MigLayout("fill,insets 10", "[grow 0][fill]", "[]"));
        formatPanel.setBorder(BorderFactory.createTitledBorder("Export Format"));
        formatPanel.add(new JLabel("Format:"), "");
        formatPanel.add(formatComboBox, "w 150!");
        add(formatPanel, "wrap 15");
        
        // Data split panel
        add(createDataSplitPanel(), "wrap 15");
        
        // Options panel
        add(createOptionsPanel(), "wrap 20");
        
        // Progress panel
        JPanel progressPanel = new JPanel(new MigLayout("fill,insets 0", "[fill]", "[][]"));
        progressPanel.add(progressBar, "wrap 5");
        progressPanel.add(statusLabel, "");
        add(progressPanel, "wrap 10");
        
        // Button panel
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[fill][fill]", "[]"));
        buttonPanel.add(cancelButton, "");
        buttonPanel.add(exportButton, "");
        add(buttonPanel, "");
    }
    
    private JPanel createDataSplitPanel() {
        JPanel panel = new JPanel(new MigLayout("fill,insets 10", "[grow 0][grow 0][grow 0][grow 0][fill]", "[][]"));
        panel.setBorder(BorderFactory.createTitledBorder("Data Split Configuration"));
        
        panel.add(new JLabel("Train:"), "");
        panel.add(trainSplitSpinner, "w 60!");
        panel.add(new JLabel("%"), "");
        panel.add(new JLabel("Val:"), "gapleft 20");
        panel.add(valSplitSpinner, "w 60!");
        panel.add(new JLabel("%"), "wrap 10");
        
        panel.add(includeTestSplitCheckBox, "span 2");
        panel.add(new JLabel("Test:"), "");
        panel.add(testSplitSpinner, "w 60!");
        panel.add(new JLabel("%"), "");
        
        return panel;
    }
    
    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new MigLayout("fill,insets 10", "[fill]", "[][]"));
        panel.setBorder(BorderFactory.createTitledBorder("Export Options"));
        
        panel.add(createDataYamlCheckBox, "wrap 5");
        panel.add(copyImagesCheckBox, "");
        
        return panel;
    }
    
    private void setupEventHandlers() {
        browseButton.addActionListener(e -> browseForDirectory());
        exportButton.addActionListener(e -> exportDataset());
        cancelButton.addActionListener(e -> {
            if (exportInProgress) {
                // TODO: Cancel export operation
                exportInProgress = false;
            } else {
                dispose();
            }
        });
        
        // Enable/disable test split spinner
        includeTestSplitCheckBox.addActionListener(e -> {
            testSplitSpinner.setEnabled(includeTestSplitCheckBox.isSelected());
            updateSplitPercentages();
        });
        
        // Update split percentages when values change
        trainSplitSpinner.addChangeListener(e -> updateSplitPercentages());
        valSplitSpinner.addChangeListener(e -> updateSplitPercentages());
        testSplitSpinner.addChangeListener(e -> updateSplitPercentages());
    }
    
    private void browseForDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Export Directory");
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            exportPathField.setText(selectedDir.getAbsolutePath());
        }
    }
    
    private void updateSplitPercentages() {
        // Ensure percentages add up to 100
        int train = (Integer) trainSplitSpinner.getValue();
        int val = (Integer) valSplitSpinner.getValue();
        int test = includeTestSplitCheckBox.isSelected() ? (Integer) testSplitSpinner.getValue() : 0;
        
        int total = train + val + test;
        if (total != 100) {
            // Adjust validation split to make total 100
            int newVal = 100 - train - test;
            if (newVal >= 0 && newVal <= 90) {
                valSplitSpinner.setValue(newVal);
            }
        }
    }
    
    private void exportDataset() {
        YoloProject project = ProjectManager.getInstance().getCurrentProject();
        if (project == null) {
            JOptionPane.showMessageDialog(this, "No project is currently open.", "No Project", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String exportPath = exportPathField.getText().trim();
        if (exportPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an export directory.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (project.getImages().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No images found in the project.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        exportInProgress = true;
        exportButton.setEnabled(false);
        cancelButton.setText("Cancel");
        
        // Create export worker
        SwingWorker<Void, String> exportWorker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    publish("Preparing export...");
                    Thread.sleep(500); // Simulate preparation
                    
                    // Get split ratios
                    double trainRatio = (Integer) trainSplitSpinner.getValue() / 100.0;
                    double valRatio = (Integer) valSplitSpinner.getValue() / 100.0;
                    double testRatio = includeTestSplitCheckBox.isSelected() ? (Integer) testSplitSpinner.getValue() / 100.0 : 0.0;
                    
                    publish("Creating directory structure...");
                    setProgress(10);
                    
                    // Create export directory structure
                    File exportDir = new File(exportPath);
                    exportDir.mkdirs();
                    
                    publish("Exporting annotations...");
                    setProgress(50);
                    
                    // Use ProjectManager to export
                    ProjectManager.getInstance().exportDataset(exportPath);
                    
                    publish("Export completed successfully!");
                    setProgress(100);
                    
                } catch (IOException e) {
                    throw new RuntimeException("Export failed: " + e.getMessage(), e);
                }
                
                return null;
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    statusLabel.setText(message);
                }
            }
            
            @Override
            protected void done() {
                try {
                    get(); // This will throw an exception if the background task failed
                    
                    progressBar.setValue(100);
                    progressBar.setString("Export Complete");
                    statusLabel.setText("Dataset exported successfully!");
                    
                    JOptionPane.showMessageDialog(ExportDatasetDialog.this, 
                        "Dataset exported successfully to:\n" + exportPath, 
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    
                } catch (Exception e) {
                    progressBar.setValue(0);
                    progressBar.setString("Export Failed");
                    statusLabel.setText("Export failed: " + e.getMessage());
                    
                    JOptionPane.showMessageDialog(ExportDatasetDialog.this, 
                        "Export failed: " + e.getMessage(), 
                        "Export Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    exportInProgress = false;
                    exportButton.setEnabled(true);
                    cancelButton.setText("Close");
                }
            }
        };
        
        exportWorker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                int progress = (Integer) evt.getNewValue();
                progressBar.setValue(progress);
                progressBar.setString(progress + "%");
            }
        });
        
        exportWorker.execute();
    }
}
