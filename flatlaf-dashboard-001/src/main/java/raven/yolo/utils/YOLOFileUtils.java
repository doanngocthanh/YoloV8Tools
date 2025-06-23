package raven.yolo.utils;

import raven.yolo.model.BoundingBox;
import raven.yolo.model.ClassManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for YOLO file operations
 */
public class YOLOFileUtils {
    
    /**
     * Save annotations to YOLO format (.txt file)
     */
    public static void saveAnnotations(List<BoundingBox> boundingBoxes, String imagePath) throws IOException {
        String txtPath = imagePath.replaceAll("\\.(jpg|jpeg|png|bmp)$", ".txt");
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(txtPath))) {
            for (BoundingBox box : boundingBoxes) {
                writer.println(box.toYOLOFormat());
            }
        }
    }
    
    /**
     * Load annotations from YOLO format (.txt file)
     */
    public static List<BoundingBox> loadAnnotations(String imagePath, ClassManager classManager) throws IOException {
        String txtPath = imagePath.replaceAll("\\.(jpg|jpeg|png|bmp)$", ".txt");
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        
        File txtFile = new File(txtPath);
        if (!txtFile.exists()) {
            return boundingBoxes; // Return empty list if no annotation file
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    BoundingBox box = parseYOLOLine(line, classManager);
                    if (box != null) {
                        boundingBoxes.add(box);
                    }
                }
            }
        }
        
        return boundingBoxes;
    }
    
    /**
     * Parse a single line from YOLO annotation file
     */
    private static BoundingBox parseYOLOLine(String line, ClassManager classManager) {
        try {
            String[] parts = line.split("\\s+");
            if (parts.length != 5) {
                return null;
            }
            
            int classId = Integer.parseInt(parts[0]);
            double centerX = Double.parseDouble(parts[1]);
            double centerY = Double.parseDouble(parts[2]);
            double width = Double.parseDouble(parts[3]);
            double height = Double.parseDouble(parts[4]);
            
            String className = classManager.getClassName(classId);
            return new BoundingBox(classId, className, centerX, centerY, width, height);
            
        } catch (NumberFormatException e) {
            System.err.println("Error parsing YOLO line: " + line);
            return null;
        }
    }
    
    /**
     * Save class names to classes.txt file
     */
    public static void saveClasses(ClassManager classManager, String directory) throws IOException {
        Path classesPath = Paths.get(directory, "classes.txt");
        String classesContent = classManager.exportClassesToString();
        Files.write(classesPath, classesContent.getBytes());
    }
    
    /**
     * Load class names from classes.txt file
     */
    public static void loadClasses(ClassManager classManager, String directory) throws IOException {
        Path classesPath = Paths.get(directory, "classes.txt");
        if (Files.exists(classesPath)) {
            String classesContent = new String(Files.readAllBytes(classesPath));
            classManager.importClassesFromString(classesContent);
        }
    }
    
    /**
     * Load image from file
     */
    public static BufferedImage loadImage(String imagePath) throws IOException {
        return ImageIO.read(new File(imagePath));
    }
    
    /**
     * Get all image files in a directory
     */
    public static List<String> getImageFiles(String directoryPath) {
        List<String> imageFiles = new ArrayList<>();
        File directory = new File(directoryPath);
        
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || 
                       lowerName.endsWith(".png") || lowerName.endsWith(".bmp");
            });
            
            if (files != null) {
                for (File file : files) {
                    imageFiles.add(file.getAbsolutePath());
                }
            }
        }
        
        return imageFiles;
    }
    
    /**
     * Create YOLO dataset structure
     * Creates train/val folders with images and labels subfolders
     */
    public static void createYOLODatasetStructure(String datasetPath) throws IOException {
        String[] folders = {
            "train/images",
            "train/labels", 
            "val/images",
            "val/labels"
        };
        
        for (String folder : folders) {
            Path folderPath = Paths.get(datasetPath, folder);
            Files.createDirectories(folderPath);
        }
    }
    
    /**
     * Generate data.yaml file for YOLO training
     */
    public static void generateDataYaml(String datasetPath, ClassManager classManager) throws IOException {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# YOLO Dataset Configuration\n");
        yaml.append("path: ").append(datasetPath).append("\n");
        yaml.append("train: train/images\n");
        yaml.append("val: val/images\n");
        yaml.append("\n");
        yaml.append("# Classes\n");
        yaml.append("nc: ").append(classManager.getClassCount()).append("  # number of classes\n");
        yaml.append("names: [");
        
        List<String> classes = classManager.getClasses();
        for (int i = 0; i < classes.size(); i++) {
            yaml.append("'").append(classes.get(i)).append("'");
            if (i < classes.size() - 1) {
                yaml.append(", ");
            }
        }
        yaml.append("]\n");
        
        Path yamlPath = Paths.get(datasetPath, "data.yaml");
        Files.write(yamlPath, yaml.toString().getBytes());
    }
}
