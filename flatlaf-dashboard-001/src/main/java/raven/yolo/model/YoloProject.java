package raven.yolo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YoloProject {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("created_date")
    private LocalDateTime createdDate;
    
    @JsonProperty("project_path")
    private String projectPath;
    
    @JsonProperty("classes")
    private List<String> classes;
    
    @JsonProperty("images")
    private List<YoloImage> images;
      public YoloProject() {
        this.classes = new ArrayList<>();
        this.images = new ArrayList<>();
        this.createdDate = LocalDateTime.now();
        this.id = generateProjectId();
    }
    
    public YoloProject(String name, String description, String projectPath) {
        this();
        this.name = name;
        this.description = description;
        this.projectPath = projectPath;
    }
    
    private String generateProjectId() {
        return "project_" + System.currentTimeMillis();
    }
      // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
    
    public String getProjectPath() {
        return projectPath;
    }
    
    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }
    
    public List<String> getClasses() {
        return classes;
    }
    
    public void setClasses(List<String> classes) {
        this.classes = classes;
    }
    
    public List<YoloImage> getImages() {
        return images;
    }
    
    public void setImages(List<YoloImage> images) {
        this.images = images;
    }
    
    // Utility methods
    public void addClass(String className) {
        if (!classes.contains(className)) {
            classes.add(className);
        }
    }
    
    public void removeClass(String className) {
        classes.remove(className);
    }
    
    public void addImage(YoloImage image) {
        images.add(image);
    }
    
    public void removeImage(YoloImage image) {
        images.remove(image);
    }
    
    public File getImagesDir() {
        return new File(projectPath, "images");
    }
    
    public File getLabelsDir() {
        return new File(projectPath, "labels");
    }
    
    public File getProjectFile() {
        return new File(projectPath, "project.json");
    }
}
