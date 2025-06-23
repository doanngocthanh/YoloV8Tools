package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;

public class ProjectSettings extends JPanel {
    
    private JTextField defaultProjectPathField;
    private JCheckBox showAnnotationLabelsCheckBox;
    private JSlider annotationOpacitySlider;
    private JComboBox<String> exportFormatComboBox;
    private JButton saveButton;
    private JButton resetButton;
    private JButton browseButton;
    
    private Properties settings;
    private final String SETTINGS_FILE = "yolo_settings.properties";
    
    public ProjectSettings() {
        loadSettings();
        initComponents();
        setupLayout();
        setupEventHandlers();
        loadSettingsToUI();
    }
    
    private void loadSettings() {
        settings = new Properties();
        try {
            File settingsFile = new File(SETTINGS_FILE);
            if (settingsFile.exists()) {
                try (FileInputStream fis = new FileInputStream(settingsFile)) {
                    settings.load(fis);
                }
            } else {
                // Set default values
                setDefaultSettings();
            }
        } catch (IOException e) {
            setDefaultSettings();
        }
    }
      private void setDefaultSettings() {
        settings.setProperty("default.project.path", System.getProperty("user.home") + File.separator + "YOLOProjects");
        settings.setProperty("show.annotation.labels", "true");
        settings.setProperty("annotation.opacity", "80");
        settings.setProperty("export.format", "YOLO");
    }
    
    private void initComponents() {
        setLayout(new MigLayout("fill,insets 20", "[fill]", "[][][][][][]20[]"));
        
        // Title
        JLabel titleLabel = new JLabel("Project Settings");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +6");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Default project path
        defaultProjectPathField = new JTextField();
        defaultProjectPathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Default project directory...");
        browseButton = new JButton("Browse...");
        browseButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
          // Annotation display settings
        showAnnotationLabelsCheckBox = new JCheckBox("Show Annotation Labels");
        annotationOpacitySlider = new JSlider(0, 100, 80);
        annotationOpacitySlider.setMajorTickSpacing(25);
        annotationOpacitySlider.setMinorTickSpacing(5);
        annotationOpacitySlider.setPaintTicks(true);
        annotationOpacitySlider.setPaintLabels(true);
        
        // Export format
        exportFormatComboBox = new JComboBox<>(new String[]{"YOLO", "COCO", "Pascal VOC"});
        exportFormatComboBox.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Buttons
        saveButton = new JButton("Save Settings");
        saveButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        resetButton = new JButton("Reset to Default");
        resetButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
          add(titleLabel, "wrap 20");
        add(createProjectPathPanel(), "wrap 15");
        add(createDisplayPanel(), "wrap 15");
        add(createExportPanel(), "wrap 20");
        add(createButtonPanel(), "");
    }
    
    private JPanel createProjectPathPanel() {
        JPanel panel = new JPanel(new MigLayout("fill,insets 10", "[grow 0][fill][grow 0]", "[]"));
        panel.setBorder(BorderFactory.createTitledBorder("Default Project Location"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        
        panel.add(new JLabel("Path:"), "");
        panel.add(defaultProjectPathField, "");
        panel.add(browseButton, "w 80!");
        
        return panel;
    }
      private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new MigLayout("fill,insets 10", "[fill]", "[][]"));
        panel.setBorder(BorderFactory.createTitledBorder("Annotation Display"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        
        JPanel opacityPanel = new JPanel(new MigLayout("fill,insets 0", "[grow 0][fill]", "[]"));
        opacityPanel.add(new JLabel("Opacity:"), "");
        opacityPanel.add(annotationOpacitySlider, "");
        
        panel.add(showAnnotationLabelsCheckBox, "wrap 10");
        panel.add(opacityPanel, "");
        
        return panel;
    }
    
    private JPanel createExportPanel() {
        JPanel panel = new JPanel(new MigLayout("fill,insets 10", "[grow 0][fill]", "[]"));
        panel.setBorder(BorderFactory.createTitledBorder("Export Settings"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        
        panel.add(new JLabel("Default Format:"), "");
        panel.add(exportFormatComboBox, "w 150!");
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0", "[fill][fill]", "[]"));
        
        panel.add(resetButton, "");
        panel.add(saveButton, "");
        
        return panel;
    }
    
    private void setupLayout() {
        setBackground(Color.WHITE);
    }
    
    private void setupEventHandlers() {
        browseButton.addActionListener(e -> browseForDirectory());        saveButton.addActionListener(e -> saveSettings());
        resetButton.addActionListener(e -> resetSettings());
    }
    
    private void browseForDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Default Project Directory");
        
        String currentPath = defaultProjectPathField.getText();
        if (!currentPath.isEmpty()) {
            fileChooser.setCurrentDirectory(new File(currentPath));
        }
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            defaultProjectPathField.setText(selectedDir.getAbsolutePath());
        }
    }
      private void loadSettingsToUI() {
        defaultProjectPathField.setText(settings.getProperty("default.project.path", ""));
        showAnnotationLabelsCheckBox.setSelected(Boolean.parseBoolean(settings.getProperty("show.annotation.labels", "true")));
        annotationOpacitySlider.setValue(Integer.parseInt(settings.getProperty("annotation.opacity", "80")));
        exportFormatComboBox.setSelectedItem(settings.getProperty("export.format", "YOLO"));
    }
      private void saveSettings() {
        try {
            settings.setProperty("default.project.path", defaultProjectPathField.getText());
            settings.setProperty("show.annotation.labels", String.valueOf(showAnnotationLabelsCheckBox.isSelected()));
            settings.setProperty("annotation.opacity", String.valueOf(annotationOpacitySlider.getValue()));
            settings.setProperty("export.format", exportFormatComboBox.getSelectedItem().toString());
            
            try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
                settings.store(fos, "YOLO Annotation Tool Settings");
            }
            
            JOptionPane.showMessageDialog(this, "Settings saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving settings: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void resetSettings() {
        int result = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to reset all settings to default values?", 
            "Confirm Reset", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            setDefaultSettings();
            loadSettingsToUI();
        }
    }
    
    // Getter methods for other components to access settings
    public String getDefaultProjectPath() {
        return settings.getProperty("default.project.path", "");
    }
    
    public boolean isShowAnnotationLabels() {
        return Boolean.parseBoolean(settings.getProperty("show.annotation.labels", "true"));
    }
    
    public int getAnnotationOpacity() {
        return Integer.parseInt(settings.getProperty("annotation.opacity", "80"));
    }
    
    public String getExportFormat() {
        return settings.getProperty("export.format", "YOLO");
    }
}
