package raven.yolo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.yolo.manager.ProjectManager;
import raven.yolo.manager.WorkspaceManager;
import raven.yolo.model.YoloProject;
import raven.yolo.model.YoloImage;
import raven.yolo.model.YoloAnnotation;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;

public class ImportDatasetDialog extends JDialog {
      public interface ImportCompleteListener {
        void onImportComplete();
    }
      public interface ProgressCallback {
        void publishProgress(String message);
    }
    
    // Console logger for debugging
    private static void logProgress(String message) {
        System.out.println("[IMPORT] " + message);
    }
    
    private JTextField datasetPathField;
    private JTextField projectNameField;
    private JTextArea projectDescriptionArea;
    private JTextArea previewArea;
    private JButton browseButton;
    private JButton importButton;
    private JButton cancelButton;
    
    private File selectedDatasetPath;
    private Map<String, String> detectedClasses;
    private int detectedImageCount;
    private int detectedAnnotationCount;
    private ImportCompleteListener importCompleteListener;
    
    public ImportDatasetDialog(Frame parent) {
        super(parent, "Import YOLO Dataset", true);
        initComponents();
        setupLayout();
        setupEventHandlers();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(parent);
    }
    
    public void setImportCompleteListener(ImportCompleteListener listener) {
        this.importCompleteListener = listener;
    }
    
    private void initComponents() {
        datasetPathField = new JTextField();
        datasetPathField.setEditable(false);
        datasetPathField.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        projectNameField = new JTextField();
        projectNameField.putClientProperty(FlatClientProperties.STYLE, "arc:5");
          projectDescriptionArea = new JTextArea(3, 30);
        projectDescriptionArea.setLineWrap(true);
        projectDescriptionArea.setWrapStyleWord(true);
        
        previewArea = new JTextArea(10, 30);
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        browseButton = new JButton("Browse...");
        browseButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        importButton = new JButton("Import Dataset");
        importButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        importButton.setEnabled(false);
        
        cancelButton = new JButton("Cancel");
        cancelButton.putClientProperty(FlatClientProperties.STYLE, "arc:5");
        
        detectedClasses = new HashMap<>();
    }
    
    private void setupLayout() {
        setLayout(new MigLayout("insets 20", "[grow,fill]", "[][][][grow,fill][]"));
        
        // Dataset path selection
        JPanel pathPanel = new JPanel(new MigLayout("insets 0", "[grow,fill][]", "[]"));
        pathPanel.setBorder(BorderFactory.createTitledBorder("Dataset Path"));
        pathPanel.add(datasetPathField);
        pathPanel.add(browseButton);
        add(pathPanel, "wrap, gapbottom 10");
        
        // Project info
        JPanel projectPanel = new JPanel(new MigLayout("insets 10", "[grow,fill]", "[]5[]5[]"));
        projectPanel.setBorder(BorderFactory.createTitledBorder("Project Information"));
        
        projectPanel.add(new JLabel("Project Name:"), "wrap");
        projectPanel.add(projectNameField, "wrap");
        projectPanel.add(new JLabel("Description:"), "wrap");
        projectPanel.add(new JScrollPane(projectDescriptionArea), "wrap");
        
        add(projectPanel, "wrap, gapbottom 10");
        
        // Preview
        JPanel previewPanel = new JPanel(new MigLayout("insets 10", "[grow,fill]", "[grow,fill]"));
        previewPanel.setBorder(BorderFactory.createTitledBorder("Dataset Preview"));
        previewPanel.add(new JScrollPane(previewArea));
        add(previewPanel, "wrap, gapbottom 10");
        
        // Buttons
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[grow][]20[]", "[]"));
        buttonPanel.add(new JLabel(), "growx");
        buttonPanel.add(importButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel);
    }
    
    private void setupEventHandlers() {
        browseButton.addActionListener(e -> browseDatasetPath());
        importButton.addActionListener(e -> importDataset());
        cancelButton.addActionListener(e -> dispose());
    }
    
    private void browseDatasetPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select YOLO Dataset Folder");
        
