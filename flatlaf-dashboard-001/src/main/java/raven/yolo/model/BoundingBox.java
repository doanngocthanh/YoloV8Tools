package raven.yolo.model;

import java.awt.*;

/**
 * Represents a YOLO bounding box annotation
 */
public class BoundingBox {
    private int classId;
    private String className;
    private double centerX; // normalized 0-1
    private double centerY; // normalized 0-1
    private double width;   // normalized 0-1
    private double height;  // normalized 0-1
    private Color color;
    private boolean selected;
    
    public BoundingBox(int classId, String className, double centerX, double centerY, double width, double height) {
        this.classId = classId;
        this.className = className;
        this.centerX = centerX;
        this.centerY = centerY;
        this.width = width;
        this.height = height;
        this.color = generateColorForClass(classId);
        this.selected = false;
    }
    
    private Color generateColorForClass(int classId) {
        // Generate distinct colors for different classes
        Color[] colors = {
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA,
            Color.CYAN, Color.ORANGE, Color.PINK, Color.LIGHT_GRAY, Color.DARK_GRAY
        };
        return colors[classId % colors.length];
    }
    
    // Convert normalized coordinates to pixel coordinates
    public Rectangle getPixelBounds(int imageWidth, int imageHeight) {
        int x = (int) ((centerX - width / 2) * imageWidth);
        int y = (int) ((centerY - height / 2) * imageHeight);
        int w = (int) (width * imageWidth);
        int h = (int) (height * imageHeight);
        return new Rectangle(x, y, w, h);
    }
    
    // Convert pixel coordinates to normalized coordinates
    public static BoundingBox fromPixelBounds(int classId, String className, Rectangle bounds, int imageWidth, int imageHeight) {
        double centerX = (bounds.x + bounds.width / 2.0) / imageWidth;
        double centerY = (bounds.y + bounds.height / 2.0) / imageHeight;
        double width = (double) bounds.width / imageWidth;
        double height = (double) bounds.height / imageHeight;
        return new BoundingBox(classId, className, centerX, centerY, width, height);
    }
    
    // Convert to YOLO format string
    public String toYOLOFormat() {
        return String.format("%d %.6f %.6f %.6f %.6f", classId, centerX, centerY, width, height);
    }
    
    // Getters and setters
    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public double getCenterX() { return centerX; }
    public void setCenterX(double centerX) { this.centerX = centerX; }
    
    public double getCenterY() { return centerY; }
    public void setCenterY(double centerY) { this.centerY = centerY; }
    
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }
    
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
