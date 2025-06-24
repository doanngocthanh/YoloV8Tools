package raven.yolo.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for individual YOLO detection
 */
public class YoloDetection {
    @JsonProperty("class_id")
    private int classId;
    
    @JsonProperty("class_name")
    private String className;
    
    @JsonProperty("confidence")
    private double confidence;
    
    @JsonProperty("bbox")
    private List<Double> bbox;
    
    // Constructors
    public YoloDetection() {}
    
    // Getters and setters
    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public List<Double> getBbox() { return bbox; }
    public void setBbox(List<Double> bbox) { this.bbox = bbox; }
}
