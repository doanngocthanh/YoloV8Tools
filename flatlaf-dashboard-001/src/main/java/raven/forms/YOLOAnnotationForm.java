package raven.forms;

import com.formdev.flatlaf.FlatClientProperties;
import raven.yolo.components.ClassPanel;
import raven.yolo.components.ImageViewer;
import raven.yolo.model.BoundingBox;
import raven.yolo.model.ClassManager;
import raven.yolo.utils.YOLOFileUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Main YOLO Annotation Form
 */
public class YOLOAnnotationForm extends JPanel {
    private ClassManager classManager;
    private ImageViewer imageViewer;
    private ClassPanel classPanel;
    
    // UI Components
    private JButton openImageButton;
    private JButton openFolderButton;
    private JButton saveButton;
    private JButton prevButton;
    private JButton nextButton;
    private JButton clearButton;
    private JButton exportButton;
    private JLabel imageInfoLabel;
    private JLabel instructionsLabel;
    
    // Image management
    private List<String> imageFiles;
    private int currentImageIndex = -1;
    private String currentImagePath;
    
    public YOLOAnnotationForm() {
        classManager = new ClassManager();
        initComponents();
        setupLayout();
        setupEventListeners();
        setupKeyBindings();
        updateUI();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.DARK_GRAY);
        
        // Image viewer
        imageViewer = new ImageViewer(classManager);
        
        // Class panel
        classPanel = new ClassPanel(classManager);
        
        // Buttons
        openImageButton = new JButton("Open Image");
        openFolderButton = new JButton("Open Folder");
        saveButton = new JButton("Save");
        prevButton = new JButton("← Previous");
        nextButton = new JButton("Next →");
        clearButton = new JButton("Clear All");
        exportButton = new JButton("Export Dataset");
        
        // Style buttons
        openImageButton.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor");
        openFolderButton.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor");
        saveButton.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor");
        clearButton.putClientProperty(FlatClientProperties.STYLE, "background:$Component.error.focusedBorderColor");
        exportButton.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor");
        
