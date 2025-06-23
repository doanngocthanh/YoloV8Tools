package raven.yolo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YoloAnnotation {
    
    @JsonProperty("class_id")
    private int classId;
    
    @JsonProperty("class_name")
    private String className;
    
    @JsonProperty("x_center")
    private double xCenter;
    
    @JsonProperty("y_center")
    private double yCenter;
    
    @JsonProperty("width")
    private double width;
    
    @JsonProperty("height")
    private double height;
    
    public YoloAnnotation() {
    }
    
    public YoloAnnotation(int classId, String className, double xCenter, double yCenter, double width, double height) {
        this.classId = classId;
        this.className = className;
        this.xCenter = xCenter;
        this.yCenter = yCenter;
        this.width = width;
        this.height = height;
    }
    
    // Getters and Setters
    public int getClassId() {
        return classId;
    }
    
    public void setClassId(int classId) {
        this.classId = classId;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public double getXCenter() {
        return xCenter;
    }
    
    public void setXCenter(double xCenter) {
        this.xCenter = xCenter;
    }
    
    public double getYCenter() {
        return yCenter;
    }
    
    public void setYCenter(double yCenter) {
        this.yCenter = yCenter;
    }
    
    public double getWidth() {
        return width;
    }
    
    public void setWidth(double width) {
        this.width = width;
    }
    
    public double getHeight() {
        return height;
    }
    
    public void setHeight(double height) {
        this.height = height;
    }
    
    // Utility methods for converting between normalized and pixel coordinates
    public int getPixelX(int imageWidth) {
        return (int) (xCenter * imageWidth);
    }
    
    public int getPixelY(int imageHeight) {
        return (int) (yCenter * imageHeight);
    }
    
    public int getPixelWidth(int imageWidth) {
        return (int) (width * imageWidth);
    }
    
    public int getPixelHeight(int imageHeight) {
        return (int) (height * imageHeight);
    }
    
    public int getPixelX1(int imageWidth) {
        return getPixelX(imageWidth) - getPixelWidth(imageWidth) / 2;
    }
    
    public int getPixelY1(int imageHeight) {
        return getPixelY(imageHeight) - getPixelHeight(imageHeight) / 2;
    }
    
    public int getPixelX2(int imageWidth) {
        return getPixelX(imageWidth) + getPixelWidth(imageWidth) / 2;
    }
    
    public int getPixelY2(int imageHeight) {
        return getPixelY(imageHeight) + getPixelHeight(imageHeight) / 2;
    }
    
    // Convert from pixel coordinates to normalized coordinates
    public static YoloAnnotation fromPixelCoordinates(int classId, String className, 
                                                     int x1, int y1, int x2, int y2, 
                                                     int imageWidth, int imageHeight) {
        double centerX = (x1 + x2) / 2.0 / imageWidth;
        double centerY = (y1 + y2) / 2.0 / imageHeight;
        double width = (double) Math.abs(x2 - x1) / imageWidth;
        double height = (double) Math.abs(y2 - y1) / imageHeight;
        
        return new YoloAnnotation(classId, className, centerX, centerY, width, height);
    }
    
    // Convert to YOLO format string
    public String toYoloFormat() {
        return String.format("%d %.6f %.6f %.6f %.6f", classId, xCenter, yCenter, width, height);
    }
    
    @Override
    public String toString() {
        return String.format("YoloAnnotation{classId=%d, className='%s', xCenter=%.3f, yCenter=%.3f, width=%.3f, height=%.3f}", 
                           classId, className, xCenter, yCenter, width, height);
    }
}
