package raven.yolo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YoloImage {
    
    @JsonProperty("filename")
    private String filename;
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("width")
    private int width;
    
    @JsonProperty("height")
    private int height;
    
    @JsonProperty("annotations")
    private List<YoloAnnotation> annotations;
    
    @JsonProperty("labeled")
    private boolean labeled;
    
    public YoloImage() {
        this.annotations = new ArrayList<>();
        this.labeled = false;
    }
    
    public YoloImage(String filename, String path, int width, int height) {
        this();
        this.filename = filename;
        this.path = path;
        this.width = width;
        this.height = height;
    }
    
    // Getters and Setters
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public List<YoloAnnotation> getAnnotations() {
        return annotations;
    }
    
    public void setAnnotations(List<YoloAnnotation> annotations) {
        this.annotations = annotations;
    }
    
    public boolean isLabeled() {
        return labeled;
    }
    
    public void setLabeled(boolean labeled) {
        this.labeled = labeled;
    }
    
    // Utility methods
    public void addAnnotation(YoloAnnotation annotation) {
        annotations.add(annotation);
        labeled = true;
    }
    
    public void removeAnnotation(YoloAnnotation annotation) {
        annotations.remove(annotation);
        labeled = !annotations.isEmpty();
    }
    
    public void clearAnnotations() {
        annotations.clear();
        labeled = false;
    }
    
    public String getFilenameWithoutExtension() {
        if (filename == null) return null;
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }
}