        // Labels
        imageInfoLabel = new JLabel("No image loaded");
        imageInfoLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        
        instructionsLabel = new JLabel("<html><b>Instructions:</b><br>" +
            "• Left click and drag to create bounding box<br>" +
            "• Right click on box to delete<br>" +
            "• Left click on box to select<br>" +
            "• Use arrow keys to navigate images</html>");
        instructionsLabel.putClientProperty(FlatClientProperties.STYLE, "font:-2");
    }
    
    private void setupLayout() {
        // Top toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(openImageButton);
        toolbar.add(openFolderButton);
        toolbar.add(saveButton);
        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(prevButton);
        toolbar.add(nextButton);
        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(clearButton);
        toolbar.add(exportButton);
        
        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusPanel.add(imageInfoLabel, BorderLayout.WEST);
        
        // Right side panel
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(280, 0));
        rightPanel.add(classPanel, BorderLayout.NORTH);
        rightPanel.add(instructionsLabel, BorderLayout.SOUTH);
        
        // Image viewer in scroll pane
        JScrollPane imageScrollPane = new JScrollPane(imageViewer);
        imageScrollPane.setPreferredSize(new Dimension(800, 600));
        imageScrollPane.putClientProperty(FlatClientProperties.STYLE, "border:0,0,0,0");
        
        add(toolbar, BorderLayout.NORTH);
        add(imageScrollPane, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventListeners() {
        openImageButton.addActionListener(e -> openImage());
        openFolderButton.addActionListener(e -> openFolder());
        saveButton.addActionListener(e -> saveAnnotations());
        prevButton.addActionListener(e -> loadPreviousImage());
        nextButton.addActionListener(e -> loadNextImage());
        clearButton.addActionListener(e -> clearAnnotations());
        exportButton.addActionListener(e -> exportDataset());
    }
    
    private void setupKeyBindings() {
        // Setup key bindings for navigation
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previous");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "next");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "save");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        
        actionMap.put("previous", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                loadPreviousImage();
            }
        });
        
        actionMap.put("next", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                loadNextImage();
            }
        });
        
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveAnnotations();
            }
        });
        
        actionMap.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                imageViewer.deleteSelectedBoundingBox();
            }
        });
    }
    
    private void openImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "bmp"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadImage(selectedFile.getAbsolutePath());
            imageFiles = null; // Clear folder mode
            currentImageIndex = -1;
        }
    }
    
    private void openFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            imageFiles = YOLOFileUtils.getImageFiles(selectedFolder.getAbsolutePath());
            
            if (!imageFiles.isEmpty()) {
                currentImageIndex = 0;
                loadImage(imageFiles.get(0));
                
                // Try to load classes.txt if exists
                try {
                    YOLOFileUtils.loadClasses(classManager, selectedFolder.getAbsolutePath());
                    classPanel.refreshClassList();
                } catch (Exception e) {
                    System.out.println("No classes.txt found or error loading: " + e.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(this, "No image files found in the selected folder.");
            }
        }
    }
    
    private void loadImage(String imagePath) {
        try {
            BufferedImage image = YOLOFileUtils.loadImage(imagePath);
            imageViewer.setImage(image);
            currentImagePath = imagePath;
            
            // Load existing annotations if any
            List<BoundingBox> annotations = YOLOFileUtils.loadAnnotations(imagePath, classManager);
            imageViewer.setBoundingBoxes(annotations);
              updateUIState();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading image: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadPreviousImage() {
        if (imageFiles != null && currentImageIndex > 0) {
            saveAnnotations(); // Auto-save current annotations
            currentImageIndex--;
            loadImage(imageFiles.get(currentImageIndex));
        }
    }
    
    private void loadNextImage() {
        if (imageFiles != null && currentImageIndex < imageFiles.size() - 1) {
            saveAnnotations(); // Auto-save current annotations
            currentImageIndex++;
            loadImage(imageFiles.get(currentImageIndex));
        }
    }
    
    private void saveAnnotations() {
        if (currentImagePath != null) {
            try {
                List<BoundingBox> annotations = imageViewer.getBoundingBoxes();
                YOLOFileUtils.saveAnnotations(annotations, currentImagePath);
                JOptionPane.showMessageDialog(this, "Annotations saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error saving annotations: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void clearAnnotations() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to clear all annotations for this image?",
            "Confirm Clear",
            JOptionPane.YES_NO_OPTION
        );
        if (result == JOptionPane.YES_OPTION) {
            imageViewer.clearBoundingBoxes();
        }
    }
    
    private void exportDataset() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Export Directory");
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File exportDir = fileChooser.getSelectedFile();
            
            try {
                // Create YOLO dataset structure
                YOLOFileUtils.createYOLODatasetStructure(exportDir.getAbsolutePath());
                
                // Save classes.txt
                YOLOFileUtils.saveClasses(classManager, exportDir.getAbsolutePath());
                
                // Generate data.yaml
                YOLOFileUtils.generateDataYaml(exportDir.getAbsolutePath(), classManager);
                
                JOptionPane.showMessageDialog(this, 
                    "Dataset structure created successfully!\n" +
                    "Remember to organize your images and labels into train/val folders.",
                    "Export Complete", 
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error exporting dataset: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void updateUIState() {
        boolean hasImage = currentImagePath != null;
        boolean hasImageList = imageFiles != null && !imageFiles.isEmpty();
        
        saveButton.setEnabled(hasImage);
        clearButton.setEnabled(hasImage);
        prevButton.setEnabled(hasImageList && currentImageIndex > 0);
        nextButton.setEnabled(hasImageList && currentImageIndex < imageFiles.size() - 1);
        
        if (hasImage) {
            File imageFile = new File(currentImagePath);
            String info = imageFile.getName();
            if (hasImageList) {
                info += String.format(" (%d/%d)", currentImageIndex + 1, imageFiles.size());
            }
            imageInfoLabel.setText(info);
        } else {
            imageInfoLabel.setText("No image loaded");
        }
    }
}
