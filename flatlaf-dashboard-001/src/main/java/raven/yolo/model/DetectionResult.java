package raven.yolo.model;

import java.awt.*;

/**
 * Represents a single object detection result
 */
public class DetectionResult {
    private int classId;
    private String className;
    private double confidence;
    private BoundingBox boundingBox;
    
    public DetectionResult() {}
    
    public DetectionResult(int classId, String className, double confidence, BoundingBox boundingBox) {
        this.classId = classId;
        this.className = className;
        this.confidence = confidence;
        this.boundingBox = boundingBox;
    }
    
    // Getters and setters
    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public BoundingBox getBoundingBox() { return boundingBox; }
    public void setBoundingBox(BoundingBox boundingBox) { this.boundingBox = boundingBox; }
    
    /**
     * Get color based on confidence threshold
     */
    public Color getConfidenceColor() {
        if (confidence >= 0.8) {
            return new Color(0, 255, 0, 180); // Green for high confidence
        } else if (confidence >= 0.6) {
            return new Color(255, 255, 0, 180); // Yellow for medium confidence
        } else if (confidence >= 0.4) {
            return new Color(255, 165, 0, 180); // Orange for low-medium confidence
        } else {
            return new Color(255, 0, 0, 180); // Red for low confidence
        }
    }
    
    /**
     * Get border color (darker version of fill color)
     */
    public Color getBorderColor() {
        Color baseColor = getConfidenceColor();
        return new Color(
            Math.max(0, baseColor.getRed() - 50),
            Math.max(0, baseColor.getGreen() - 50),
            Math.max(0, baseColor.getBlue() - 50),
            255
        );
    }
    
    @Override
    public String toString() {
        return String.format("Detection: Class=%s (ID: %d), Confidence=%.2f%%, BBox=%s", 
            className != null ? className : "Unknown", classId, confidence * 100, boundingBox);
    }
    
    /**
     * Represents a bounding box with x, y, width, height
     */
    public static class BoundingBox {
        private double x, y, width, height;
        
        public BoundingBox() {}
        
        public BoundingBox(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        // Getters and setters
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
        
        public double getWidth() { return width; }
        public void setWidth(double width) { this.width = width; }
        
        public double getHeight() { return height; }
        public void setHeight(double height) { this.height = height; }
        
        /**
         * Get rectangle for drawing
         */
        public Rectangle getRectangle() {
            return new Rectangle((int)x, (int)y, (int)width, (int)height);
        }
        
        /**
         * Scale bounding box to fit image dimensions
         */
        public BoundingBox scale(double scaleX, double scaleY) {
            return new BoundingBox(
                x * scaleX,
                y * scaleY,
                width * scaleX,
                height * scaleY
            );
        }
        
        @Override
        public String toString() {
            return String.format("[%.2f, %.2f, %.2f, %.2f]", x, y, width, height);
        }
    }
}
