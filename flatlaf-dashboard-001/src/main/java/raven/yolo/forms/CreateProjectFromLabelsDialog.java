package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.manager.ProjectManager;
import raven.yolo.manager.WorkspaceManager;
import raven.yolo.model.YoloAnnotation;
import raven.yolo.model.YoloImage;
import raven.yolo.model.YoloProject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Dialog for creating a new project from cropped labeled objects
 */
public class CreateProjectFromLabelsDialog extends JDialog {
    private JTextField projectNameField;
    private JTextArea descriptionArea;
    private JCheckBox preserveClassesCheckBox;
    private JButton createButton;
    private JButton cancelButton;    private JProgressBar progressBar;
    private JLabel statusLabel;
    private YoloProject sourceProject;
    private boolean confirmed = false;
    private SwingWorker<Boolean, String> currentWorker;
    
    public CreateProjectFromLabelsDialog(Frame parent, YoloProject sourceProject) {
        super(parent, "Create Project from Labels", true);
        this.sourceProject = sourceProject;
        initComponents();
        setupLayout();
        setupEventHandlers();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        // Project name
        projectNameField = new JTextField(sourceProject.getName() + "_cropped");
        projectNameField.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Description
        descriptionArea = new JTextArea(3, 30);
        descriptionArea.setText("Project created from cropped labeled objects of: " + sourceProject.getName());
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Options
        preserveClassesCheckBox = new JCheckBox("Preserve original class names", true);
        
        // Buttons
        createButton = new JButton("Create Project");
        createButton.putClientProperty(FlatClientProperties.STYLE, "arc:5;background:$Component.accentColor");
        
        cancelButton = new JButton("Cancel");
        cancelButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        // Progress
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        progressBar.setVisible(false);
        
        statusLabel = new JLabel("Ready to create project");
        statusLabel.putClientProperty(FlatClientProperties.STYLE, "font:-1");
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("fill,insets 20", "[fill]", "[][][][][][][][]"));
        
