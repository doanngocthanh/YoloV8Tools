package raven.yolo.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import raven.yolo.model.YoloProject;
import raven.yolo.model.YoloImage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ProjectManager {
    
    private static ProjectManager instance;
    private final ObjectMapper objectMapper;
    private YoloProject currentProject;
    private final List<ProjectListener> listeners;
    
    private ProjectManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.listeners = new ArrayList<>();
    }
    
    public static ProjectManager getInstance() {
        if (instance == null) {
            instance = new ProjectManager();
        }
        return instance;
    }
      public YoloProject createProject(String name, String description, String projectPath) throws IOException {
        // Use workspace manager to create project directory
        WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
        
        File projectDir = new File(projectPath);
        if (!projectDir.exists()) {
            projectDir.mkdirs();
        }
        
        // Create subdirectories
        new File(projectDir, "images").mkdirs();
        new File(projectDir, "labels").mkdirs();
        new File(projectDir, "exports").mkdirs();
        
        YoloProject project = new YoloProject(name, description, projectPath);
        saveProject(project);
        
        // Add to recent projects
        workspaceManager.addRecentProject(name, projectPath);
        
        this.currentProject = project;
        notifyProjectChanged();
        
        return project;
    }
      public YoloProject loadProject(String projectPath) throws IOException {
        File projectFile = new File(projectPath, "project.json");
        if (!projectFile.exists()) {
            throw new FileNotFoundException("Project file not found: " + projectFile.getAbsolutePath());
        }
        
        YoloProject project = objectMapper.readValue(projectFile, YoloProject.class);
        project.setProjectPath(projectPath);
        
        // Add to recent projects
        WorkspaceManager.getInstance().addRecentProject(project.getName(), projectPath);
        
        this.currentProject = project;
        notifyProjectChanged();
        
        return project;
    }
    
    public void saveProject(YoloProject project) throws IOException {
        if (project == null) return;
        
        File projectFile = project.getProjectFile();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(projectFile, project);
    }
      public void saveCurrentProject() throws IOException {
        if (currentProject != null) {
            saveProject(currentProject);
            // Also save all image annotations to label files
            saveAllAnnotationsToFiles();
        }
    }
    
    public void saveImageAnnotations(YoloImage image) throws IOException {
        if (currentProject == null || image == null) return;
        
        // Create label file
        File labelFile = new File(currentProject.getLabelsDir(), image.getFilenameWithoutExtension() + ".txt");
        
        // Ensure labels directory exists
        if (!currentProject.getLabelsDir().exists()) {
            currentProject.getLabelsDir().mkdirs();
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(labelFile))) {
            for (var annotation : image.getAnnotations()) {
                writer.println(annotation.toYoloFormat());
            }
        }
    }
    
    private void saveAllAnnotationsToFiles() throws IOException {
        if (currentProject == null) return;
        
        for (YoloImage image : currentProject.getImages()) {
            saveImageAnnotations(image);
        }
    }
    
    public YoloProject getCurrentProject() {
        return currentProject;
    }
    
    public void setCurrentProject(YoloProject project) {
        this.currentProject = project;
        notifyProjectChanged();
    }
    
    public void addImageToProject(File imageFile) throws IOException {
        if (currentProject == null) {
            throw new IllegalStateException("No project is currently open");
        }
        
        // Copy image to project images directory
        File imagesDir = currentProject.getImagesDir();
        File targetFile = new File(imagesDir, imageFile.getName());
        Files.copy(imageFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        // Read image dimensions (simplified - you might want to use BufferedImage for real dimensions)
        // For now, we'll set default dimensions
        YoloImage yoloImage = new YoloImage(imageFile.getName(), targetFile.getAbsolutePath(), 640, 480);
        
        currentProject.addImage(yoloImage);
        saveCurrentProject();
        notifyProjectChanged();
    }
    
    public void removeImageFromProject(YoloImage image) throws IOException {
        if (currentProject == null) return;
        
        // Remove image file
        File imageFile = new File(image.getPath());
        if (imageFile.exists()) {
            imageFile.delete();
        }
        
        // Remove label file
        File labelFile = new File(currentProject.getLabelsDir(), image.getFilenameWithoutExtension() + ".txt");
        if (labelFile.exists()) {
            labelFile.delete();
        }
        
        currentProject.removeImage(image);
        saveCurrentProject();
        notifyProjectChanged();
    }
    
    public void addClass(String className) throws IOException {
        if (currentProject == null) return;
        
        currentProject.addClass(className);
        saveCurrentProject();
        notifyProjectChanged();
    }
    
    public void removeClass(String className) throws IOException {
        if (currentProject == null) return;
        
        currentProject.removeClass(className);
        saveCurrentProject();
        notifyProjectChanged();
    }
    
    public void exportDataset(String exportPath) throws IOException {
        if (currentProject == null) {
            throw new IllegalStateException("No project is currently open");
        }
        
        File exportDir = new File(exportPath);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        
        // Create train/val directories
        File trainDir = new File(exportDir, "train");
        File valDir = new File(exportDir, "val");
        File trainImagesDir = new File(trainDir, "images");
        File trainLabelsDir = new File(trainDir, "labels");
        File valImagesDir = new File(valDir, "images");
        File valLabelsDir = new File(valDir, "labels");
        
        trainImagesDir.mkdirs();
        trainLabelsDir.mkdirs();
        valImagesDir.mkdirs();
        valLabelsDir.mkdirs();
          // Split images (80% train, 20% val) with random shuffle
        List<YoloImage> images = currentProject.getImages();
        java.util.Collections.shuffle(images, new java.util.Random(42)); // Fixed seed for reproducibility
        int trainCount = (int) (images.size() * 0.8);
          for (int i = 0; i < images.size(); i++) {
            YoloImage image = images.get(i);
            boolean isTrain = i < trainCount;
            
            File targetImagesDir = isTrain ? trainImagesDir : valImagesDir;
            File targetLabelsDir = isTrain ? trainLabelsDir : valLabelsDir;
            
            // Generate UUID-based filename to avoid special characters and spaces
            String uuid = java.util.UUID.randomUUID().toString();
            String originalFilename = image.getFilename();
            String fileExtension = getFileExtension(originalFilename);
            String safeFilename = uuid + "." + fileExtension;
            String safeLabelFilename = uuid + ".txt";
            
            // Copy image with safe name
            File sourceImage = new File(image.getPath());
            File targetImage = new File(targetImagesDir, safeFilename);
            Files.copy(sourceImage.toPath(), targetImage.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // Create label file with safe name
            File labelFile = new File(targetLabelsDir, safeLabelFilename);
            try (PrintWriter writer = new PrintWriter(new FileWriter(labelFile))) {
                for (var annotation : image.getAnnotations()) {
                    writer.println(annotation.toYoloFormat());
                }
            }
            
            System.out.println("Exported: " + originalFilename + " -> " + safeFilename);
        }
        
        // Create data.yaml file
        File dataYaml = new File(exportDir, "data.yaml");
        try (PrintWriter writer = new PrintWriter(new FileWriter(dataYaml))) {
            writer.println("path: " + exportDir.getAbsolutePath());
            writer.println("train: train/images");
            writer.println("val: val/images");
            writer.println("nc: " + currentProject.getClasses().size());
            writer.print("names: [");
            for (int i = 0; i < currentProject.getClasses().size(); i++) {
                if (i > 0) writer.print(", ");
                writer.print("'" + currentProject.getClasses().get(i) + "'");
            }
            writer.println("]");
        }
    }
    
    public void renameProject(String oldProjectPath, String newProjectName) throws IOException {
        // Validate new name
        if (newProjectName == null || newProjectName.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty");
        }
        
        // Check if project exists
        File oldProjectDir = new File(oldProjectPath);
        if (!oldProjectDir.exists()) {
            throw new FileNotFoundException("Project directory not found: " + oldProjectPath);
        }
        
        // Get workspace path
        String workspacePath = WorkspaceManager.getInstance().getWorkspacePath();
        if (workspacePath == null) {
            throw new IllegalStateException("No workspace configured");
        }
        
        // Create new project path
        String newProjectPath = workspacePath + File.separator + newProjectName.trim();
        File newProjectDir = new File(newProjectPath);
        
        // Check if target directory already exists
        if (newProjectDir.exists()) {
            throw new IOException("A project with name '" + newProjectName + "' already exists");
        }
        
        // Load current project data
        YoloProject project = null;
        File projectFile = new File(oldProjectDir, "project.json");
        if (projectFile.exists()) {
            project = objectMapper.readValue(projectFile, YoloProject.class);
        }
        
        // Rename/move directory
        boolean success = oldProjectDir.renameTo(newProjectDir);
        if (!success) {
            throw new IOException("Failed to rename project directory");
        }
        
        // Update project data if it exists
        if (project != null) {
            project.setName(newProjectName.trim());
            project.setProjectPath(newProjectPath);
            
            // Save updated project file
            File newProjectFile = new File(newProjectDir, "project.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(newProjectFile, project);
            
            // If this is the current project, update it
            if (currentProject != null && currentProject.getProjectPath().equals(oldProjectPath)) {
                currentProject = project;
                notifyProjectChanged();
            }
        }
        
        // Update recent projects
        WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
        workspaceManager.removeRecentProject(oldProjectPath);
        workspaceManager.addRecentProject(newProjectName.trim(), newProjectPath);
    }
    
    public void deleteProject(String projectPath) throws IOException {
        File projectDir = new File(projectPath);
        if (projectDir.exists()) {
            deleteDirectory(projectDir);
        }
        
        // If this is the current project, clear it
        if (currentProject != null && currentProject.getProjectPath().equals(projectPath)) {
            currentProject = null;
            notifyProjectChanged();
        }
    }
    
    private void deleteDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        throw new IOException("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete directory: " + directory.getAbsolutePath());
        }
    }
      public List<String> getRecentProjects() {
        return WorkspaceManager.getInstance().getRecentProjects().stream()
                .map(rp -> rp.getProjectPath())
                .toList();
    }
    
    // Listener pattern for UI updates
    public interface ProjectListener {
        void onProjectChanged(YoloProject project);
    }
    
    public void addProjectListener(ProjectListener listener) {
        listeners.add(listener);
    }
    
    public void removeProjectListener(ProjectListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyProjectChanged() {
        for (ProjectListener listener : listeners) {
            listener.onProjectChanged(currentProject);
        }
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "jpg"; // default extension
    }
}
