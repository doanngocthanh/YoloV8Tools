package raven.yolo.training;

import raven.yolo.manager.ProjectManager;
import raven.yolo.model.TrainingConfig;
import raven.yolo.model.YoloProject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Training Manager for YOLO models
 * Handles model training using Python YOLOv8
 */
public class TrainingManager {
    
    private static TrainingManager instance;
    private Process currentTrainingProcess;
    private boolean isTraining = false;
    private final List<TrainingListener> listeners;
    
    private TrainingManager() {
        this.listeners = new ArrayList<>();
    }
    
    public static TrainingManager getInstance() {
        if (instance == null) {
            instance = new TrainingManager();
        }
        return instance;
    }
    
    /**
     * Start training with given configuration
     */
    public CompletableFuture<Boolean> startTraining(TrainingConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                YoloProject currentProject = ProjectManager.getInstance().getCurrentProject();
                if (currentProject == null) {
                    throw new IllegalStateException("No project is currently open");
                }
                
                // Validate dataset before training
                String validationMessage = validateDatasetForTraining(currentProject);
                if (validationMessage != null) {
                    throw new IllegalStateException(validationMessage);
                }
                
                isTraining = true;
                notifyTrainingStarted();
                
                // Prepare training environment
                String trainingDir = prepareTrainingEnvironment(currentProject, config);
                
                // Execute training
                boolean success = executeTraining(trainingDir, config);
                
                isTraining = false;
                if (success) {
                    notifyTrainingCompleted();
                } else {
                    notifyTrainingFailed("Training process failed");
                }
                
                return success;
                
            } catch (Exception e) {
                isTraining = false;
                notifyTrainingFailed(e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Stop current training process
     */
    public void stopTraining() {
        if (currentTrainingProcess != null && currentTrainingProcess.isAlive()) {
            currentTrainingProcess.destroyForcibly();
            currentTrainingProcess = null;
            isTraining = false;
            notifyTrainingStopped();
        }
    }
    
    /**
     * Prepare training environment and dataset
     */
    private String prepareTrainingEnvironment(YoloProject project, TrainingConfig config) throws IOException {
        String projectPath = project.getProjectPath();
        String trainingDir = Paths.get(projectPath, "training").toString();
        
        // Create training directory structure
        File trainingDirFile = new File(trainingDir);
        if (!trainingDirFile.exists()) {
            trainingDirFile.mkdirs();
        }
        
        // Create dataset directories
        String datasetDir = Paths.get(trainingDir, "dataset").toString();
        new File(datasetDir, "images/train").mkdirs();
        new File(datasetDir, "images/val").mkdirs();
        new File(datasetDir, "labels/train").mkdirs();
        new File(datasetDir, "labels/val").mkdirs();
        
        // Copy and split dataset
        prepareDataset(project, datasetDir);
        
        // Create data.yaml file
        createDataYaml(project, datasetDir);
        
        // Create training script
        createTrainingScript(trainingDir, config);
        
        return trainingDir;
    }    /**
     * Prepare dataset for training (split train/val)
     */
    private void prepareDataset(YoloProject project, String datasetDir) throws IOException {
        List<String> validImageFiles = new ArrayList<>();
        
        // Filter for images that have valid annotations
        for (var image : project.getImages()) {
            if (!image.getAnnotations().isEmpty()) {
                // Check if image file exists
                File imageFile = new File(image.getPath());
                if (!imageFile.exists()) {
                    System.err.println("Warning: Image file not found: " + image.getPath());
                    continue;
                }
                
                // Check if has valid annotations (non-zero size)
                boolean hasValidAnnotation = false;
                for (var annotation : image.getAnnotations()) {
                    if (annotation.getWidth() > 0 && annotation.getHeight() > 0) {
                        hasValidAnnotation = true;
                        break;
                    }
                }
                
                if (hasValidAnnotation) {
                    validImageFiles.add(image.getPath());
                }
            }
        }
        
        if (validImageFiles.isEmpty()) {
            throw new IllegalStateException("No valid annotated images found for training");
        }
        
        System.out.println("Found " + validImageFiles.size() + " valid annotated images for training");
        
        // Split 80% train, 20% val with random shuffle
        java.util.Collections.shuffle(validImageFiles, new java.util.Random(42)); // Fixed seed for reproducibility
        int trainSize = (int) (validImageFiles.size() * 0.8);
        
        for (int i = 0; i < validImageFiles.size(); i++) {
            String imagePath = validImageFiles.get(i);
            String originalName = new File(imagePath).getName();
            
            // Generate UUID-based filename to avoid special characters and spaces
            String uuid = java.util.UUID.randomUUID().toString();
            String fileExtension = getFileExtension(originalName);
            String safeName = uuid + "." + fileExtension;
            String labelName = uuid + ".txt";
            
            String destImageDir = i < trainSize ? "images/train" : "images/val";
            String destLabelDir = i < trainSize ? "labels/train" : "labels/val";
            
            // Copy image with safe name
            Path sourceImage = Paths.get(imagePath);
            Path destImage = Paths.get(datasetDir, destImageDir, safeName);
            Files.copy(sourceImage, destImage, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // Create YOLO format label file
            createYoloLabelFile(project, imagePath, Paths.get(datasetDir, destLabelDir, labelName));
            
            System.out.println("Copied: " + originalName + " -> " + safeName);
        }
    }
    
    /**
     * Create YOLO format label file for an image
     */
    private void createYoloLabelFile(YoloProject project, String imagePath, Path labelPath) throws IOException {
        // Find the image in project
        var imageOpt = project.getImages().stream()
            .filter(img -> img.getPath().equals(imagePath))
            .findFirst();
            
        if (imageOpt.isEmpty()) {
            System.err.println("Warning: Image not found in project: " + imagePath);
            return;
        }
        
        var image = imageOpt.get();
        List<String> labelLines = new ArrayList<>();
        
        for (var annotation : image.getAnnotations()) {
            // Skip invalid annotations
            if (annotation.getWidth() <= 0 || annotation.getHeight() <= 0) {
                continue;
            }
            
            // YOLO format: class_id x_center y_center width height (all normalized 0-1)
            String line = String.format("%d %.6f %.6f %.6f %.6f", 
                annotation.getClassId(),
                annotation.getXCenter(),
                annotation.getYCenter(), 
                annotation.getWidth(),
                annotation.getHeight());
            labelLines.add(line);
        }        
        if (!labelLines.isEmpty()) {
            Files.write(labelPath, labelLines, java.nio.charset.StandardCharsets.UTF_8);
        } else {
            System.err.println("Warning: No valid annotations for image: " + imagePath);
        }
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "jpg"; // default extension
    }
    
    /**
     * Create data.yaml file for training
     */
    private void createDataYaml(YoloProject project, String datasetDir) throws IOException {
        StringBuilder yaml = new StringBuilder();
        yaml.append("path: ").append(datasetDir).append("\n");
        yaml.append("train: images/train\n");
        yaml.append("val: images/val\n");
        yaml.append("\n");
        yaml.append("nc: ").append(project.getClasses().size()).append("\n");
        yaml.append("names:\n");
        
        for (int i = 0; i < project.getClasses().size(); i++) {
            yaml.append("  ").append(i).append(": ").append(project.getClasses().get(i)).append("\n");
        }
        
        Files.write(Paths.get(datasetDir, "data.yaml"), yaml.toString().getBytes());
    }
      /**
     * Create Python training script
     */
    private void createTrainingScript(String trainingDir, TrainingConfig config) throws IOException {
        // Get optimal device for training based on CUDA availability
        PythonSetupManager pythonSetup = PythonSetupManager.getInstance();
        String optimalDevice = pythonSetup.getOptimalDevice(config.getDevice());
        
        StringBuilder script = new StringBuilder();        script.append("#!/usr/bin/env python3\n");
        script.append("import os\n");
        script.append("import sys\n");
        script.append("import traceback\n");
        script.append("from ultralytics import YOLO\n\n");
        
        script.append("def main():\n");
        script.append("    try:\n");
        script.append("        print('Starting YOLO training...')\n");
        script.append("        \n");
        script.append("        # Load model\n");
        script.append("        print(f'Loading model: ").append(config.getModelVariant()).append("')\n");
        script.append("        model = YOLO('").append(config.getModelVariant()).append("')\n\n");
        
        script.append("        # Training parameters\n");
        script.append("        print('Training parameters:')\n");
        script.append("        print(f'  Data: dataset/data.yaml')\n");
        script.append("        print(f'  Epochs: ").append(config.getEpochs()).append("')\n");
        script.append("        print(f'  Image size: ").append(config.getImageSize()).append("')\n");
        script.append("        print(f'  Batch size: ").append(config.getBatchSize()).append("')\n");
        script.append("        print(f'  Requested device: ").append(config.getDevice()).append("')\n");
        script.append("        print(f'  Optimal device: ").append(optimalDevice).append("')\n");
        script.append("        print('')\n\n");
        
        script.append("        # Start training\n");
        script.append("        results = model.train(\n");
        script.append("        data='dataset/data.yaml',\n");
        script.append("        epochs=").append(config.getEpochs()).append(",\n");
        script.append("        imgsz=").append(config.getImageSize()).append(",\n");
        script.append("        batch=").append(config.getBatchSize()).append(",\n");
        script.append("        lr0=").append(config.getLearningRate()).append(",\n");        script.append("        device='").append(optimalDevice).append("',\n");
        script.append("        workers=").append(config.getWorkers()).append(",\n");
        script.append("        patience=").append(config.getPatience()).append(",\n");        script.append("        cache=").append(toPythonBoolean(config.isCache())).append(",\n");
        script.append("        augment=").append(toPythonBoolean(config.isAugment())).append(",\n");
        script.append("        mosaic=").append(config.getMosaic()).append(",\n");
        script.append("        mixup=").append(config.getMixup()).append(",\n");        script.append("        copy_paste=").append(config.getCopyPaste()).append(",\n");
        script.append("        project='runs/train',\n");
        script.append("        name='yolo_model',\n");        script.append("        exist_ok=").append(toPythonBoolean(true)).append(",\n");
        script.append("        verbose=").append(toPythonBoolean(true)).append("\n");        script.append("        )\n\n");
        
        script.append("        print('\\nTraining completed successfully!')\n");
        script.append("        print(f'Best model saved at: {results.save_dir}/weights/best.pt')\n");
        script.append("        print(f'Last model saved at: {results.save_dir}/weights/last.pt')\n");
        script.append("        return 0\n\n");
        
        script.append("    except Exception as e:\n");
        script.append("        print(f'\\nERROR: Training failed with exception: {str(e)}')\n");
        script.append("        print('\\nFull traceback:')\n");
        script.append("        traceback.print_exc()\n");
        script.append("        return 1\n\n");
        
        script.append("if __name__ == '__main__':\n");
        script.append("    exit_code = main()\n");
        script.append("    sys.exit(exit_code)\n");
        
        Files.write(Paths.get(trainingDir, "train.py"), script.toString().getBytes());
    }
    
    /**
     * Convert Java boolean to Python boolean string
     */
    private String toPythonBoolean(boolean value) {
        return value ? "True" : "False";
    }
    
    /**
     * Execute training process
     */    private boolean executeTraining(String trainingDir, TrainingConfig config) {
        try {
            // Check Python environment for current project
            PythonSetupManager pythonManager = PythonSetupManager.getInstance();
            
            // Use project-specific Python command (virtual environment if available)
            String pythonCommand = pythonManager.getProjectPythonCommand();
            if (pythonCommand == null) {
                throw new RuntimeException("Python not found. Please setup Python environment first.");
            }
            
            // Check if ultralytics is installed in project environment
            if (!pythonManager.isUltralyticsInstalledInProject()) {
                throw new RuntimeException("Ultralytics not installed in current project environment. Please setup project environment.");
            }
            
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(trainingDir));
            
            pb.command(pythonCommand, "train.py");
            
            // Set environment variables
            pb.environment().put("PYTHONPATH", System.getProperty("user.dir"));
            
            currentTrainingProcess = pb.start();
            
            // Monitor training output
            monitorTrainingOutput(currentTrainingProcess);
            
            int exitCode = currentTrainingProcess.waitFor();
            return exitCode == 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }    }
    
    /**
     * Monitor training output
     */
    private void monitorTrainingOutput(Process process) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    notifyTrainingProgress(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    notifyTrainingError(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    public boolean isTraining() {
        return isTraining;
    }
    
    // Listener pattern for training events
    public interface TrainingListener {
        default void onTrainingStarted() {}
        default void onTrainingProgress(String message) {}
        default void onTrainingCompleted() {}
        default void onTrainingFailed(String error) {}
        default void onTrainingStopped() {}
        default void onTrainingError(String error) {}
    }
    
    public void addTrainingListener(TrainingListener listener) {
        listeners.add(listener);
    }
    
    public void removeTrainingListener(TrainingListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyTrainingStarted() {
        listeners.forEach(TrainingListener::onTrainingStarted);
    }
    
    private void notifyTrainingProgress(String message) {
        listeners.forEach(l -> l.onTrainingProgress(message));
    }
    
    private void notifyTrainingCompleted() {
        listeners.forEach(TrainingListener::onTrainingCompleted);
    }
    
    private void notifyTrainingFailed(String error) {
        listeners.forEach(l -> l.onTrainingFailed(error));
    }
    
    private void notifyTrainingStopped() {
        listeners.forEach(TrainingListener::onTrainingStopped);
    }
    
    private void notifyTrainingError(String error) {
        listeners.forEach(l -> l.onTrainingError(error));
    }
    
    /**
     * Validate dataset before training
     * @return null if valid, error message if invalid
     */
    private String validateDatasetForTraining(YoloProject project) {
        // Check if project has any classes
        if (project.getClasses().isEmpty()) {
            return "No classes defined for training. Please add at least one class in the annotation tool.";
        }
        
        // Check if project has any images
        if (project.getImages().isEmpty()) {
            return "No images found in project. Please add images to the project.";
        }
        
        // Count valid annotated images
        int validImageCount = 0;
        int totalAnnotationCount = 0;
        
        for (var image : project.getImages()) {
            if (!image.getAnnotations().isEmpty()) {
                // Check if image file exists
                File imageFile = new File(image.getPath());
                if (!imageFile.exists()) {
                    continue; // Skip missing images
                }
                  // Count valid annotations (not zero-sized bounding boxes)
                for (var annotation : image.getAnnotations()) {
                    if (annotation.getWidth() > 0 && annotation.getHeight() > 0) {
                        totalAnnotationCount++;
                    }
                }
                
                if (!image.getAnnotations().isEmpty()) {
                    validImageCount++;
                }
            }
        }
        
        // Check minimum requirements
        if (validImageCount == 0) {
            return "No annotated images found. Please annotate at least some images before training.";
        }
        
        if (totalAnnotationCount == 0) {
            return "No valid annotations found. Please make sure your bounding boxes are properly drawn.";
        }
        
        if (validImageCount < 2) {
            return "At least 2 annotated images are recommended for training. You have " + validImageCount + " annotated image(s).";
        }
        
        System.out.println("Dataset validation passed:");
        System.out.println("  - " + validImageCount + " annotated images");
        System.out.println("  - " + totalAnnotationCount + " total annotations");
        System.out.println("  - " + project.getClasses().size() + " classes");
        
        return null; // Valid
    }
}