        // Title
        JLabel titleLabel = new JLabel("Create Project from Labels");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, "wrap 15");
        
        // Info
        JLabel infoLabel = new JLabel("<html><i>This will create a new project with cropped images from all labeled objects in the current project.</i></html>");
        add(infoLabel, "wrap 10");
        
        // Project name
        add(new JLabel("Project Name:"), "wrap 5");
        add(projectNameField, "wrap 10");
        
        // Description
        add(new JLabel("Description:"), "wrap 5");
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        add(descScrollPane, "wrap 10");
        
        // Options
        add(preserveClassesCheckBox, "wrap 15");
        
        // Progress
        add(progressBar, "wrap 5");
        add(statusLabel, "wrap 15");
        
        // Buttons
        JPanel buttonPanel = new JPanel(new MigLayout("fill,insets 0", "[fill][fill]", "[]"));
        buttonPanel.add(cancelButton, "");
        buttonPanel.add(createButton, "");
        add(buttonPanel, "");
    }
      private void setupEventHandlers() {
        createButton.addActionListener(e -> createProject());
        cancelButton.addActionListener(e -> {
            if (currentWorker != null && !currentWorker.isDone()) {
                currentWorker.cancel(true);
            }
            dispose();
        });
    }
    
    private void createProject() {
        String projectName = projectNameField.getText().trim();
        if (projectName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a project name.", 
                "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check if project already exists
        String workspacePath = WorkspaceManager.getInstance().getCurrentWorkspacePath();
        File projectDir = new File(workspacePath, projectName);
        if (projectDir.exists()) {
            JOptionPane.showMessageDialog(this, 
                "A project with this name already exists. Please choose a different name.", 
                "Project Exists", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Disable UI during creation
        createButton.setEnabled(false);
        cancelButton.setEnabled(false);
        projectNameField.setEnabled(false);
        descriptionArea.setEnabled(false);
        preserveClassesCheckBox.setEnabled(false);        progressBar.setVisible(true);
        statusLabel.setText("Starting...");        // Create project in background
        currentWorker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    publish("Initializing project...");
                    
                    String workspacePath = WorkspaceManager.getInstance().getCurrentWorkspacePath();
                    File projectDir = new File(workspacePath, projectName);
                    
                    // Create project directory structure
                    if (!projectDir.mkdirs()) {
                        throw new IOException("Failed to create project directory");
                    }
                    
                    File imagesDir = new File(projectDir, "images");
                    File labelsDir = new File(projectDir, "labels");
                    
                    if (!imagesDir.mkdirs() || !labelsDir.mkdirs()) {
                        throw new IOException("Failed to create project subdirectories");
                    }
                    
                    // Create new project
                    YoloProject newProject = new YoloProject();
                    newProject.setName(projectName);
                    newProject.setDescription(descriptionArea.getText().trim());
                    newProject.setProjectPath(projectDir.getAbsolutePath());
                    
                    // Copy classes if preserving
                    if (preserveClassesCheckBox.isSelected()) {
                        List<String> sourceClasses = sourceProject.getClasses();
                        for (String className : sourceClasses) {
                            newProject.addClass(className);
                        }
                    }
                    
                    // Process each image and its annotations
                    List<YoloImage> sourceImages = sourceProject.getImages();
                    int totalObjects = sourceImages.stream().mapToInt(img -> img.getAnnotations().size()).sum();
                    int processedObjects = 0;
                    
                    for (YoloImage sourceImage : sourceImages) {
                        // Check if task was cancelled
                        if (isCancelled()) {
                            return false;
                        }
                        
                        if (sourceImage.getAnnotations().isEmpty()) {
                            continue;
                        }
                        
                        publish("Processing " + sourceImage.getFilename() + "...");
                        
                        // Load source image
                        BufferedImage sourceImg;
                        try {
                            sourceImg = ImageIO.read(new File(sourceImage.getPath()));
                            if (sourceImg == null) {
                                System.err.println("Failed to load image: " + sourceImage.getPath());
                                continue;
                            }
                        } catch (IOException e) {
                            System.err.println("Error loading image " + sourceImage.getPath() + ": " + e.getMessage());
                            continue;
                        }
                        
                        // Process each annotation
                        for (YoloAnnotation annotation : sourceImage.getAnnotations()) {
                            // Check if task was cancelled
                            if (isCancelled()) {
                                return false;
                            }
                            
                            processedObjects++;
                            
                            // Update progress on EDT
                            final int progress = (int) ((double) processedObjects / totalObjects * 100);
                            SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
                            
                            // Calculate crop bounds
                            int imgWidth = sourceImg.getWidth();
                            int imgHeight = sourceImg.getHeight();
                            
                            double centerX = annotation.getXCenter() * imgWidth;
                            double centerY = annotation.getYCenter() * imgHeight;
                            double width = annotation.getWidth() * imgWidth;
                            double height = annotation.getHeight() * imgHeight;
                            
                            int x = Math.max(0, (int) (centerX - width / 2));
                            int y = Math.max(0, (int) (centerY - height / 2));
                            int w = Math.min(imgWidth - x, (int) width);
                            int h = Math.min(imgHeight - y, (int) height);
                            
                            // Skip invalid crops
                            if (w <= 0 || h <= 0) {
                                continue;
                            }
                            
                            // Crop image
                            BufferedImage croppedImg = sourceImg.getSubimage(x, y, w, h);
                            
                            // Generate unique filename
                            String originalName = sourceImage.getFilename().substring(0, sourceImage.getFilename().lastIndexOf('.'));
                            String extension = ".jpg"; // Force JPG for consistency
                            String className = preserveClassesCheckBox.isSelected() && annotation.getClassName() != null ? 
                                annotation.getClassName() : "object";
                            String croppedFilename = String.format("%s_%s_%d%s", 
                                originalName, className.replaceAll("[^a-zA-Z0-9]", "_"), processedObjects, extension);
                            
                            // Save cropped image
                            File croppedImageFile = new File(imagesDir, croppedFilename);
                            try {
                                ImageIO.write(croppedImg, "jpg", croppedImageFile);
                            } catch (IOException e) {
                                System.err.println("Error saving cropped image " + croppedFilename + ": " + e.getMessage());
                                continue;
                            }
                            
                            // Create YoloImage for the cropped image
                            YoloImage croppedYoloImage = new YoloImage();
                            croppedYoloImage.setFilename(croppedFilename);
                            croppedYoloImage.setPath(croppedImageFile.getAbsolutePath());
                            croppedYoloImage.setWidth(w);
                            croppedYoloImage.setHeight(h);
                            
                            // Always add the main annotation (the object that was cropped)
                            YoloAnnotation mainAnnotation = new YoloAnnotation();
                            mainAnnotation.setClassId(annotation.getClassId());
                            mainAnnotation.setClassName(annotation.getClassName());
                            mainAnnotation.setXCenter(0.5); // Center of cropped image
                            mainAnnotation.setYCenter(0.5); // Center of cropped image
                            mainAnnotation.setWidth(1.0);   // Full width of cropped image
                            mainAnnotation.setHeight(1.0);  // Full height of cropped image
                            croppedYoloImage.addAnnotation(mainAnnotation);
                            
                            // Check for other annotations that overlap with this crop area
                            for (YoloAnnotation otherAnnotation : sourceImage.getAnnotations()) {
                                if (otherAnnotation == annotation) continue; // Skip the main annotation
                                
                                // Calculate other annotation's absolute position
                                double otherCenterX = otherAnnotation.getXCenter() * imgWidth;
                                double otherCenterY = otherAnnotation.getYCenter() * imgHeight;
                                double otherWidth = otherAnnotation.getWidth() * imgWidth;
                                double otherHeight = otherAnnotation.getHeight() * imgHeight;
                                
                                double otherLeft = otherCenterX - otherWidth / 2;
                                double otherRight = otherCenterX + otherWidth / 2;
                                double otherTop = otherCenterY - otherHeight / 2;
                                double otherBottom = otherCenterY + otherHeight / 2;
                                
                                // Check if other annotation overlaps with crop area
                                double cropLeft = x;
                                double cropRight = x + w;
                                double cropTop = y;
                                double cropBottom = y + h;
                                
                                if (otherLeft < cropRight && otherRight > cropLeft && 
                                    otherTop < cropBottom && otherBottom > cropTop) {
                                    
                                    // Calculate intersection
                                    double intersectLeft = Math.max(otherLeft, cropLeft);
                                    double intersectRight = Math.min(otherRight, cropRight);
                                    double intersectTop = Math.max(otherTop, cropTop);
                                    double intersectBottom = Math.min(otherBottom, cropBottom);
                                    
                                    // Convert to coordinates relative to cropped image
                                    double relativeLeft = intersectLeft - cropLeft;
                                    double relativeRight = intersectRight - cropLeft;
                                    double relativeTop = intersectTop - cropTop;
                                    double relativeBottom = intersectBottom - cropTop;
                                    
                                    // Convert to YOLO format (normalized)
                                    double relativeCenterX = (relativeLeft + relativeRight) / 2.0 / w;
                                    double relativeCenterY = (relativeTop + relativeBottom) / 2.0 / h;
                                    double relativeWidth = (relativeRight - relativeLeft) / w;
                                    double relativeHeight = (relativeBottom - relativeTop) / h;
                                    
                                    // Only add if the overlapping area is significant (> 10% of the object)
                                    double overlapRatio = (relativeWidth * relativeHeight);
                                    if (overlapRatio > 0.1) {
                                        YoloAnnotation overlappingAnnotation = new YoloAnnotation();
                                        overlappingAnnotation.setClassId(otherAnnotation.getClassId());
                                        overlappingAnnotation.setClassName(otherAnnotation.getClassName());
                                        overlappingAnnotation.setXCenter(relativeCenterX);
                                        overlappingAnnotation.setYCenter(relativeCenterY);
                                        overlappingAnnotation.setWidth(relativeWidth);
                                        overlappingAnnotation.setHeight(relativeHeight);
                                        croppedYoloImage.addAnnotation(overlappingAnnotation);
                                    }
                                }
                            }
                            
                            newProject.addImage(croppedYoloImage);
                            
                            // Save annotation file with all annotations
                            String labelFilename = croppedFilename.substring(0, croppedFilename.lastIndexOf('.')) + ".txt";
                            File labelFile = new File(labelsDir, labelFilename);
                            
                            try (java.io.PrintWriter writer = new java.io.PrintWriter(labelFile)) {
                                for (YoloAnnotation ann : croppedYoloImage.getAnnotations()) {
                                    writer.printf("%d %.6f %.6f %.6f %.6f%n",
                                        ann.getClassId(),
                                        ann.getXCenter(),
                                        ann.getYCenter(),
                                        ann.getWidth(),
                                        ann.getHeight());
                                }
                            }
                        }
                    }
                    
                    publish("Saving project...");
                    
                    // Save project
                    ProjectManager.getInstance().saveProject(newProject);
                    
                    publish("Project created successfully!");
                    return true;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    publish("Error: " + e.getMessage());
                    return false;
                }
            }
            
            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String message = chunks.get(chunks.size() - 1);
                    statusLabel.setText(message);
                    // Force UI update
                    statusLabel.repaint();
                }
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    
                    // Re-enable UI
                    createButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    projectNameField.setEnabled(true);
                    descriptionArea.setEnabled(true);
                    preserveClassesCheckBox.setEnabled(true);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                    
                    if (success) {
                        statusLabel.setText("Project created successfully!");
                        confirmed = true;
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(CreateProjectFromLabelsDialog.this, 
                                "Project created successfully with cropped images!", 
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                            dispose();
                        });
                    } else {
                        statusLabel.setText("Failed to create project");
                        JOptionPane.showMessageDialog(CreateProjectFromLabelsDialog.this, 
                            "Failed to create project. Please check the logs for details.", 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Re-enable UI
                    createButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    projectNameField.setEnabled(true);
                    descriptionArea.setEnabled(true);
                    preserveClassesCheckBox.setEnabled(true);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                    statusLabel.setText("Error creating project");
                    
                    JOptionPane.showMessageDialog(CreateProjectFromLabelsDialog.this, 
                        "Error creating project: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);                }
            }
        };
        
        currentWorker.execute();
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
}
