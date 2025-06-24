package raven.yolo.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for YOLO inference response
 */
public class YoloInferenceResponse {
    @JsonProperty("detections")
    private List<YoloDetection> detections;
    
    @JsonProperty("image_shape")
    private List<Integer> imageShape;
    
    @JsonProperty("error")
    private String error;
    
    // Constructors
    public YoloInferenceResponse() {}
    
    // Getters and setters
    public List<YoloDetection> getDetections() { return detections; }
    public void setDetections(List<YoloDetection> detections) { this.detections = detections; }
    
    public List<Integer> getImageShape() { return imageShape; }
    public void setImageShape(List<Integer> imageShape) { this.imageShape = imageShape; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