        // Set default directory to user's Downloads
        String userHome = System.getProperty("user.home");
        File downloadsDir = new File(userHome, "Downloads");
        if (downloadsDir.exists()) {
            chooser.setCurrentDirectory(downloadsDir);
        }
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedDatasetPath = chooser.getSelectedFile();
            datasetPathField.setText(selectedDatasetPath.getAbsolutePath());
            
            // Auto-generate project name from folder name
            String folderName = selectedDatasetPath.getName();
            projectNameField.setText(folderName);
            projectDescriptionArea.setText("Imported from dataset: " + folderName);
            
            // Analyze dataset
            analyzeDataset();
        }
    }
    
    private void analyzeDataset() {
        if (selectedDatasetPath == null || !selectedDatasetPath.exists()) {
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            previewArea.setText("Analyzing dataset...");
            importButton.setEnabled(false);
        });
        
        // Run analysis in background thread
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {                    detectedClasses.clear();
                    detectedImageCount = 0;
                    detectedAnnotationCount = 0;
                    
                    publish("Scanning dataset structure...\n");
                    
                    // Look for common YOLO dataset structures
                    File[] subDirs = selectedDatasetPath.listFiles(File::isDirectory);
                    File trainDir = null, validDir = null, testDir = null;
                    
                    if (subDirs != null) {
                        for (File dir : subDirs) {
                            String dirName = dir.getName().toLowerCase();
                            if (dirName.equals("train") || dirName.equals("training")) {
                                trainDir = dir;
                            } else if (dirName.equals("valid") || dirName.equals("validation") || dirName.equals("val")) {
                                validDir = dir;
                            } else if (dirName.equals("test")) {
                                testDir = dir;
                            }
                        }
                    }
                    
                    // If no standard structure, use the root directory
                    if (trainDir == null && validDir == null && testDir == null) {
                        trainDir = selectedDatasetPath;
                        publish("Using root directory as training data\n");
                    }
                    
                    // Analyze each directory
                    if (trainDir != null) {
                        analyzeDirectory(trainDir, "train");
                        publish("Found training data in: " + trainDir.getName() + "\n");
                    }
                    if (validDir != null) {
                        analyzeDirectory(validDir, "valid");
                        publish("Found validation data in: " + validDir.getName() + "\n");
                    }
                    if (testDir != null) {
                        analyzeDirectory(testDir, "test");
                        publish("Found test data in: " + testDir.getName() + "\n");
                    }
                      // Look for classes.txt or data.yaml
                    publish("Looking for class definition files...\n");
                    File classesFile = new File(selectedDatasetPath, "classes.txt");
                    File dataYaml = new File(selectedDatasetPath, "data.yaml");
                    
                    publish("Checking for classes.txt: " + classesFile.exists() + "\n");
                    publish("Checking for data.yaml: " + dataYaml.exists() + "\n");
                    
                    if (classesFile.exists()) {
                        parseClassesFile(classesFile);
                        publish("Found and parsed classes.txt file\n");
                    } else if (dataYaml.exists()) {
                        parseDataYaml(dataYaml);
                        publish("Found and parsed data.yaml file\n");
                    }
                      publish("\n=== Dataset Summary ===\n");
                    publish("Images found: " + detectedImageCount + "\n");
                    publish("Annotations found: " + detectedAnnotationCount + "\n");
                    publish("Classes found: " + detectedClasses.size() + "\n");
                    
                    if (!detectedClasses.isEmpty()) {
                        publish("\nClasses detected:\n");
                        for (Map.Entry<String, String> entry : detectedClasses.entrySet()) {
                            publish("  ID " + entry.getKey() + ": " + entry.getValue() + "\n");
                        }
                    } else {
                        publish("\nWARNING: No classes detected! Please check your data.yaml or classes.txt file.\n");
                    }
                    
                    if (detectedImageCount > 0) {
                        publish("\nDataset is ready for import!\n");
                    } else {
                        publish("\nNo images found in dataset. Please check the dataset structure.\n");
                    }
                    
                } catch (Exception e) {
                    publish("Error analyzing dataset: " + e.getMessage() + "\n");
                }
                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String chunk : chunks) {
                    previewArea.append(chunk);
                }
                previewArea.setCaretPosition(previewArea.getDocument().getLength());
            }
            
            @Override
            protected void done() {
                importButton.setEnabled(detectedImageCount > 0);
            }
        };
        
        worker.execute();
    }
    
    private void analyzeDirectory(File dir, String type) throws IOException {
        File imagesDir = new File(dir, "images");
        File labelsDir = new File(dir, "labels");
        
        // If no images/labels subdirs, use the directory itself
        if (!imagesDir.exists()) {
            imagesDir = dir;
        }
        if (!labelsDir.exists()) {
            labelsDir = dir;
        }
        
        // Count images
        File[] imageFiles = imagesDir.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
                   lower.endsWith(".png") || lower.endsWith(".bmp");
        });
        
        if (imageFiles != null) {
            detectedImageCount += imageFiles.length;
        }
        
        // Count annotations
        File[] labelFiles = labelsDir.listFiles((d, name) -> name.toLowerCase().endsWith(".txt"));
        if (labelFiles != null) {
            detectedAnnotationCount += labelFiles.length;
            
            // Parse some label files to detect classes
            int sampled = 0;
            for (File labelFile : labelFiles) {
                if (sampled >= 10) break; // Sample first 10 files
                parseLabelFile(labelFile);
                sampled++;
            }
        }
    }
    
    private void parseLabelFile(File labelFile) throws IOException {
        List<String> lines = Files.readAllLines(labelFile.toPath());
        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 5) {
                String classId = parts[0];
                if (!detectedClasses.containsKey(classId)) {
                    detectedClasses.put(classId, "class_" + classId);
                }
            }
        }
    }
    
    private void parseClassesFile(File classesFile) throws IOException {
        List<String> lines = Files.readAllLines(classesFile.toPath());
        detectedClasses.clear();
        for (int i = 0; i < lines.size(); i++) {
            String className = lines.get(i).trim();
            if (!className.isEmpty()) {
                detectedClasses.put(String.valueOf(i), className);
            }
        }
    }
      private void parseDataYaml(File dataYaml) throws IOException {
        logProgress("Parsing data.yaml file: " + dataYaml.getAbsolutePath());
        List<String> lines = Files.readAllLines(dataYaml.toPath());
        boolean inNames = false;
        detectedClasses.clear();
        
        for (String line : lines) {
            line = line.trim();
            logProgress("Processing line: " + line);
            
            // Handle format: names: ['class1', 'class2', ...]
            if (line.startsWith("names:")) {
                String namesValue = line.substring(6).trim(); // Remove "names:"
                logProgress("Found names line: " + namesValue);
                
                if (namesValue.startsWith("[") && namesValue.endsWith("]")) {
                    // Parse array format: ['MRZ', 'class2', ...]
                    String arrayContent = namesValue.substring(1, namesValue.length() - 1); // Remove [ ]
                    String[] classNames = arrayContent.split(",");
                    
                    for (int i = 0; i < classNames.length; i++) {
                        String className = classNames[i].trim();
                        // Remove quotes if present
                        if ((className.startsWith("'") && className.endsWith("'")) ||
                            (className.startsWith("\"") && className.endsWith("\""))) {
                            className = className.substring(1, className.length() - 1);
                        }
                        if (!className.isEmpty()) {
                            detectedClasses.put(String.valueOf(i), className);
                            logProgress("Added class " + i + ": " + className);
                        }
                    }
                } else if (namesValue.isEmpty()) {
                    // Handle format:
                    // names:
                    //   - class1
                    //   - class2
                    inNames = true;
                    logProgress("Starting multiline names parsing");
                }
                continue;
            }
            
            // Handle multiline format
            if (inNames && line.startsWith("-")) {
                String className = line.substring(1).trim();
                if (className.startsWith("'") || className.startsWith("\"")) {
                    className = className.substring(1, className.length() - 1);
                }
                if (!className.isEmpty()) {
                    detectedClasses.put(String.valueOf(detectedClasses.size()), className);
                    logProgress("Added class " + (detectedClasses.size() - 1) + ": " + className);
                }
            } else if (inNames && !line.isEmpty() && !line.startsWith(" ") && !line.startsWith("-")) {
                // End of names section
                logProgress("End of names section");
                break;
            }
        }
        
        logProgress("Parsed " + detectedClasses.size() + " classes from data.yaml");
        for (Map.Entry<String, String> entry : detectedClasses.entrySet()) {
            logProgress("Class " + entry.getKey() + ": " + entry.getValue());
        }
    }
      private void importDataset() {
        logProgress("Starting import dataset process...");
        
        if (selectedDatasetPath == null || projectNameField.getText().trim().isEmpty()) {
            logProgress("Missing dataset path or project name");
            JOptionPane.showMessageDialog(this, 
                "Please select a dataset and enter a project name.", 
                "Missing Information", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String projectName = projectNameField.getText().trim();
        String description = projectDescriptionArea.getText().trim();
        
        logProgress("Project name: " + projectName);
        logProgress("Dataset path: " + selectedDatasetPath.getAbsolutePath());
        
        // Check if project name already exists
        String workspacePath = WorkspaceManager.getInstance().getWorkspacePath();
        logProgress("Workspace path: " + workspacePath);
        
        File projectDir = new File(workspacePath, projectName);
        if (projectDir.exists()) {
            logProgress("Project directory already exists, asking user...");
            int result = JOptionPane.showConfirmDialog(this,
                "A project with this name already exists. Do you want to overwrite it?",
                "Project Exists",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION) {
                logProgress("User cancelled overwrite");
                return;
            }
        }
          logProgress("Creating progress dialog...");
        // Create progress dialog but don't show it yet
        JProgressDialog progressDialog = new JProgressDialog(this, "Importing Dataset");
        
        logProgress("Starting SwingWorker...");
        
        // Import in background thread
        SwingWorker<Boolean, String> worker = new SwingWorker<Boolean, String>() {            @Override
            protected Boolean doInBackground() throws Exception {                logProgress("SwingWorker.doInBackground() started");
                try {
                    logProgress("About to call Thread.sleep() to test threading...");
                    Thread.sleep(500); // Test if worker is running
                    logProgress("Thread.sleep() completed successfully");
                    
                    logProgress("About to publish 'Creating project...'");
                    publish("Creating project...");
                    logProgress("Creating project: " + projectName);
                      // Create project
                    logProgress("Calling ProjectManager.createProject()...");
                    YoloProject project = ProjectManager.getInstance().createProject(projectName, description, projectDir.getAbsolutePath());
                    logProgress("Project created successfully: " + project.getName());
                      // Add classes
                    publish("Adding classes...");
                    logProgress("Adding " + detectedClasses.size() + " classes");
                    for (Map.Entry<String, String> entry : detectedClasses.entrySet()) {
                        logProgress("Adding class: " + entry.getValue());
                        project.addClass(entry.getValue());
                    }
                    logProgress("All classes added successfully");                      // Import images and annotations
                    publish("Importing images and annotations...");
                    logProgress("Starting import of images and annotations");
                    logProgress("Dataset root: " + selectedDatasetPath.getAbsolutePath());
                    importDatasetFiles(project, selectedDatasetPath, msg -> {
                        publish(msg);
                        logProgress(msg);
                    });
                    logProgress("Import files completed");
                    
                    // Save project
                    publish("Saving project...");
                    logProgress("Saving project to disk");
                    ProjectManager.getInstance().saveProject(project);
                    ProjectManager.getInstance().setCurrentProject(project);
                    
                    publish("Import completed successfully!");
                    logProgress("Import completed successfully!");
                    return true;
                      } catch (Exception e) {
                    logProgress("ERROR in doInBackground: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    e.printStackTrace();
                    publish("Error: " + e.getMessage());
                    return false;
                }
            }
              @Override
            protected void process(List<String> chunks) {
                logProgress("SwingWorker.process() called with " + chunks.size() + " chunks");
                for (String chunk : chunks) {
                    progressDialog.updateStatus(chunk);
                    logProgress("UI update: " + chunk);
                }
            }
            
            @Override
            protected void done() {
                logProgress("SwingWorker.done() called");
                progressDialog.dispose();
                try {
                    boolean success = get();                    if (success) {
                        JOptionPane.showMessageDialog(ImportDatasetDialog.this,
                            "Dataset imported successfully!\nProject '" + projectName + "' is now ready for use.",
                            "Import Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        // Notify listener
                        if (importCompleteListener != null) {
                            importCompleteListener.onImportComplete();
                        }
                        
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(ImportDatasetDialog.this,
                            "Failed to import dataset. Please check the error messages.",
                            "Import Failed",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ImportDatasetDialog.this,
                        "Failed to import dataset: " + e.getMessage(),
                        "Import Failed",
                        JOptionPane.ERROR_MESSAGE);                }
            }
        };
          logProgress("About to execute SwingWorker...");
        worker.execute();
        logProgress("SwingWorker.execute() called successfully");
        
        // Show progress dialog after worker starts
        logProgress("Now showing progress dialog...");
        progressDialog.setVisible(true);
        logProgress("Progress dialog shown");
    }
      private void importDatasetFiles(YoloProject project, File datasetRoot, ProgressCallback progress) throws IOException {
        logProgress("importDatasetFiles() started");
        logProgress("Dataset root: " + datasetRoot.getAbsolutePath());
        logProgress("Project path: " + project.getProjectPath());
        
        // Find all directories that might contain images
        List<File> imageDirs = new ArrayList<>();
        
        logProgress("Looking for standard YOLO structure...");
        // Check for standard YOLO structure
        File trainDir = new File(datasetRoot, "train/images");
        File validDir = new File(datasetRoot, "valid/images");
        File testDir = new File(datasetRoot, "test/images");
        
        if (trainDir.exists()) {
            imageDirs.add(trainDir);
            logProgress("Found train/images directory");
        }
        if (validDir.exists()) {
            imageDirs.add(validDir);
            logProgress("Found valid/images directory");
        }
        if (testDir.exists()) {
            imageDirs.add(testDir);
            logProgress("Found test/images directory");
        }
          // If no standard structure, check for images in train/valid/test folders directly
        if (imageDirs.isEmpty()) {
            logProgress("No standard structure found, checking alternative...");
            trainDir = new File(datasetRoot, "train");
            validDir = new File(datasetRoot, "valid");
            testDir = new File(datasetRoot, "test");
            
            if (trainDir.exists()) {
                imageDirs.add(trainDir);
                logProgress("Found train directory");
            }
            if (validDir.exists()) {
                imageDirs.add(validDir);
                logProgress("Found valid directory");
            }
            if (testDir.exists()) {
                imageDirs.add(testDir);
                logProgress("Found test directory");
            }
        }
        
        // If still no structure, use root directory
        if (imageDirs.isEmpty()) {
            logProgress("No structured directories found, using root directory");
            imageDirs.add(datasetRoot);
        }
        
        logProgress("Total directories to process: " + imageDirs.size());// Process each directory
        for (File imageDir : imageDirs) {            progress.publishProgress("Processing directory: " + imageDir.getName());
            logProgress("Processing directory: " + imageDir.getName());
            importFromDirectory(project, imageDir, datasetRoot, progress);
        }
    }
      private void importFromDirectory(YoloProject project, File imageDir, File datasetRoot, ProgressCallback progress) throws IOException {
        logProgress("importFromDirectory() started for: " + imageDir.getAbsolutePath());
        
        // Find corresponding labels directory
        File labelsDir = new File(imageDir.getParent(), "labels");
        if (!labelsDir.exists()) {
            labelsDir = imageDir; // Same directory
            logProgress("Labels directory same as images: " + labelsDir.getAbsolutePath());
        } else {
            logProgress("Labels directory: " + labelsDir.getAbsolutePath());
        }
          // Get all image files
        logProgress("Scanning for image files in: " + imageDir.getAbsolutePath());
        File[] imageFiles = imageDir.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
                   lower.endsWith(".png") || lower.endsWith(".bmp");
        });
          if (imageFiles == null) {
            logProgress("No files found in directory (null result)");
            return;
        }
        
        logProgress("Scan completed. Found " + imageFiles.length + " image files");
        progress.publishProgress("Found " + imageFiles.length + " images in " + imageDir.getName());
        logProgress("Found " + imageFiles.length + " images in " + imageDir.getName());
          String projectImagesPath = project.getProjectPath() + File.separator + "images";
        File projectImagesDir = new File(projectImagesPath);
        projectImagesDir.mkdirs();
        logProgress("Project images directory: " + projectImagesPath);
        
        logProgress("Starting to process " + imageFiles.length + " files...");
          int processed = 0;
        for (File imageFile : imageFiles) {
            processed++;
            
            if (processed == 1) {
                logProgress("Processing first file: " + imageFile.getName());
            }// Only update progress every 20 files or at the end to reduce overhead
            if (processed % 20 == 0 || processed == imageFiles.length) {
                String progressMsg = "Processing image " + processed + "/" + imageFiles.length + " in " + imageDir.getName();
                progress.publishProgress(progressMsg);
                logProgress(progressMsg);
            }
            
            // Generate UUID for filename to avoid special characters
            String uuid = UUID.randomUUID().toString();
            String extension = getFileExtension(imageFile.getName());
            String newImageName = uuid + "." + extension;            // Copy image to project
            File targetImageFile = new File(projectImagesDir, newImageName);
            Files.copy(imageFile.toPath(), targetImageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // Use default dimensions for faster import - can be updated later if needed
            int imageWidth = 640;  // Default width
            int imageHeight = 480; // Default height
            
            // Skip reading actual image dimensions for faster import
            // TODO: Add option to read actual dimensions in background thread later
            
            // Create YoloImage
            YoloImage yoloImage = new YoloImage(newImageName, targetImageFile.getAbsolutePath(), imageWidth, imageHeight);
            
            // Look for corresponding label file
            String baseName = getFileNameWithoutExtension(imageFile.getName());
            File labelFile = new File(labelsDir, baseName + ".txt");
              if (labelFile.exists()) {
                // Parse annotations - optimized for speed
                try {
                    List<String> lines = Files.readAllLines(labelFile.toPath());
                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 5) {
                            try {
                                int classId = Integer.parseInt(parts[0]);
                                double x = Double.parseDouble(parts[1]);
                                double y = Double.parseDouble(parts[2]);
                                double width = Double.parseDouble(parts[3]);
                                double height = Double.parseDouble(parts[4]);
                                
                                // Get class name
                                String className = detectedClasses.get(String.valueOf(classId));
                                if (className == null) {
                                    className = "class_" + classId;
                                }
                                
                                // Create annotation
                                YoloAnnotation annotation = new YoloAnnotation(classId, className, x, y, width, height);
                                yoloImage.addAnnotation(annotation);
                                
                            } catch (NumberFormatException e) {
                                // Skip invalid lines silently for faster processing
                            }
                        }
                    }
                } catch (IOException e) {
                    // Skip files that can't be read
                    System.err.println("Failed to read annotation file: " + labelFile.getName());
                }
            }
            
            // Add image to project
            project.addImage(yoloImage);
        }
    }
    
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }
    
    private String getFileNameWithoutExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }
      // Simple progress dialog with throttled updates
    private static class JProgressDialog extends JDialog {
        private JLabel statusLabel;
        private long lastUpdateTime = 0;
        private static final long UPDATE_THROTTLE_MS = 100; // Update at most every 100ms
        
        public JProgressDialog(Dialog parent, String title) {
            super(parent, title, true);
            initComponents();
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            setSize(400, 120);
            setLocationRelativeTo(parent);
        }
        
        private void initComponents() {
            setLayout(new MigLayout("insets 20", "[grow,center]", "[]10[]"));
            
            statusLabel = new JLabel("Please wait...");
            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(statusLabel, "wrap");
            
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            add(progressBar, "growx");
        }
        
        public void updateStatus(String status) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime >= UPDATE_THROTTLE_MS) {
                SwingUtilities.invokeLater(() -> statusLabel.setText(status));
                lastUpdateTime = currentTime;
            }
        }
    }
}
